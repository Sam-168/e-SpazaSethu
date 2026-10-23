import type { ReactNode } from 'react';
import { ArrowLeft } from 'lucide-react';

interface PageHeaderProps {
    title: string;
    description?: string;
    onBack?: () => void;
    action?: ReactNode;
}

export function PageHeader({ title, description, onBack, action }: PageHeaderProps) {
    return (
        <header className="mb-6 border-b border-line pb-4">
            <div className="flex min-h-11 items-center gap-3">
                {onBack && (
                    <button
                        type="button"
                        onClick={onBack}
                        aria-label="Go back"
                        className="-ml-2 flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-ink hover:bg-gray-100"
                    >
                        <ArrowLeft aria-hidden="true" className="h-5 w-5" />
                    </button>
                )}
                <div className="min-w-0 flex-1">
                    <h1 className="text-2xl font-semibold tracking-tight text-ink">{title}</h1>
                    {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
                </div>
                {action}
            </div>
        </header>
    );
}
