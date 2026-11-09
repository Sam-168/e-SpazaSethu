import api from './api';

export interface BarcodeSuggestion {
    status: 'FOUND' | 'NOT_FOUND' | 'UNAVAILABLE';
    name: string | null;
    quantity: string | null;
    source: string;
}

export async function lookupBarcode(barcode: string): Promise<BarcodeSuggestion> {
    const { data } = await api.get<BarcodeSuggestion>(`/products/barcode-lookup/${encodeURIComponent(barcode)}`);
    return data;
}

export function suggestedName(suggestion: BarcodeSuggestion): string {
    const name = suggestion.name?.trim() || '';
    const quantity = suggestion.quantity?.trim() || '';
    return quantity && !name.toLowerCase().includes(quantity.toLowerCase()) ? `${name} ${quantity}` : name;
}
