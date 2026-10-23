interface LoadingStateProps {
    rows?: number;
    label?: string;
}

export function LoadingState({ rows = 4, label = 'Loading' }: LoadingStateProps) {
    return (
        <div role="status" aria-label={label} className="animate-pulse border-y border-line bg-white px-4">
            {Array.from({ length: rows }, (_, index) => (
                <div key={index} className="flex min-h-16 items-center gap-3 border-b border-line last:border-b-0">
                    <div className="h-9 w-9 rounded-md bg-gray-200" />
                    <div className="flex-1">
                        <div className="h-3.5 w-2/3 rounded bg-gray-200" />
                        <div className="mt-2 h-3 w-1/3 rounded bg-gray-100" />
                    </div>
                </div>
            ))}
            <span className="sr-only">{label}</span>
        </div>
    );
}
