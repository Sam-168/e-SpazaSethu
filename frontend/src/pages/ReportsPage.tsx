import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import api from '../services/api';
import type { LowStockProduct, SalesReportSummary, TopProductReport } from '../types';

type Preset = 'today' | 'last7' | 'month' | 'custom';

interface ReportData {
    summary: SalesReportSummary;
    byQuantity: TopProductReport[];
    byRevenue: TopProductReport[];
    lowStock: LowStockProduct[];
}

const currency = new Intl.NumberFormat('en-ZA', {
    style: 'currency',
    currency: 'ZAR',
    minimumFractionDigits: 2,
});

const toDateInput = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

const getPresetRange = (preset: Exclude<Preset, 'custom'>) => {
    const today = new Date();
    const from = new Date(today);

    if (preset === 'last7') {
        from.setDate(today.getDate() - 6);
    } else if (preset === 'month') {
        from.setDate(1);
    }

    return { from: toDateInput(from), to: toDateInput(today) };
};

const presets: Array<{ value: Exclude<Preset, 'custom'>; label: string }> = [
    { value: 'today', label: 'Today' },
    { value: 'last7', label: 'Last 7 days' },
    { value: 'month', label: 'This month' },
];

export default function ReportsPage() {
    const initialRange = getPresetRange('last7');
    const [preset, setPreset] = useState<Preset>('last7');
    const [from, setFrom] = useState(initialRange.from);
    const [to, setTo] = useState(initialRange.to);
    const [data, setData] = useState<ReportData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadReports = useCallback(async () => {
        if (!from || !to || from > to) {
            setError('Choose a valid date range. The start date must be before the end date.');
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const params = { from, to };
            const [summary, byQuantity, byRevenue, lowStock] = await Promise.all([
                api.get<SalesReportSummary>('/reports/summary', { params }),
                api.get<TopProductReport[]>('/reports/top-products/by-quantity', { params }),
                api.get<TopProductReport[]>('/reports/top-products/by-revenue', { params }),
                api.get<LowStockProduct[]>('/products/low-stock'),
            ]);

            setData({
                summary: summary.data,
                byQuantity: byQuantity.data,
                byRevenue: byRevenue.data,
                lowStock: lowStock.data,
            });
        } catch {
            setError("Couldn't load the reports. Check your connection and try again.");
        } finally {
            setLoading(false);
        }
    }, [from, to]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- initial report fetch owns loading state
        void loadReports();
    }, [loadReports]);

    const applyPreset = (nextPreset: Exclude<Preset, 'custom'>) => {
        const range = getPresetRange(nextPreset);
        setPreset(nextPreset);
        setFrom(range.from);
        setTo(range.to);
    };

    return (
        <main className="min-h-screen bg-[#FCFCFB] px-4 pb-28 pt-6 text-gray-950 sm:px-6 lg:px-10">
            <div className="mx-auto max-w-6xl">
                <header className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <p className="mb-1 text-xs font-semibold uppercase tracking-[0.18em] text-[#005f7b]">
                            Store performance
                        </p>
                        <h1 className="text-3xl font-semibold tracking-tight">Reports</h1>
                        <p className="mt-1 text-sm text-gray-500">Sales, profit and product movement for the selected period.</p>
                    </div>
                    <button
                        type="button"
                        onClick={() => void loadReports()}
                        disabled={loading}
                        className="inline-flex h-10 items-center justify-center gap-2 self-start border border-gray-300 px-4 text-sm font-medium transition hover:border-[#005f7b] hover:text-[#005f7b] disabled:cursor-not-allowed disabled:opacity-50 sm:self-auto"
                    >
                        <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                        Refresh
                    </button>
                </header>

                <section aria-label="Report period" className="mb-8 border-y border-gray-200 py-5">
                    <div className="mb-4 flex flex-wrap gap-2">
                        {presets.map(({ value, label }) => (
                            <button
                                key={value}
                                type="button"
                                aria-pressed={preset === value}
                                onClick={() => applyPreset(value)}
                                className={`h-9 px-4 text-sm font-medium transition ${
                                    preset === value
                                        ? 'bg-[#005f7b] text-white'
                                        : 'border border-gray-300 bg-white text-gray-700 hover:border-[#005f7b]'
                                }`}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                    <div className="grid gap-3 sm:max-w-xl sm:grid-cols-[1fr_auto_1fr] sm:items-end">
                        <label className="text-sm font-medium">
                            <span className="mb-1.5 block text-xs text-gray-500">From</span>
                            <input
                                type="date"
                                value={from}
                                max={to}
                                onChange={(event) => {
                                    setFrom(event.target.value);
                                    setPreset('custom');
                                }}
                                className="h-11 w-full border border-gray-300 bg-white px-3 outline-none focus:border-[#005f7b] focus:ring-1 focus:ring-[#005f7b]"
                            />
                        </label>
                        <span className="hidden pb-3 text-sm text-gray-400 sm:block">to</span>
                        <label className="text-sm font-medium">
                            <span className="mb-1.5 block text-xs text-gray-500">To</span>
                            <input
                                type="date"
                                value={to}
                                min={from}
                                onChange={(event) => {
                                    setTo(event.target.value);
                                    setPreset('custom');
                                }}
                                className="h-11 w-full border border-gray-300 bg-white px-3 outline-none focus:border-[#005f7b] focus:ring-1 focus:ring-[#005f7b]"
                            />
                        </label>
                    </div>
                </section>

                {error && (
                    <div className="mb-8 flex items-start gap-3 border-l-4 border-red-600 bg-red-50 p-4 text-sm text-red-800" role="alert">
                        <AlertTriangle className="mt-0.5 shrink-0" size={18} />
                        <div>
                            <p>{error}</p>
                            <button type="button" onClick={() => void loadReports()} className="mt-2 font-semibold underline">
                                Try again
                            </button>
                        </div>
                    </div>
                )}

                {loading && !data ? (
                    <ReportsSkeleton />
                ) : data ? (
                    <>
                        <section aria-label="Report totals" className="mb-10 grid border-y border-gray-200 sm:grid-cols-2">
                            <div className="py-5 sm:border-r sm:border-gray-200 sm:pr-8">
                                <p className="text-sm text-gray-500">Sales</p>
                                <p className="mt-1 text-3xl font-semibold tracking-tight">
                                    {currency.format(data.summary.totalAmount)}
                                </p>
                            </div>
                            <div className="border-t border-gray-200 py-5 sm:border-t-0 sm:pl-8">
                                <p className="text-sm text-gray-500">Estimated profit</p>
                                <p className="mt-1 text-3xl font-semibold tracking-tight text-[#005f7b]">
                                    {currency.format(data.summary.totalProfit)}
                                </p>
                            </div>
                        </section>

                        <div className="mb-12 grid gap-10 lg:grid-cols-2 lg:gap-14">
                            <RankingList
                                title="Top products by quantity"
                                caption="Units sold"
                                items={data.byQuantity}
                                value={(item) => `${item.quantitySold} unit${item.quantitySold === 1 ? '' : 's'}`}
                            />
                            <RankingList
                                title="Top products by revenue"
                                caption="Sales value"
                                items={data.byRevenue}
                                value={(item) => currency.format(item.revenue)}
                            />
                        </div>

                        <section>
                            <div className="mb-3 flex items-baseline justify-between gap-4">
                                <div>
                                    <h2 className="text-lg font-semibold">Stock requiring attention</h2>
                                    <p className="text-sm text-gray-500">Current stock position, independent of the report period.</p>
                                </div>
                                <span className="text-sm font-semibold text-[#005f7b]">{data.lowStock.length}</span>
                            </div>
                            {data.lowStock.length === 0 ? (
                                <p className="border-t border-gray-200 py-5 text-sm text-gray-500">All stock levels are healthy.</p>
                            ) : (
                                <ul className="border-t border-gray-300">
                                    {data.lowStock.map((product) => (
                                        <li key={product.productId} className="grid grid-cols-[1fr_auto] gap-4 border-b border-gray-200 py-4 text-sm">
                                            <span className="font-medium">{product.name}</span>
                                            <span className="text-right text-gray-600">
                                                {product.stockQuantity} remaining · threshold {product.lowStockThreshold}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    </>
                ) : null}
            </div>
        </main>
    );
}

function RankingList({
    title,
    caption,
    items,
    value,
}: {
    title: string;
    caption: string;
    items: TopProductReport[];
    value: (item: TopProductReport) => string;
}) {
    return (
        <section>
            <div className="mb-3 flex items-baseline justify-between gap-4">
                <h2 className="text-lg font-semibold">{title}</h2>
                <span className="text-xs uppercase tracking-wide text-gray-400">{caption}</span>
            </div>
            {items.length === 0 ? (
                <p className="border-t border-gray-200 py-5 text-sm text-gray-500">No completed sales in this period.</p>
            ) : (
                <ol className="border-t border-gray-300">
                    {items.map((item, index) => (
                        <li key={item.productId} className="grid grid-cols-[2rem_1fr_auto] items-center gap-3 border-b border-gray-200 py-4">
                            <span className="text-sm font-semibold text-[#005f7b]">{String(index + 1).padStart(2, '0')}</span>
                            <div className="min-w-0">
                                <p className="truncate text-sm font-medium">{item.productName}</p>
                                <p className="text-xs text-gray-500">{currency.format(item.revenue)} revenue</p>
                            </div>
                            <span className="text-right text-sm font-semibold">{value(item)}</span>
                        </li>
                    ))}
                </ol>
            )}
        </section>
    );
}

function ReportsSkeleton() {
    return (
        <div className="animate-pulse" aria-label="Loading reports">
            <div className="mb-10 grid border-y border-gray-200 sm:grid-cols-2">
                <div className="h-24 py-5 sm:border-r sm:border-gray-200 sm:pr-8"><div className="h-4 w-20 bg-gray-200" /></div>
                <div className="h-24 border-t border-gray-200 py-5 sm:border-t-0 sm:pl-8"><div className="h-4 w-20 bg-gray-200" /></div>
            </div>
            <div className="grid gap-10 lg:grid-cols-2">
                <div className="h-64 bg-gray-100" />
                <div className="h-64 bg-gray-100" />
            </div>
        </div>
    );
}
