import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

type LoadState = 'loading' | 'success' | 'error';

// Defined locally rather than imported from '../types': the shared types
// file already has a differently-shaped SaleItem (Amanda's side), and
// GET /sales/:id's exact response shape isn't confirmed yet. Once the
// backend contract is settled, swap this for the real shared type.
interface SaleLineItem {
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
}

interface SaleDetail {
    saleId: string;
    saleDateTime: string;
    cashierName: string;
    paymentMethod: string;
    status: string;
    totalAmount: number;
    items: SaleLineItem[];
}

const currency = new Intl.NumberFormat('en-ZA', {
    style: 'currency',
    currency: 'ZAR',
    minimumFractionDigits: 2,
});

const PAYMENT_LABELS: Record<string, string> = {
    CASH: 'Cash',
    CARD: 'Card',
    MOBILE: 'Mobile',
    MOBILE_PAYMENT: 'Mobile Payment',
};

function formatSaleDateTime(iso: string): string {
    const date = new Date(iso);
    const datePart = date.toLocaleDateString('en-ZA', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    });
    const timePart = date.toLocaleTimeString('en-ZA', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    });
    return `${datePart}, ${timePart}`;
}

export default function SaleDetailPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { currentUser } = useAuth();
    const isAdmin = currentUser?.role === 'ADMIN';

    const [state, setState] = useState<LoadState>('loading');
    const [sale, setSale] = useState<SaleDetail | null>(null);

    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const [isCancelling, setIsCancelling] = useState(false);
    const [cancelError, setCancelError] = useState<string | null>(null);

    const loadSale = async () => {
        if (!id) return;
        setState('loading');
        try {
            const { data } = await api.get<SaleDetail>(`/sales/${id}`);
            setSale(data);
            setState('success');
        } catch {
            setState('error');
        }
    };

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- intentional: loading state resets on every id change for the standard fetch-on-mount/param-change pattern
        void loadSale();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const handleCancelSale = async () => {
        if (!id) return;
        setIsCancelling(true);
        setCancelError(null);
        try {
            await api.patch(`/sales/${id}/cancel`);
            setSale((prev: SaleDetail | null) => (prev ? { ...prev, status: 'CANCELLED' } : prev));
            setIsConfirmOpen(false);
        } catch {
            setCancelError("Couldn't cancel this sale. Please try again.");
        } finally {
            setIsCancelling(false);
        }
    };

    if (state === 'loading') {
        return <SaleDetailSkeleton />;
    }

    if (state === 'error' || !sale) {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center px-6 text-center bg-canvas">
                <p className="text-gray-700 mb-4">Couldn't load this sale.</p>
                <button onClick={() => void loadSale()} className="h-11 px-6 rounded-2xl bg-gray-900 text-white font-medium">
                    Retry
                </button>
            </div>
        );
    }

    const isCancelled = sale.status.toUpperCase() === 'CANCELLED';

    return (
        <div className="min-h-screen bg-canvas px-5 pt-6 pb-28">
            {/* Back */}
            <button onClick={() => navigate(-1)} className="flex items-center gap-0.5 text-sm text-gray-500 mb-5">
                <ChevronLeft size={18} />
                Back
            </button>

            <p className="text-xs text-gray-400 mb-1">Sale #{sale.saleId}</p>
            <h1 className="text-lg font-bold text-gray-900 mb-5">{formatSaleDateTime(sale.saleDateTime)}</h1>

            {/* Summary card — row style lifted from the receipt mockup */}
            <div className="bg-white rounded-2xl shadow-sm mb-4 overflow-hidden">
                <Row label="Cashier" value={sale.cashierName} />
                <Row label="Payment Method" value={PAYMENT_LABELS[sale.paymentMethod.toUpperCase()] ?? sale.paymentMethod} />
                <Row
                    label="Status"
                    value={
                        <span
                            className={`text-xs px-2.5 py-1 rounded-full font-semibold ${
                                isCancelled ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                            }`}
                        >
              {isCancelled ? 'Cancelled' : 'Completed'}
            </span>
                    }
                    noBorder
                />
            </div>

            {/* Line items */}
            <div className="bg-white rounded-2xl p-4 shadow-sm mb-4 overflow-x-auto">
                <table className="w-full text-sm">
                    <thead>
                    <tr className="text-xs text-gray-400 text-left">
                        <th className="font-medium pb-2">Product</th>
                        <th className="font-medium pb-2 text-center w-10">Qty</th>
                        <th className="font-medium pb-2 text-right w-20">Price</th>
                        <th className="font-medium pb-2 text-right w-20">Subtotal</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                    {sale.items.map((item: SaleLineItem) => (
                        <tr key={item.productId}>
                            <td className="py-2 text-gray-900">{item.productName}</td>
                            <td className="py-2 text-center text-gray-600">{item.quantity}</td>
                            <td className="py-2 text-right text-gray-600">{currency.format(item.unitPrice)}</td>
                            <td className="py-2 text-right text-gray-900 font-medium">
                                {currency.format(item.subtotal)}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {/* Total — large, at bottom, per spec */}
            <div className="bg-white rounded-2xl p-4 shadow-sm mb-4 flex items-center justify-between">
                <span className="text-sm text-gray-500">Total Amount</span>
                <span className="text-2xl font-bold text-gray-900">{currency.format(sale.totalAmount)}</span>
            </div>

            {/* Admin: cancel sale */}
            {isAdmin && !isCancelled && (
                <button
                    onClick={() => {
                        setCancelError(null);
                        setIsConfirmOpen(true);
                    }}
                    className="w-full h-14 rounded-2xl bg-red-600 text-white font-semibold"
                >
                    Cancel Sale
                </button>
            )}

            {/* Confirmation modal */}
            {isConfirmOpen && (
                <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center">
                    <button
                        aria-label="Dismiss"
                        className="absolute inset-0 bg-black/40"
                        onClick={() => !isCancelling && setIsConfirmOpen(false)}
                    />
                    <div className="relative w-full sm:max-w-sm bg-white rounded-t-2xl sm:rounded-2xl p-5 pb-[calc(env(safe-area-inset-bottom)+1.25rem)]">
                        <h2 className="text-base font-bold text-gray-900 mb-2">Cancel this sale?</h2>
                        <p className="text-sm text-gray-500 mb-4">
                            This will mark the sale as cancelled. Stock will not be restored automatically.
                        </p>

                        {cancelError && <p className="text-sm text-red-600 mb-3">{cancelError}</p>}

                        <div className="space-y-2">
                            <button
                                onClick={() => void handleCancelSale()}
                                disabled={isCancelling}
                                className="w-full h-14 rounded-2xl bg-red-600 text-white font-semibold disabled:opacity-60"
                            >
                                {isCancelling ? 'Cancelling…' : 'Cancel Sale'}
                            </button>
                            <button
                                onClick={() => setIsConfirmOpen(false)}
                                disabled={isCancelling}
                                className="w-full h-14 rounded-2xl bg-white border border-gray-200 text-gray-700 font-semibold disabled:opacity-60"
                            >
                                Keep Sale
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

function Row({
                 label,
                 value,
                 noBorder,
             }: {
    label: string;
    value: ReactNode;
    noBorder?: boolean;
}) {
    return (
        <div className={`flex items-center justify-between px-4 py-3 ${noBorder ? '' : 'border-b border-gray-100'}`}>
            <span className="text-sm text-gray-500">{label}</span>
            <span className="text-sm font-semibold text-gray-900">{value}</span>
        </div>
    );
}

function SaleDetailSkeleton() {
    return (
        <div className="min-h-screen bg-canvas px-5 pt-6 pb-28 animate-pulse">
            <div className="h-4 w-16 bg-gray-200 rounded mb-5" />
            <div className="h-3 w-24 bg-gray-200 rounded mb-1" />
            <div className="h-5 w-48 bg-gray-200 rounded mb-5" />
            <div className="h-32 bg-gray-200 rounded-2xl mb-4" />
            <div className="h-40 bg-gray-200 rounded-2xl mb-4" />
            <div className="h-16 bg-gray-200 rounded-2xl" />
        </div>
    );
}
