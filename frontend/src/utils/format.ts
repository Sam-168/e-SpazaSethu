export function formatCurrency(value: number | null | undefined): string {
    const currency = localStorage.getItem('storeCurrency') || 'ZAR';
    return new Intl.NumberFormat('en-ZA', {
        style: 'currency', currency, minimumFractionDigits: 2,
    }).format(value ?? 0);
}

export function formatDate(value: string | Date, options?: Intl.DateTimeFormatOptions): string {
    const date = typeof value === 'string' ? new Date(value) : value;
    return date.toLocaleDateString('en-ZA', options ?? {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    });
}

export function formatDateTime(value: string | Date): string {
    const date = typeof value === 'string' ? new Date(value) : value;
    return date.toLocaleString('en-ZA', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    });
}

export function formatQuantity(value: number, unit = 'units'): string {
    return `${new Intl.NumberFormat('en-ZA').format(value)} ${unit}`;
}
