import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Barcode, Camera, Flashlight, Keyboard, X } from 'lucide-react';
import type { IScannerControls } from '@zxing/browser';
import { Button } from './Button';
import { formatCurrency } from '../../utils/format';

export interface ScanFeedback { kind:'added'|'missing'|'soldOut'|'limit'|'error'; message:string; barcode?:string }
interface BarcodeScannerDialogProps { open:boolean; onScan:(barcode:string)=>void|ScanFeedback|Promise<void|ScanFeedback>; onClose:()=>void; title?:string; continuous?:boolean; summary?:{itemCount:number;total:number}; onReview?:()=>void; onAddUnknown?: (barcode:string)=>void }

export function BarcodeScannerDialog({open,onScan,onClose,title='Scan barcode',continuous=false,summary,onReview,onAddUnknown}:BarcodeScannerDialogProps){
    const videoRef=useRef<HTMLVideoElement>(null); const controlsRef=useRef<IScannerControls|null>(null); const onScanRef=useRef(onScan);
    const busyRef=useRef(false); const lastReadRef=useRef({barcode:'',at:0}); const audioRef=useRef<AudioContext|null>(null);
    const [status,setStatus]=useState<'starting'|'scanning'|'error'>('starting'); const [message,setMessage]=useState('Starting camera…'); const [manual,setManual]=useState(''); const [torch,setTorch]=useState(false); const [torchAvailable,setTorchAvailable]=useState(false); const [feedback,setFeedback]=useState<ScanFeedback|null>(null);
    useEffect(()=>{onScanRef.current=onScan;},[onScan]);
    useEffect(()=>{if(!open)return;busyRef.current=false;lastReadRef.current={barcode:'',at:0};
        // eslint-disable-next-line react-hooks/set-state-in-effect -- opening the scanner starts a new camera session
        setStatus('starting');setMessage('Starting camera…');setManual('');setTorch(false);setTorchAvailable(false);setFeedback(null);
        let active=true;
        const stop=()=>{controlsRef.current?.stop();controlsRef.current=null;const stream=videoRef.current?.srcObject as MediaStream|null;stream?.getTracks().forEach(track=>track.stop());if(videoRef.current)videoRef.current.srcObject=null;};
        const notify=(result:ScanFeedback)=>{
            if(!active)return;
            setFeedback(result);
            const success=result.kind==='added';
            navigator.vibrate?.(success?45:[70,50,70]);
            try{
                const audio=audioRef.current??new AudioContext();audioRef.current=audio;
                void audio.resume().then(()=>{if(!active)return;const tone=audio.createOscillator();const gain=audio.createGain();tone.frequency.value=success?880:330;gain.gain.value=0.035;tone.connect(gain);gain.connect(audio.destination);tone.start();tone.stop(audio.currentTime+(success?0.07:0.13));}).catch(()=>{});
            }catch{/* Audio is optional when blocked by the browser. */}
        };
        const accept=async(barcode:string)=>{
            if(!barcode||busyRef.current)return;
            busyRef.current=true;
            try{const result=await onScanRef.current(barcode);if(result)notify(result);}
            catch{notify({kind:'error',message:'The barcode could not be checked. Try again.'});}
            finally{busyRef.current=false;}
        };
        const start=async()=>{
            if(!window.isSecureContext&&!['localhost','127.0.0.1'].includes(window.location.hostname)){setStatus('error');setMessage('Camera scanning requires HTTPS. You can still enter the barcode manually.');return;}
            if(!navigator.mediaDevices?.getUserMedia){setStatus('error');setMessage('This browser does not provide camera access. Enter the barcode manually.');return;}
            try{
                const [{BrowserMultiFormatReader},{BarcodeFormat,DecodeHintType}]=await Promise.all([import('@zxing/browser'),import('@zxing/library')]);
                if(!active)return;
                const formats=[BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.ITF];
                const hints=new Map();hints.set(DecodeHintType.POSSIBLE_FORMATS,formats);hints.set(DecodeHintType.TRY_HARDER,true);
                const reader=new BrowserMultiFormatReader(hints,{delayBetweenScanAttempts:120,delayBetweenScanSuccess:250});
                const controls=await reader.decodeFromConstraints({audio:false,video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720}}},videoRef.current!,result=>{
                    if(!active||!result)return;
                    const barcode=result.getText().trim();const now=Date.now();const last=lastReadRef.current;
                    // Keep one item held in the frame from being counted repeatedly.
                    if(barcode===last.barcode&&now-last.at<1200){lastReadRef.current={barcode,at:now};return;}
                    if(busyRef.current)return;
                    lastReadRef.current={barcode,at:now};
                    if(!continuous)stop();
                    void accept(barcode);
                });
                if(!active){controls.stop();return;}
                controlsRef.current=controls;setTorchAvailable(Boolean(controls.switchTorch));setStatus('scanning');
                setMessage(continuous?'Scan items one after another. Move an item away before scanning it again.':'Point the camera at the barcode.');
            }catch(error){if(!active)return;setStatus('error');const name=error instanceof DOMException?error.name:'';setMessage(name==='NotAllowedError'?'Camera permission was denied. Allow access in your browser settings or enter the code manually.':name==='NotFoundError'?'No camera was found on this device. Enter the barcode manually.':'The camera could not be started. Close other camera apps or enter the code manually.');}
        };
        void start();
        return()=>{active=false;stop();void audioRef.current?.close();audioRef.current=null;};
    },[open,continuous]);
    const submit=(event:FormEvent)=>{event.preventDefault();const value=manual.trim();if(!value)return;setManual('');void Promise.resolve(onScanRef.current(value)).then(result=>{if(result){setFeedback(result);navigator.vibrate?.(result.kind==='added'?45:[70,50,70]);}}).catch(()=>setFeedback({kind:'error',message:'The barcode could not be checked. Try again.'}));};
    const toggleTorch=async()=>{if(!controlsRef.current?.switchTorch)return;try{await controlsRef.current.switchTorch(!torch);setTorch(!torch);}catch{setTorchAvailable(false);}};
    if(!open)return null;
    return <div className="fixed inset-0 z-[80] flex flex-col bg-black text-white" role="dialog" aria-modal="true" aria-labelledby="scanner-title"><header className="flex min-h-16 items-center gap-3 px-4"><div className="min-w-0 flex-1"><h2 id="scanner-title" className="font-semibold">{title}</h2><p className="text-xs text-white/70">EAN, UPC, Code 128 and Code 39</p></div>{torchAvailable&&<button type="button" aria-label={torch?'Turn flash off':'Turn flash on'} onClick={()=>void toggleTorch()} className={`grid h-11 w-11 place-items-center rounded-full ${torch?'bg-white text-black':'bg-white/15'}`}><Flashlight className="h-5 w-5" /></button>}<button type="button" aria-label="Close scanner" onClick={onClose} className="grid h-11 w-11 place-items-center rounded-full bg-white/15"><X className="h-5 w-5" /></button></header>
        <div className="relative min-h-0 flex-1 overflow-hidden bg-gray-950"><video ref={videoRef} muted playsInline className="h-full w-full object-cover" /><div className="pointer-events-none absolute inset-0 grid place-items-center"><div className="relative h-36 w-[82%] max-w-md rounded-xl border-2 border-white/90 shadow-[0_0_0_9999px_rgba(0,0,0,0.45)]"><span className="absolute inset-x-5 top-1/2 h-0.5 bg-brand-300 shadow-[0_0_10px_rgba(114,197,217,0.9)]" /></div></div>{status!=='scanning'&&<div className="absolute inset-0 grid place-items-center bg-black/70 px-6 text-center"><div>{status==='starting'?<Camera className="mx-auto h-10 w-10 animate-pulse" />:<Barcode className="mx-auto h-10 w-10 text-amber-400" />}<p className="mt-3 max-w-sm text-sm leading-6">{message}</p></div></div>}</div>
        <footer className="bg-white px-4 pb-[calc(env(safe-area-inset-bottom)+1rem)] pt-4 text-ink">{summary&&<div className="mx-auto mb-3 flex max-w-lg items-center justify-between border-b border-line pb-3"><span className="text-sm text-muted">{summary.itemCount} {summary.itemCount===1?'item':'items'} in basket</span><strong className="text-2xl text-ink">{formatCurrency(summary.total)}</strong></div>}<p aria-live="polite" className={`mx-auto mb-3 max-w-lg text-center text-sm font-medium ${feedback?.kind==='added'?'text-brand-800':feedback?'text-red-700':'text-muted'}`}>{feedback?.message??(status==='scanning'?message:'Use manual entry if the camera is unavailable.')}</p>{feedback?.kind==='missing'&&feedback.barcode&&onAddUnknown&&<Button variant="secondary" className="mx-auto mb-3 block w-full max-w-lg" onClick={()=>onAddUnknown(feedback.barcode!)}>Add this product</Button>}<form onSubmit={submit} className="mx-auto flex max-w-lg gap-2"><label className="relative min-w-0 flex-1"><span className="sr-only">Enter barcode manually</span><Keyboard className="absolute left-3 top-3.5 h-5 w-5 text-muted" /><input inputMode="numeric" autoComplete="off" value={manual} onChange={event=>setManual(event.target.value)} placeholder="Enter barcode" className="min-h-12 w-full rounded-lg border border-line bg-white pl-10 pr-3 text-base outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label><Button type="submit" disabled={!manual.trim()}>Use code</Button></form>{onReview&&summary&&summary.itemCount>0&&<Button className="mx-auto mt-3 block w-full max-w-lg" onClick={onReview}>Review sale · {formatCurrency(summary.total)}</Button>}</footer>
    </div>;
}
