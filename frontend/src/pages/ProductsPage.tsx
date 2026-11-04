import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, FolderCog, Package, Plus, Search } from 'lucide-react';
import api from '../services/api';
import type { Category, Product } from '../types';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { getApiErrorMessage } from '../utils/api-error';
import { formatCurrency } from '../utils/format';


export default function ProductsPage() {
    const { currentUser } = useAuth();
    const admin = currentUser?.role === 'ADMIN';
    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [search, setSearch] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [showInactive, setShowInactive] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        setLoading(true); setError(null);
        try {
            const [productResponse, categoryResponse] = await Promise.all([
                api.get<Product[]>(showInactive ? '/products/admin/all' : '/products', { params: { search: search.trim() || undefined, categoryId: categoryId || undefined } }),
                api.get<Category[]>('/categories'),
            ]);
            setProducts(productResponse.data); setCategories(categoryResponse.data);
        } catch (caught) { setError(getApiErrorMessage(caught, 'Products could not be loaded.')); }
        finally { setLoading(false); }
    }, [categoryId, search, showInactive]);

    useEffect(() => { const timer = window.setTimeout(() => void load(), 250); return () => window.clearTimeout(timer); }, [load]);

    return <div className="app-page">
        <PageHeader title="Products" description="Search prices and keep the shop catalogue accurate." action={admin ? <Link to="/products/new"><Button leadingIcon={<Plus className="h-5 w-5" />}><span className="hidden sm:inline">Add product</span><span className="sm:hidden">Add</span></Button></Link> : undefined} />
        <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_15rem]">
            <label className="relative block"><span className="sr-only">Search products</span><Search className="pointer-events-none absolute left-3.5 top-3.5 h-5 w-5 text-muted" /><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search name or barcode" className="min-h-12 w-full rounded-lg border border-line bg-white pl-11 pr-4 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label>
            <select aria-label="Filter by category" value={categoryId} onChange={e => setCategoryId(e.target.value)} className="min-h-12 rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100"><option value="">All categories</option>{categories.map(c => <option key={c.categoryId} value={c.categoryId}>{c.name}</option>)}</select>
        </div>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3 text-sm text-muted"><span>{products.length} {products.length === 1 ? 'product' : 'products'}</span>{admin && <div className="flex items-center gap-4"><label className="inline-flex min-h-11 cursor-pointer items-center gap-2"><input type="checkbox" checked={showInactive} onChange={event=>setShowInactive(event.target.checked)} className="h-4 w-4 accent-brand-700" />Include inactive</label><Link to="/products/categories" className="inline-flex min-h-11 items-center gap-2 font-semibold text-brand-700 hover:text-brand-900"><FolderCog className="h-4 w-4" />Categories</Link></div>}</div>
        {loading && <LoadingState label="Loading products" />}
        {error && <div role="alert" className="border-y border-red-200 bg-red-50 p-4 text-sm text-red-700"><p>{error}</p><Button variant="secondary" className="mt-3" onClick={() => void load()}>Try again</Button></div>}
        {!loading && !error && products.length === 0 && <EmptyState icon={<Package className="h-8 w-8" />} title="No products found" description={search || categoryId ? 'No active product matches this search. Check the barcode or clear the filters.' : 'Add the first item sold in this shop.'} action={admin && !search && !categoryId ? <Link to="/products/new"><Button>Add product</Button></Link> : undefined} />}
        {!loading && !error && products.length > 0 && <div className="border-y border-line bg-white"><div className="hidden grid-cols-[1fr_10rem_8rem_7rem] gap-4 border-b border-line px-4 py-3 text-xs font-semibold uppercase tracking-wide text-muted md:grid"><span>Product</span><span>Category</span><span>Price</span><span>Stock</span></div><ul className="divide-y divide-line">{products.map(p => <li key={p.productId}><Link to={admin ? `/products/${p.productId}/edit` : '#'} className={`grid gap-2 px-4 py-4 hover:bg-brand-50 md:grid-cols-[1fr_10rem_8rem_7rem] md:items-center md:gap-4 ${!p.isActive?'bg-gray-50 text-muted':''}`}><div className="min-w-0"><div className="flex items-center gap-2"><p className="truncate font-semibold text-ink">{p.name}</p>{!p.isActive&&<StatusBadge tone="neutral">Inactive</StatusBadge>}</div><p className="mt-0.5 text-xs text-muted">{p.barcode || 'No barcode'}</p></div><p className="text-sm text-muted">{p.categoryName || 'Uncategorised'}</p><p className="font-semibold text-ink">{formatCurrency(p.sellingPrice)}</p><div className="flex items-center gap-2"><span className="text-sm font-medium">{p.stockQuantity}</span>{p.isActive&&p.lowStock && <StatusBadge tone="warning"><AlertTriangle className="mr-1 h-3.5 w-3.5" />Low</StatusBadge>}</div></Link></li>)}</ul></div>}
    </div>;
}
