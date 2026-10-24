import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { LoaderCircle } from 'lucide-react';
import { cn } from '../../utils/cn';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type ButtonSize = 'md' | 'lg' | 'icon';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    size?: ButtonSize;
    loading?: boolean;
    leadingIcon?: ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
    primary: 'border-brand-700 bg-brand-700 text-white hover:bg-brand-800 active:bg-brand-900',
    secondary: 'border-line bg-white text-ink hover:bg-gray-50 active:bg-gray-100',
    ghost: 'border-transparent bg-transparent text-ink hover:bg-gray-100 active:bg-gray-200',
    danger: 'border-red-600 bg-red-600 text-white hover:bg-red-700 active:bg-red-800',
};

const sizeClasses: Record<ButtonSize, string> = {
    md: 'min-h-11 px-4 text-sm',
    lg: 'min-h-14 px-5 text-base',
    icon: 'h-11 w-11 p-0',
};

export function Button({
    children,
    className,
    variant = 'primary',
    size = 'md',
    loading = false,
    leadingIcon,
    disabled,
    type = 'button',
    ...props
}: ButtonProps) {
    return (
        <button
            type={type}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            className={cn(
                'inline-flex items-center justify-center gap-2 rounded-lg border font-semibold transition-colors',
                'disabled:cursor-not-allowed disabled:opacity-55',
                variantClasses[variant],
                sizeClasses[size],
                className,
            )}
            {...props}
        >
            {loading ? <LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" /> : leadingIcon}
            {children}
        </button>
    );
}
