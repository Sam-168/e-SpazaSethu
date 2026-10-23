import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';
import { cn } from '../../utils/cn';

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
    label: string;
    error?: string;
    hint?: string;
    leadingIcon?: ReactNode;
    trailingAction?: ReactNode;
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
    { id, label, error, hint, leadingIcon, trailingAction, className, ...props },
    ref,
) {
    const inputId = id ?? props.name;
    const messageId = inputId ? `${inputId}-message` : undefined;

    return (
        <div className="w-full">
            <label htmlFor={inputId} className="mb-1.5 block text-sm font-medium text-ink">{label}</label>
            <div className="relative">
                {leadingIcon && (
                    <span className="pointer-events-none absolute inset-y-0 left-0 flex w-11 items-center justify-center text-muted">
                        {leadingIcon}
                    </span>
                )}
                <input
                    ref={ref}
                    id={inputId}
                    aria-invalid={Boolean(error)}
                    aria-describedby={error || hint ? messageId : undefined}
                    className={cn(
                        'min-h-12 w-full rounded-lg border bg-white px-3.5 text-base text-ink outline-none transition',
                        'placeholder:text-gray-400 focus:border-brand-600 focus:ring-3 focus:ring-brand-100',
                        'disabled:bg-gray-100 disabled:text-gray-500',
                        Boolean(leadingIcon) && 'pl-11',
                        Boolean(trailingAction) && 'pr-12',
                        error ? 'border-red-500 focus:border-red-600 focus:ring-red-100' : 'border-line',
                        className,
                    )}
                    {...props}
                />
                {trailingAction && <span className="absolute inset-y-0 right-0 flex w-12 items-center justify-center">{trailingAction}</span>}
            </div>
            {(error || hint) && (
                <p id={messageId} className={cn('mt-1.5 text-sm', error ? 'text-red-700' : 'text-muted')}>
                    {error ?? hint}
                </p>
            )}
        </div>
    );
});
