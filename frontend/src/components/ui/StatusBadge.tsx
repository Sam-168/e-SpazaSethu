import type { ReactNode } from 'react';
import { cn } from '../../utils/cn';

type StatusTone = 'neutral' | 'success' | 'warning' | 'danger';

interface StatusBadgeProps {
    children: ReactNode;
    tone?: StatusTone;
}

const toneClasses: Record<StatusTone, string> = {
    neutral: 'bg-gray-100 text-gray-700',
    success: 'bg-brand-50 text-brand-800',
    warning: 'bg-warning-50 text-warning-700',
    danger: 'bg-red-50 text-red-700',
};

export function StatusBadge({ children, tone = 'neutral' }: StatusBadgeProps) {
    return <span className={cn('inline-flex min-h-6 items-center rounded-full px-2.5 text-xs font-semibold', toneClasses[tone])}>{children}</span>;
}
