import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Barcode, Camera, Flashlight, Keyboard, X } from 'lucide-react';
import type { IScannerControls } from '@zxing/browser';
import { Button } from './Button';
import { formatCurrency } from '../../utils/format';

export interface ScanFeedback { kind: 'added' | 'missing' | 'soldOut' | 'limit' | 'error'; message: string; barcode?: string }
interface BarcodeScannerDialogProps { open: boolean; onScan: (barcode: string) => void | ScanFeedback | Promise<void | ScanFeedback>; onClose: () => void; title?: string; continuous?: boolean; summary?: { itemCount: number; total: number }; onReview?: () => void; onAddUnknown?: (barcode: string) => void }

type NativeBarcode = { rawValue: string };
type NativeDetector = { detect: (source: HTMLVideoElement) => Promise<NativeBarcode[]> };
type NativeDetectorConstructor = {
    new (options: { formats: string[] }): NativeDetector;
    getSupportedFormats: () => Promise<string[]>;
};
const retailFormats = ['ean_13', 'ean_8', 'upc_a', 'upc_e'];
const cameraConstraints: MediaStreamConstraints = {
    audio: false,
    video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
};

export function BarcodeScannerDialog({ open, onScan, onClose, title = 'Scan barcode', continuous = false, summary, onReview, onAddUnknown }: BarcodeScannerDialogProps) {
    const videoRef = useRef<HTMLVideoElement>(null);
    const controlsRef = useRef<IScannerControls | null>(null);
    const streamRef = useRef<MediaStream | null>(null);
    const onScanRef = useRef(onScan);
    const switchScannerRef = useRef<(() => void) | null>(null);
    const busyRef = useRef(false);
    const lastReadRef = useRef({ barcode: '', at: 0 });
    const audioRef = useRef<AudioContext | null>(null);
    const [status, setStatus] = useState<'starting' | 'scanning' | 'error'>('starting');
    const [message, setMessage] = useState('Starting camera…');
    const [manual, setManual] = useState('');
    const [torch, setTorch] = useState(false);
    const [torchAvailable, setTorchAvailable] = useState(false);
    const [engine, setEngine] = useState<'native' | 'zxing' | null>(null);
    const [feedback, setFeedback] = useState<ScanFeedback | null>(null);
    const [timing, setTiming] = useState<{ firstReadMs: number | null; lastIntervalMs: number | null; reads: number }>({ firstReadMs: null, lastIntervalMs: null, reads: 0 });
    const [showTiming, setShowTiming] = useState(false);

    useEffect(() => { onScanRef.current = onScan; }, [onScan]);

    useEffect(() => {
        if (!open) return;
        busyRef.current = false;
        lastReadRef.current = { barcode: '', at: 0 };
        // eslint-disable-next-line react-hooks/set-state-in-effect -- a new scanner session resets its visible state
        setStatus('starting');
        setMessage('Starting camera…');
        setManual('');
        setTorch(false);
        setTorchAvailable(false);
        setEngine(null);
        setFeedback(null);
        setTiming({ firstReadMs: null, lastIntervalMs: null, reads: 0 });
        setShowTiming(false);

        let active = true;
        let mode: 'native' | 'zxing' | null = null;
        let scanTimer: number | undefined;
        let readyAt = 0;
        let previousReadAt = 0;
        const stopCamera = () => {
            mode = null;
            window.clearTimeout(scanTimer);
            controlsRef.current?.stop();
            controlsRef.current = null;
            streamRef.current?.getTracks().forEach(track => track.stop());
            streamRef.current = null;
            const video = videoRef.current;
            if (video) video.srcObject = null;
            setTorch(false);
            setTorchAvailable(false);
        };
        const notify = (result: ScanFeedback) => {
            if (!active) return;
            setFeedback(result);
            const success = result.kind === 'added';
            navigator.vibrate?.(success ? 45 : [70, 50, 70]);
            try {
                const audio = audioRef.current ?? new AudioContext();
                audioRef.current = audio;
                void audio.resume().then(() => {
                    if (!active) return;
                    const tone = audio.createOscillator();
                    const gain = audio.createGain();
                    tone.frequency.value = success ? 880 : 330;
                    gain.gain.value = 0.035;
                    tone.connect(gain);
                    gain.connect(audio.destination);
                    tone.start();
                    tone.stop(audio.currentTime + (success ? 0.07 : 0.13));
                }).catch(() => {});
            } catch { /* Sound is optional when the browser blocks it. */ }
        };
        const accept = async (barcode: string) => {
            if (!barcode || busyRef.current) return;
            busyRef.current = true;
            try {
                const result = await onScanRef.current(barcode);
                if (result) notify(result);
            } catch {
                notify({ kind: 'error', message: 'The barcode could not be checked. Try again.' });
            } finally {
                busyRef.current = false;
            }
        };
        const read = (barcode: string) => {
            if (!active || !barcode || busyRef.current) return;
            const now = performance.now();
            const last = lastReadRef.current;
            // A product held in frame should count once; remove it briefly before rescanning.
            if (barcode === last.barcode && now - last.at < 1200) {
                lastReadRef.current = { barcode, at: now };
                return;
            }
            lastReadRef.current = { barcode, at: now };
            setTiming(current => ({
                firstReadMs: current.firstReadMs ?? Math.round(now - readyAt),
                lastIntervalMs: previousReadAt ? Math.round(now - previousReadAt) : null,
                reads: current.reads + 1,
            }));
            previousReadAt = now;
            if (!continuous) stopCamera();
            void accept(barcode);
        };
        const cameraError = (error: unknown) => {
            if (!active) return;
            setStatus('error');
            const name = error instanceof DOMException ? error.name : '';
            setMessage(name === 'NotAllowedError'
                ? 'Camera permission was denied. Allow it in browser settings or enter the code manually.'
                : name === 'NotFoundError'
                    ? 'No camera was found on this device. Enter the barcode manually.'
                    : 'The camera could not be started. Close other camera apps or enter the code manually.');
        };
        const startZxing = async (extended = false) => {
            if (!active) return;
            stopCamera();
            setStatus('starting');
            setMessage('Starting alternate scanner…');
            try {
                const [{ BrowserMultiFormatReader }, { BarcodeFormat, DecodeHintType }] = await Promise.all([import('@zxing/browser'), import('@zxing/library')]);
                if (!active || !videoRef.current) return;
                const formats = [BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E];
                if (extended) formats.push(BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF);
                const hints = new Map();
                hints.set(DecodeHintType.POSSIBLE_FORMATS, formats);
                if (extended) hints.set(DecodeHintType.TRY_HARDER, true);
                const reader = new BrowserMultiFormatReader(hints, { delayBetweenScanAttempts: 100, delayBetweenScanSuccess: 200 });
                mode = 'zxing';
                readyAt = performance.now();
                const controls = await reader.decodeFromConstraints(cameraConstraints, videoRef.current, result => {
                    if (active && mode === 'zxing' && result) read(result.getText().trim());
                });
                if (!active || mode !== 'zxing') { controls.stop(); return; }
                controlsRef.current = controls;
                const track = (videoRef.current.srcObject as MediaStream | null)?.getVideoTracks()[0];
                setTorchAvailable(Boolean(controls.switchTorch && (track?.getCapabilities?.() as (MediaTrackCapabilities & { torch?: boolean }) | undefined)?.torch));
                setEngine('zxing');
                setStatus('scanning');
                setMessage('Align the barcode in the frame. Hold steady; turn on the light if needed.');
            } catch (error) {
                stopCamera();
                cameraError(error);
            }
        };
        switchScannerRef.current = () => { void startZxing(true); };
        const startNative = async (Detector: NativeDetectorConstructor, formats: string[]) => {
            try {
                const detector = new Detector({ formats });
                const stream = await navigator.mediaDevices.getUserMedia(cameraConstraints);
                if (!active || !videoRef.current) { stream.getTracks().forEach(track => track.stop()); return; }
                streamRef.current = stream;
                videoRef.current.srcObject = stream;
                await videoRef.current.play();
                if (!active) return;
                mode = 'native';
                const track = stream.getVideoTracks()[0];
                setTorchAvailable(Boolean((track?.getCapabilities?.() as (MediaTrackCapabilities & { torch?: boolean }) | undefined)?.torch));
                setEngine('native');
                setStatus('scanning');
                setMessage('Align the barcode in the frame. Hold steady; turn on the light if needed.');
                readyAt = performance.now();
                const detect = async () => {
                    if (!active || mode !== 'native' || !videoRef.current) return;
                    try {
                        const results = await detector.detect(videoRef.current);
                        if (active && mode === 'native' && results[0]) read(results[0].rawValue.trim());
                    } catch {
                        if (active && mode === 'native') void startZxing();
                        return;
                    }
                    if (active && mode === 'native') scanTimer = window.setTimeout(() => { void detect(); }, 100);
                };
                void detect();
            } catch (error) {
                stopCamera();
                // Camera permission/device errors affect both engines; a detector failure does not.
                if (error instanceof DOMException && ['NotAllowedError', 'NotFoundError'].includes(error.name)) cameraError(error);
                else void startZxing();
            }
        };
        const start = async () => {
            if (!window.isSecureContext && !['localhost', '127.0.0.1'].includes(window.location.hostname)) {
                setStatus('error');
                setMessage('Camera scanning requires HTTPS. You can still enter the barcode manually.');
                return;
            }
            if (!navigator.mediaDevices?.getUserMedia) {
                setStatus('error');
                setMessage('This browser does not provide camera access. Enter the barcode manually.');
                return;
            }
            const Detector = (window as Window & { BarcodeDetector?: NativeDetectorConstructor }).BarcodeDetector;
            if (!Detector?.getSupportedFormats) { void startZxing(); return; }
            try {
                const supported = await Detector.getSupportedFormats();
                if (!active) return;
                const formats = retailFormats.filter(format => supported.includes(format));
                if (formats.length) void startNative(Detector, formats);
                else void startZxing();
            } catch { void startZxing(); }
        };
        void start();
        return () => {
            active = false;
            switchScannerRef.current = null;
            stopCamera();
            void audioRef.current?.close();
            audioRef.current = null;
        };
    }, [open, continuous]);

    const submit = (event: FormEvent) => {
        event.preventDefault();
        const value = manual.trim();
        if (!value) return;
        setManual('');
        void Promise.resolve(onScanRef.current(value)).then(result => {
            if (result) { setFeedback(result); navigator.vibrate?.(result.kind === 'added' ? 45 : [70, 50, 70]); }
        }).catch(() => setFeedback({ kind: 'error', message: 'The barcode could not be checked. Try again.' }));
    };
    const toggleTorch = async () => {
        try {
            if (engine === 'zxing' && controlsRef.current?.switchTorch) await controlsRef.current.switchTorch(!torch);
            else if (engine === 'native') {
                const track = streamRef.current?.getVideoTracks()[0];
                if (!track) return;
                await track.applyConstraints({ advanced: [{ torch: !torch }] } as unknown as MediaTrackConstraints);
            } else return;
            setTorch(!torch);
        } catch { setTorchAvailable(false); }
    };
    if (!open) return null;
    return <div className="fixed inset-0 z-[80] flex flex-col bg-black text-white" role="dialog" aria-modal="true" aria-labelledby="scanner-title">
        <header className="flex min-h-16 items-center gap-3 px-4"><div className="min-w-0 flex-1"><h2 id="scanner-title" className="font-semibold">{title}</h2><p className="text-xs text-white/70">Retail barcodes · EAN and UPC</p></div>{torchAvailable && <button type="button" aria-label={torch ? 'Turn light off' : 'Turn light on'} onClick={() => void toggleTorch()} className={`grid h-11 w-11 place-items-center rounded-full ${torch ? 'bg-white text-black' : 'bg-white/15'}`}><Flashlight className="h-5 w-5" /></button>}<button type="button" aria-label="Close scanner" onClick={onClose} className="grid h-11 w-11 place-items-center rounded-full bg-white/15"><X className="h-5 w-5" /></button></header>
        <div className="relative min-h-0 flex-1 overflow-hidden bg-gray-950"><video ref={videoRef} muted playsInline className="h-full w-full object-cover" /><div className="pointer-events-none absolute inset-0 grid place-items-center"><div className="relative h-32 w-[86%] max-w-md border-x-2 border-white/90 shadow-[0_0_0_9999px_rgba(0,0,0,0.45)] sm:h-40"><span className="absolute inset-x-0 top-0 border-t-2 border-white/90" /><span className="absolute inset-x-0 bottom-0 border-b-2 border-white/90" /></div><p className="absolute bottom-5 px-4 text-center text-sm font-medium text-white drop-shadow-md">Centre the whole barcode, hold steady, and avoid glare.</p></div>{status !== 'scanning' && <div className="absolute inset-0 grid place-items-center bg-black/70 px-6 text-center"><div>{status === 'starting' ? <Camera className="mx-auto h-10 w-10 animate-pulse" /> : <Barcode className="mx-auto h-10 w-10 text-amber-400" />}<p className="mt-3 max-w-sm text-sm leading-6">{message}</p></div></div>}</div>
        <footer className="bg-white px-4 pb-[calc(env(safe-area-inset-bottom)+1rem)] pt-4 text-ink">{summary && <div className="mx-auto mb-3 flex max-w-lg items-center justify-between border-b border-line pb-3"><span className="text-sm text-muted">{summary.itemCount} {summary.itemCount === 1 ? 'item' : 'items'} in basket</span><strong className="text-2xl text-ink">{formatCurrency(summary.total)}</strong></div>}<p aria-live="polite" className={`mx-auto mb-3 max-w-lg text-center text-sm font-medium ${feedback?.kind === 'added' ? 'text-brand-800' : feedback ? 'text-red-700' : 'text-muted'}`}>{feedback?.message ?? (status === 'scanning' ? message : 'Use manual entry if the camera is unavailable.')}</p>{feedback?.kind === 'missing' && feedback.barcode && onAddUnknown && <Button variant="secondary" className="mx-auto mb-3 block w-full max-w-lg" onClick={() => onAddUnknown(feedback.barcode!)}>Add this product</Button>}<form onSubmit={submit} className="mx-auto flex max-w-lg gap-2"><label className="relative min-w-0 flex-1"><span className="sr-only">Enter barcode manually</span><Keyboard className="absolute left-3 top-3.5 h-5 w-5 text-muted" /><input inputMode="numeric" autoComplete="off" value={manual} onChange={event => setManual(event.target.value)} placeholder="Enter barcode" className="min-h-12 w-full rounded-lg border border-line bg-white pl-10 pr-3 text-base outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label><Button type="submit" disabled={!manual.trim()}>Use code</Button></form>{onReview && summary && summary.itemCount > 0 && <Button className="mx-auto mt-3 block w-full max-w-lg" onClick={onReview}>Review sale · {formatCurrency(summary.total)}</Button>}{status === 'scanning' && <div className="mx-auto mt-3 flex max-w-lg items-center justify-between gap-3 text-xs text-muted"><button type="button" onClick={() => switchScannerRef.current?.()} className="min-h-10 text-brand-800 underline">{engine === 'native' ? 'Try alternate scanner' : 'Scan other code types'}</button><button type="button" onClick={() => setShowTiming(value => !value)} className="min-h-10 underline">{showTiming ? 'Hide' : 'Show'} scan timing</button></div>}{showTiming && status === 'scanning' && <p className="mx-auto max-w-lg text-xs text-muted" role="status">{engine === 'native' ? 'Native camera' : 'ZXing'} · First read: {timing.firstReadMs === null ? 'waiting' : `${timing.firstReadMs} ms`} · Last gap: {timing.lastIntervalMs === null ? '—' : `${timing.lastIntervalMs} ms`} · Reads: {timing.reads}. The first read includes time spent aiming; gaps include moving between products.</p>}</footer>
    </div>;
}
