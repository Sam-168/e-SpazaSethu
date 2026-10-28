import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList, PackagePlus, ShoppingCart } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { DashboardData, LowStockProduct, SaleSummary } from '../types';

type LoadState = 'loading' | 'success' | 'error';

const currency = new Intl.NumberFormat('en-ZA', {
    style: 'currency',
    currency: 'ZAR',
    minimumFractionDigits: 2,
});

function getGreeting() {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
}

const todayLabel = new Date().toLocaleDateString('en-ZA', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
});

export default function DashboardPage() {
    const { currentUser } = useAuth();

    const [state, setState] = useState<LoadState>('loading');
    const [dashboard, setDashboard] = useState<DashboardData | null>(null);
    const [lowStock, setLowStock] = useState<LowStockProduct[]>([]);
    const [recentSales, setRecentSales] = useState<SaleSummary[]>([]);

    const loadDashboard = useCallback(async () => {
        setState('loading');
        try {
            const today = new Date().toISOString().split('T')[0];
            const [dashboardRes, lowStockRes, salesRes] = await Promise.all([
                api.get<DashboardData>('/reports/dashboard'),
                api.get<LowStockProduct[]>('/products/low-stock'),
                api.get<SaleSummary[]>('/sales', { params: { from: today, to: today } }),
            ]);
            setDashboard(dashboardRes.data);
            setLowStock(lowStockRes.data);
            setRecentSales(salesRes.data.slice(0, 5));
            setState('success');
        } catch {
            setState('error');
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- intentional: resets to 'loading' on every mount/refetch
        void loadDashboard();
    }, [loadDashboard]);

    if (state === 'loading') {
        return <DashboardSkeleton />;
    }

    if (state === 'error') {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center px-6 text-center bg-[#F7F5F0]">
                <p className="text-gray-700 mb-4">
                    Couldn't load the dashboard. Check your connection and try again.
                </p>
                <button
                    onClick={() => void loadDashboard()}
                    className="h-11 px-6 rounded-xl bg-gray-900 text-white font-medium"
                >
                    Retry
                </button>
            </div>
        );
    }

    const transactionCount = dashboard?.transactionCount ?? 0;
    const hasProfit = dashboard?.totalProfit != null;

    return (
        <div className="min-h-screen bg-[#F7F5F0] px-4 pt-6 pb-28">
            {/* Greeting header */}
            <div className="mb-5">
                <h1 className="text-xl font-semibold text-gray-900">
                    {getGreeting()}, {currentUser?.username ?? 'there'}
                </h1>
                <p className="text-sm text-gray-500">{todayLabel}</p>
            </div>

            {/* Key metrics */}
            <div className="grid grid-cols-2 gap-3 mb-2">
                <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <p className="text-xs text-gray-500 mb-1">Today's Sales</p>
                    <p className="text-2xl font-bold text-gray-900 leading-tight">
                        {currency.format(dashboard?.totalSales ?? 0)}
                    </p>
                </div>
                <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <p className="text-xs text-gray-500 mb-1">Today's Profit</p>
                    <p className="text-2xl font-bold text-gray-900 leading-tight">
                        {hasProfit ? currency.format(dashboard!.totalProfit) : '—'}
                    </p>
                </div>
            </div>
            <p className="text-sm text-gray-500 mb-6">
                {transactionCount} transaction{transactionCount === 1 ? '' : 's'} today
            </p>

            {/* Low-stock alert panel */}
            <div className="mb-6">
                {lowStock.length > 0 ? (
                    <div className="bg-red-50 border border-red-200 rounded-2xl p-4">
                        <p className="text-sm font-semibold text-red-700 mb-2">
                            {lowStock.length} product{lowStock.length === 1 ? '' : 's'} running low
                        </p>
                        <ul className="divide-y divide-red-100">
                            {lowStock.map((product) => (
                                <li key={product.productId}>
                                    <Link
                                        to={`/products/${product.productId}/edit`}
                                        className="flex items-center justify-between py-2 text-sm text-red-800"
                                    >
                                        <span>{product.name}</span>
                                        <span className="font-medium">{product.stockQuantity} left</span>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </div>
                ) : (
                    <div className="bg-green-50 border border-green-200 rounded-2xl p-4">
                        <p className="text-sm text-green-700">All stock levels are healthy.</p>
                    </div>
                )}
            </div>

            {/* Quick actions */}
            <div className="grid grid-cols-3 gap-3 mb-6">
                <Link
                    to="/pos"
                    className="flex flex-col items-center justify-center h-20 rounded-2xl bg-gray-900 text-white text-xs font-medium gap-1 text-center px-1"
                >
                    <ShoppingCart size={20} />
                    New Sale
                </Link>
                <Link
                    to="/products/new"
                    className="flex flex-col items-center justify-center h-20 rounded-2xl bg-white border border-gray-200 text-gray-700 text-xs font-medium gap-1 text-center px-1"
                >
                    <PackagePlus size={20} />
                    Add Product
                </Link>
                <Link
                    to="/stocktake"
                    className="flex flex-col items-center justify-center h-20 rounded-2xl bg-white border border-gray-200 text-gray-700 text-xs font-medium gap-1 text-center px-1"
                >
                    <ClipboardList size={20} />
                    Start Stocktake
                </Link>
            </div>

            {/* Recent sales */}
            <div>
                <h2 className="text-sm font-semibold text-gray-900 mb-2">Recent Sales</h2>
                {recentSales.length === 0 ? (
                    <div className="bg-white rounded-2xl p-4">
                        <p className="text-sm text-gray-500">No sales recorded today yet.</p>
                    </div>
                ) : (
                    <ul className="bg-white rounded-2xl divide-y divide-gray-100 overflow-hidden">
                        {recentSales.map((sale) => (
                            <li key={sale.saleId}>
                                <Link to={`/sales/${sale.saleId}`} className="flex items-center justify-between px-4 py-3">
                                    <div>
                                        <p className="text-sm text-gray-900">
                                            {new Date(sale.saleDateTime).toLocaleTimeString('en-ZA', {
                                                hour: '2-digit',
                                                minute: '2-digit',
                                            })}
                                        </p>
                                        <p className="text-xs text-gray-500 capitalize">
                                            {sale.paymentMethod.toLowerCase()}
                                        </p>
                                    </div>
                                    <p className="text-sm font-semibold text-gray-900">
                                        {currency.format(sale.totalAmount)}
                                    </p>
                                </Link>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}

function DashboardSkeleton() {
    return (
        <div className="min-h-screen bg-[#F7F5F0] px-4 pt-6 pb-28 animate-pulse">
            <div className="h-5 w-40 bg-gray-200 rounded mb-2" />
            <div className="h-3 w-28 bg-gray-200 rounded mb-6" />
            <div className="grid grid-cols-2 gap-3 mb-2">
                <div className="h-20 bg-gray-200 rounded-2xl" />
                <div className="h-20 bg-gray-200 rounded-2xl" />
            </div>
            <div className="h-3 w-24 bg-gray-200 rounded mb-6" />
            <div className="h-16 bg-gray-200 rounded-2xl mb-6" />
            <div className="grid grid-cols-3 gap-3 mb-6">
                <div className="h-20 bg-gray-200 rounded-2xl" />
                <div className="h-20 bg-gray-200 rounded-2xl" />
                <div className="h-20 bg-gray-200 rounded-2xl" />
            </div>
            <div className="h-40 bg-gray-200 rounded-2xl" />
        </div>
    );
}
