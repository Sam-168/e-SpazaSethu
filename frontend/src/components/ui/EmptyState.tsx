import type { ReactNode } from 'react';

interface EmptyStateProps {
    icon?: ReactNode;
    title: string;
    description: string;
    action?: ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
    return (
        <section className="border-y border-line bg-white px-5 py-10 text-center">
            {icon && <div className="mx-auto mb-4 flex h-11 w-11 items-center justify-center text-brand-700">{icon}</div>}
            <h2 className="text-base font-semibold text-ink">{title}</h2>
            <p className="mx-auto mt-1 max-w-sm text-sm leading-6 text-muted">{description}</p>
            {action && <div className="mt-5">{action}</div>}
        </section>
    );
}
