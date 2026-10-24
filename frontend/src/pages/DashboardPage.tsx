import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    ArrowRight,
    CheckCircle2,
    ClipboardList,
    PackagePlus,
    RefreshCw,
    ShoppingCart,
    TriangleAlert,
} from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { DashboardData, Product, SaleSummary } from '../types';
import { Button } from '../components/ui/Button';
import { formatCurrency } from '../utils/format';

type LoadState = 'loading' | 'success' | 'error';

function toLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
}

function getTodayLabel(): string {
    return new Date().toLocaleDateString('en-ZA', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
    });
}

function formatSaleTime(value: string): string {
    return new Date(value).toLocaleTimeString('en-ZA', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    });
}

export default function DashboardPage() {
    const { currentUser } = useAuth();
    const [state, setState] = useState<LoadState>('loading');
    const [dashboard, setDashboard] = useState<DashboardData | null>(null);
    const [lowStock, setLowStock] = useState<Product[]>([]);
    const [recentSales, setRecentSales] = useState<SaleSummary[]>([]);
    const isAdmin = currentUser?.role === 'ADMIN';

    const loadDashboard = useCallback(async () => {
        setState('loading');
        try {
            const today = toLocalDateString(new Date());
            const [dashboardResponse, lowStockResponse, salesResponse] = await Promise.all([
                api.get<DashboardData>('/reports/dashboard'),
                api.get<Product[]>('/products/low-stock'),
                api.get<SaleSummary[]>('/sales', { params: { from: today, to: today } }),
            ]);

            setDashboard(dashboardResponse.data);
            setLowStock(lowStockResponse.data);
            setRecentSales(salesResponse.data.slice(0, 5));
            setState('success');
        } catch {
            setState('error');
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- initial data fetch owns the loading state
        void loadDashboard();
    }, [loadDashboard]);

    const transactionLabel = useMemo(() => {
        const count = dashboard?.transactionCount ?? 0;
        return `${count} transaction${count === 1 ? '' : 's'} today`;
    }, [dashboard?.transactionCount]);

    if (state === 'loading') return <DashboardSkeleton />;

    if (state === 'error') {
        return (
            <main className="app-page flex min-h-[70vh] items-center justify-center">
                <section className="w-full max-w-md border-y border-line py-10 text-center">
                    <p className="text-lg font-semibold text-ink">The dashboard could not be loaded</p>
                    <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-muted">
                        Check that the backend is running, then try again.
                    </p>
                    <Button
                        className="mt-6"
                        leadingIcon={<RefreshCw aria-hidden="true" className="h-4 w-4" />}
                        onClick={() => void loadDashboard()}
                    >
                        Try again
                    </Button>
                </section>
            </main>
        );
    }

    return (
        <main className="app-page">
            <header className="mb-7 flex items-start justify-between gap-4 sm:mb-9">
                <div className="min-w-0">
                    <p className="text-sm font-semibold text-brand-700">{getTodayLabel()}</p>
                    <h1 className="mt-1 text-2xl font-semibold tracking-tight text-ink sm:text-3xl">
                        {getGreeting()}, {currentUser?.username ?? 'there'}
                    </h1>
                    <p className="mt-1 text-sm text-muted">Here is how your shop is doing today.</p>
                </div>
                <span className="hidden shrink-0 border border-line px-3 py-1.5 text-xs font-semibold text-muted sm:inline-flex">
                    {isAdmin ? 'Owner view' : 'Cashier view'}
                </span>
            </header>

            <section aria-label="Today's performance" className="mb-8 grid grid-cols-2 border-y border-line sm:mb-10">
                <div className="border-r border-line py-5 pr-4 sm:py-6 sm:pr-8">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Sales</p>
                    <p className="mt-2 break-words text-2xl font-semibold tracking-tight text-ink sm:text-4xl">
                        {formatCurrency(dashboard?.totalSales)}
                    </p>
                    <p className="mt-2 text-xs text-muted sm:text-sm">{transactionLabel}</p>
                </div>
                <div className="py-5 pl-4 sm:py-6 sm:pl-8">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Profit</p>
                    <p className="mt-2 break-words text-2xl font-semibold tracking-tight text-ink sm:text-4xl">
                        {formatCurrency(dashboard?.totalProfit)}
                    </p>
                    <p className="mt-2 text-xs text-muted sm:text-sm">Estimated today</p>
                </div>
            </section>

            <section aria-labelledby="quick-actions-title" className="mb-9">
                <div className="mb-3 flex items-center justify-between">
                    <h2 id="quick-actions-title" className="text-sm font-semibold uppercase tracking-[0.12em] text-muted">Quick actions</h2>
                </div>
                <div className="grid gap-3 sm:grid-cols-3">
                    <Link
                        to="/pos"
                        className="flex min-h-16 items-center justify-between bg-brand-700 px-4 text-sm font-semibold text-white transition-colors hover:bg-brand-800 sm:min-h-20"
                    >
                        <span className="flex items-center gap-3">
                            <ShoppingCart aria-hidden="true" className="h-5 w-5" />
                            New sale
                        </span>
                        <ArrowRight aria-hidden="true" className="h-4 w-4" />
                    </Link>
                    <Link
                        to={isAdmin ? '/products/new' : '/products'}
                        className="flex min-h-14 items-center justify-between border border-line bg-white px-4 text-sm font-semibold text-ink transition-colors hover:border-brand-500 hover:text-brand-800 sm:min-h-20"
                    >
                        <span className="flex items-center gap-3">
                            <PackagePlus aria-hidden="true" className="h-5 w-5 text-brand-700" />
                            {isAdmin ? 'Add product' : 'View products'}
                        </span>
                        <ArrowRight aria-hidden="true" className="h-4 w-4 text-muted" />
                    </Link>
                    <Link
                        to={isAdmin ? '/stocktake' : '/sales'}
                        className="flex min-h-14 items-center justify-between border border-line bg-white px-4 text-sm font-semibold text-ink transition-colors hover:border-brand-500 hover:text-brand-800 sm:min-h-20"
                    >
                        <span className="flex items-center gap-3">
                            <ClipboardList aria-hidden="true" className="h-5 w-5 text-brand-700" />
                            {isAdmin ? 'Start stocktake' : 'Sales history'}
                        </span>
                        <ArrowRight aria-hidden="true" className="h-4 w-4 text-muted" />
                    </Link>
                </div>
            </section>

            <div className="grid gap-10 lg:grid-cols-[minmax(0,1.4fr)_minmax(17rem,0.75fr)] lg:gap-12">
                <section aria-labelledby="recent-sales-title">
                    <div className="flex min-h-11 items-center justify-between border-b border-line">
                        <h2 id="recent-sales-title" className="text-lg font-semibold text-ink">Recent sales</h2>
                        <Link to="/sales" className="inline-flex min-h-11 items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800">
                            View all <ArrowRight aria-hidden="true" className="h-4 w-4" />
                        </Link>
                    </div>

                    {recentSales.length === 0 ? (
                        <div className="border-b border-line py-8">
                            <p className="font-medium text-ink">No sales yet today</p>
                            <p className="mt-1 text-sm text-muted">Completed sales will appear here.</p>
                        </div>
                    ) : (
                        <ul className="divide-y divide-line border-b border-line">
                            {recentSales.map((sale) => (
                                <li key={sale.saleId}>
                                    <Link
                                        to={`/sales/${sale.saleId}`}
                                        className="grid min-h-16 grid-cols-[1fr_auto] items-center gap-4 py-3 text-sm hover:text-brand-800"
                                    >
                                        <div className="min-w-0">
                                            <p className="font-semibold text-ink">Sale at {formatSaleTime(sale.saleDateTime)}</p>
                                            <p className="mt-0.5 truncate text-xs capitalize text-muted">
                                                {sale.paymentMethod.toLowerCase().replace('_', ' ')} · {sale.status.toLowerCase()}
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <span className="font-semibold text-ink">{formatCurrency(sale.totalAmount)}</span>
                                            <ArrowRight aria-hidden="true" className="h-4 w-4 text-muted" />
                                        </div>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>

                <section aria-labelledby="stock-health-title">
                    <div className="flex min-h-11 items-center justify-between border-b border-line">
                        <h2 id="stock-health-title" className="text-lg font-semibold text-ink">Stock health</h2>
                        <Link to="/products" className="inline-flex min-h-11 items-center text-sm font-semibold text-brand-700 hover:text-brand-800">
                            Products
                        </Link>
                    </div>

                    {lowStock.length === 0 ? (
                        <div className="flex gap-3 border-b border-line py-5">
                            <CheckCircle2 aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-brand-700" />
                            <div>
                                <p className="font-semibold text-ink">Stock levels look healthy</p>
                                <p className="mt-1 text-sm leading-6 text-muted">No products are below their alert level.</p>
                            </div>
                        </div>
                    ) : (
                        <div className="border-b border-line">
                            <div className="flex gap-3 border-l-4 border-warning-500 px-4 py-4">
                                <TriangleAlert aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-warning-700" />
                                <p className="text-sm font-semibold text-ink">
                                    {lowStock.length} product{lowStock.length === 1 ? '' : 's'} need attention
                                </p>
                            </div>
                            <ul className="divide-y divide-line">
                                {lowStock.slice(0, 4).map((product) => (
                                    <li key={product.productId}>
                                        <Link
                                            to={isAdmin ? `/products/${product.productId}/edit` : '/products'}
                                            className="flex min-h-14 items-center justify-between gap-4 px-4 text-sm hover:text-brand-800"
                                        >
                                            <span className="min-w-0 truncate font-medium text-ink">{product.name}</span>
                                            <span className="shrink-0 text-warning-700">{product.stockQuantity} left</span>
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </section>
            </div>
        </main>
    );
}

function DashboardSkeleton() {
    return (
        <main className="app-page animate-pulse" aria-label="Loading dashboard">
            <div className="mb-8">
                <div className="h-3.5 w-32 bg-gray-200" />
                <div className="mt-3 h-8 w-64 max-w-full bg-gray-200" />
                <div className="mt-2 h-3.5 w-52 max-w-full bg-gray-100" />
            </div>
            <div className="mb-9 grid grid-cols-2 border-y border-line">
                <div className="border-r border-line py-6 pr-5">
                    <div className="h-3 w-16 bg-gray-200" />
                    <div className="mt-3 h-8 w-32 max-w-full bg-gray-200" />
                </div>
                <div className="py-6 pl-5">
                    <div className="h-3 w-16 bg-gray-200" />
                    <div className="mt-3 h-8 w-32 max-w-full bg-gray-200" />
                </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
                <div className="h-16 bg-brand-100 sm:h-20" />
                <div className="h-16 border border-line bg-white sm:h-20" />
                <div className="h-16 border border-line bg-white sm:h-20" />
            </div>
            <div className="mt-10 grid gap-10 lg:grid-cols-2">
                <div className="h-48 border-y border-line bg-white" />
                <div className="h-40 border-y border-line bg-white" />
            </div>
            <span className="sr-only">Loading dashboard</span>
        </main>
    );
}