import { useCallback, useEffect, useMemo, useState } from 'react';
import { BarChart3, PackageOpen, RefreshCw } from 'lucide-react';
import api from '../services/api';
import type { PerformanceReport } from '../types';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { formatCurrency } from '../utils/format';
import { getApiErrorMessage } from '../utils/api-error';

type Preset = 'today' | 'last7' | 'last30' | 'custom';
const paymentLabels: Record<string, string> = { CASH: 'Cash', CARD: 'Card', MOBILE_PAYMENT: 'Mobile payment' };

function localDate(date: Date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function rangeFor(preset: Exclude<Preset, 'custom'>) {
    const to = new Date();
    const from = new Date(to);
    if (preset === 'last7') from.setDate(from.getDate() - 6);
    if (preset === 'last30') from.setDate(from.getDate() - 29);
    return { from: localDate(from), to: localDate(to) };
}

function shortDate(value: string) {
    return new Date(`${value}T00:00:00`).toLocaleDateString('en-ZA', { day: 'numeric', month: 'short' });
}

export default function ReportsPage() {
    const initial = rangeFor('last7');
    const [preset, setPreset] = useState<Preset>('last7');
    const [from, setFrom] = useState(initial.from);
    const [to, setTo] = useState(initial.to);
    const [report, setReport] = useState<PerformanceReport | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const applyPreset = (next: Exclude<Preset, 'custom'>) => {
        const range = rangeFor(next);
        setPreset(next);
        setFrom(range.from);
        setTo(range.to);
    };

    const load = useCallback(async () => {
        if (!from || !to) return;
        if (from > to) {
            setError('The start date cannot be after the end date.');
            setLoading(false);
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const response = await api.get<PerformanceReport>('/reports/performance', { params: { from, to } });
            setReport(response.data);
        } catch (caught) {
            setError(getApiErrorMessage(caught, 'The report could not be loaded.'));
        } finally {
            setLoading(false);
        }
    }, [from, to]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- date changes intentionally refresh server data
        void load();
    }, [load]);

    const maxDailySales = useMemo(
        () => Math.max(1, ...(report?.dailySales.map((day) => day.totalSales) ?? [])),
        [report],
    );
    const maxPayment = useMemo(
        () => Math.max(1, ...(report?.payments.map((payment) => payment.totalAmount) ?? [])),
        [report],
    );

    return (
        <div className="app-page">
            <PageHeader title="Reports" description="See what the shop sold and which products perform best." />

            <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
                {([['today', 'Today'], ['last7', '7 days'], ['last30', '30 days']] as const).map(([value, label]) => (
                    <button key={value} type="button" onClick={() => applyPreset(value)} className={`min-h-11 shrink-0 rounded-full border px-4 text-sm font-semibold ${preset === value ? 'border-brand-700 bg-brand-700 text-white' : 'border-line bg-white text-muted hover:border-brand-500'}`}>{label}</button>
                ))}
            </div>

            <div className="mb-6 grid gap-3 sm:grid-cols-[1fr_auto_1fr]">
                <label><span className="mb-1.5 block text-sm font-medium text-ink">From</span><input type="date" value={from} max={to} onChange={(event) => { setFrom(event.target.value); setPreset('custom'); }} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label>
                <span className="hidden self-end pb-3 text-sm text-muted sm:block">to</span>
                <label><span className="mb-1.5 block text-sm font-medium text-ink">To</span><input type="date" value={to} min={from} max={localDate(new Date())} onChange={(event) => { setTo(event.target.value); setPreset('custom'); }} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label>
            </div>

            {loading && <LoadingState label="Preparing report" />}
            {error && <div role="alert" className="border-y border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700"><p>{error}</p><Button variant="secondary" className="mt-3" leadingIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void load()}>Try again</Button></div>}

            {!loading && !error && report && (
                <>
                    <section aria-label="Report summary" className="mb-8 grid grid-cols-2 border-y border-line lg:grid-cols-5">
                        <Metric label="Sales" value={formatCurrency(report.totalSales)} />
                        <Metric label="Profit" value={formatCurrency(report.totalProfit)} />
                        <Metric label="Transactions" value={String(report.transactionCount)} />
                        <Metric label="Units sold" value={String(report.unitsSold)} />
                        <Metric label="Average sale" value={formatCurrency(report.averageSale)} last />
                    </section>

                    {report.transactionCount === 0 ? (
                        <EmptyState icon={<BarChart3 className="h-8 w-8" />} title="No completed sales in this period" description="Choose a wider date range or complete a sale in POS to see performance." />
                    ) : (
                        <div className="grid gap-8 lg:grid-cols-[minmax(0,1.5fr)_minmax(17rem,0.8fr)]">
                            <section>
                                <SectionTitle title="Sales over time" detail={`${report.transactionCount} completed transaction${report.transactionCount === 1 ? '' : 's'}`} />
                                <div className="overflow-x-auto border-y border-line bg-white px-4 py-5">
                                    <div className="flex h-56 min-w-[34rem] items-end gap-2" role="img" aria-label="Daily sales bar chart">
                                        {report.dailySales.map((day) => (
                                            <div key={day.date} className="flex min-w-8 flex-1 flex-col items-center justify-end gap-2">
                                                <span className="text-[11px] font-medium text-muted">{day.totalSales > 0 ? formatCurrency(day.totalSales).replace(/,00$/, '') : ''}</span>
                                                <div className="flex h-36 w-full items-end rounded-t bg-brand-50">
                                                    <div title={`${shortDate(day.date)}: ${formatCurrency(day.totalSales)}`} className="w-full rounded-t bg-brand-700" style={{ height: `${Math.max(day.totalSales > 0 ? 5 : 0, (day.totalSales / maxDailySales) * 100)}%` }} />
                                                </div>
                                                <span className="whitespace-nowrap text-[11px] text-muted">{shortDate(day.date)}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </section>

                            <section>
                                <SectionTitle title="Payment methods" detail="Completed sales only" />
                                <div className="divide-y divide-line border-y border-line bg-white">
                                    {report.payments.map((payment) => (
                                        <div key={payment.paymentMethod} className="px-4 py-4">
                                            <div className="mb-2 flex items-baseline justify-between gap-3"><span className="font-medium text-ink">{paymentLabels[payment.paymentMethod]}</span><span className="font-semibold text-ink">{formatCurrency(payment.totalAmount)}</span></div>
                                            <div className="h-2 overflow-hidden rounded-full bg-gray-100"><div className="h-full rounded-full bg-brand-700" style={{ width: `${(payment.totalAmount / maxPayment) * 100}%` }} /></div>
                                            <p className="mt-2 text-xs text-muted">{payment.transactionCount} transaction{payment.transactionCount === 1 ? '' : 's'}</p>
                                        </div>
                                    ))}
                                </div>
                            </section>

                            <ProductRanking title="Top five by quantity" detail="Most units sold" products={report.topProductsByQuantity} value={product=>`${product.quantitySold} units`} />
                            <ProductRanking title="Top five by revenue" detail="Highest sales value" products={report.topProductsByRevenue} value={product=>formatCurrency(product.revenue)} />
                            <section className="lg:col-span-2">
                                <SectionTitle title="Stock requiring attention" detail={`${report.lowStockProducts.length} low-stock product${report.lowStockProducts.length===1?'':'s'}`} />
                                {report.lowStockProducts.length===0?<p className="border-y border-line bg-white px-4 py-5 text-sm text-muted">All active products are above their low-stock thresholds.</p>:<ul className="divide-y divide-line border-y border-line bg-white">{report.lowStockProducts.map(product=><li key={product.productId} className="flex items-center justify-between gap-4 px-4 py-4"><span className="font-medium text-ink">{product.productName}</span><span className="text-sm text-muted">{product.stockQuantity} remaining · alert at {product.lowStockThreshold}</span></li>)}</ul>}
                                <p className="mt-3 text-xs text-muted">Cancelled sales are excluded from performance totals.</p>
                            </section>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

function Metric({ label, value, last = false }: { label: string; value: string; last?: boolean }) {
    return <div className={`px-3 py-5 sm:px-5 ${last ? '' : 'border-r border-line'} [&:nth-child(2)]:border-r-0 lg:[&:nth-child(2)]:border-r`}><p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p><p className="mt-2 text-xl font-semibold text-ink sm:text-2xl">{value}</p></div>;
}

function SectionTitle({ title, detail }: { title: string; detail: string }) {
    return <div className="mb-3 flex items-end justify-between gap-4"><h2 className="text-lg font-semibold text-ink">{title}</h2><p className="text-xs text-muted">{detail}</p></div>;
}

function ProductRanking({title,detail,products,value}:{title:string;detail:string;products:PerformanceReport['topProducts'];value:(product:PerformanceReport['topProducts'][number])=>string}) {
    return <section><SectionTitle title={title} detail={detail} />{products.length===0?<EmptyState icon={<PackageOpen className="h-7 w-7" />} title="No product data" description="Product performance will appear after completed sales." />:<ol className="divide-y divide-line border-y border-line bg-white">{products.map((product,index)=><li key={product.productId} className="grid grid-cols-[2rem_1fr_auto] items-center gap-3 px-4 py-4"><span className="text-sm font-semibold text-brand-700">{index+1}</span><span className="min-w-0"><span className="block truncate font-medium text-ink">{product.productName}</span><span className="mt-0.5 block text-xs text-muted">{formatCurrency(product.revenue)} revenue</span></span><span className="text-sm font-semibold text-ink">{value(product)}</span></li>)}</ol>}</section>;
}
