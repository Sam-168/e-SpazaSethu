import { useEffect, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { Button } from './Button';

interface DialogProps {
    open: boolean;
    title: string;
    description?: string;
    children?: ReactNode;
    confirmLabel?: string;
    cancelLabel?: string;
    confirmVariant?: 'primary' | 'danger';
    busy?: boolean;
    onConfirm?: () => void;
    onClose: () => void;
}

export function Dialog({ open, title, description, children, confirmLabel, cancelLabel = 'Cancel', confirmVariant = 'primary', busy = false, onConfirm, onClose }: DialogProps) {
    useEffect(() => {
        if (!open) return;
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !busy) onClose();
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [busy, onClose, open]);

    if (!open) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-6">
            <button type="button" aria-label="Close dialog" className="absolute inset-0 cursor-default bg-black/45" onClick={() => !busy && onClose()} />
            <section role="dialog" aria-modal="true" aria-labelledby="dialog-title" className="relative w-full max-w-md rounded-t-2xl bg-white p-5 pb-[calc(env(safe-area-inset-bottom)+1.25rem)] sm:rounded-2xl">
                <div className="flex items-start gap-3">
                    <div className="min-w-0 flex-1">
                        <h2 id="dialog-title" className="text-lg font-semibold text-ink">{title}</h2>
                        {description && <p className="mt-1 text-sm leading-6 text-muted">{description}</p>}
                    </div>
                    <button type="button" aria-label="Close" disabled={busy} onClick={onClose} className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-muted hover:bg-gray-100">
                        <X aria-hidden="true" className="h-5 w-5" />
                    </button>
                </div>
                {children && <div className="mt-4">{children}</div>}
                {(confirmLabel || cancelLabel) && (
                    <div className="mt-6 grid grid-cols-2 gap-3">
                        <Button variant="secondary" size="lg" disabled={busy} onClick={onClose}>{cancelLabel}</Button>
                        {confirmLabel && onConfirm && <Button variant={confirmVariant} size="lg" loading={busy} onClick={onConfirm}>{confirmLabel}</Button>}
                    </div>
                )}
            </section>
        </div>
    );
}
