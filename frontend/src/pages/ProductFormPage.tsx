import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';
import type { Category, Product } from '../types';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { TextField } from '../components/ui/TextField';
import { getApiErrorMessage } from '../utils/api-error';

interface FormState { name: string; barcode: string; sellingPrice: string; costPrice: string; stockQuantity: string; lowStockThreshold: string; categoryId: string; description: string }
const empty: FormState = { name: '', barcode: '', sellingPrice: '', costPrice: '', stockQuantity: '0', lowStockThreshold: '5', categoryId: '', description: '' };

export default function ProductFormPage() {
    const { id } = useParams(); const editing = Boolean(id); const navigate = useNavigate();
    const [form, setForm] = useState(empty); const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(editing); const [saving, setSaving] = useState(false); const [error, setError] = useState<string | null>(null); const [confirm, setConfirm] = useState(false);
    const set = (key: keyof FormState, value: string) => setForm(current => ({ ...current, [key]: value }));
    useEffect(() => { Promise.all([api.get<Category[]>('/categories'), editing ? api.get<Product>(`/products/${id}`) : Promise.resolve(null)]).then(([cats, product]) => { setCategories(cats.data); if (product) { const p=product.data; setForm({ name:p.name, barcode:p.barcode || '', sellingPrice:String(p.sellingPrice), costPrice:p.costPrice == null ? '' : String(p.costPrice), stockQuantity:String(p.stockQuantity), lowStockThreshold:p.lowStockThreshold == null ? '' : String(p.lowStockThreshold), categoryId:p.categoryId || '', description:p.description || '' }); } }).catch(c => setError(getApiErrorMessage(c, 'Product details could not be loaded.'))).finally(() => setLoading(false)); }, [editing, id]);
    const submit = async (event: FormEvent) => { event.preventDefault(); if (!form.name.trim() || Number(form.sellingPrice) <= 0) { setError('Enter a product name and a selling price greater than zero.'); return; } setSaving(true); setError(null); const payload = { name:form.name.trim(), barcode:form.barcode.trim() || null, sellingPrice:Number(form.sellingPrice), costPrice:form.costPrice === '' ? null : Number(form.costPrice), lowStockThreshold:form.lowStockThreshold === '' ? null : Number(form.lowStockThreshold), categoryId:form.categoryId || null, description:form.description.trim() || null, ...(!editing ? { stockQuantity:Number(form.stockQuantity) } : {}) }; try { if (editing) await api.put(`/products/${id}`, payload); else await api.post('/products', payload); navigate('/products'); } catch (caught) { setError(getApiErrorMessage(caught, 'The product could not be saved.')); } finally { setSaving(false); } };
    const deactivate = async () => { setSaving(true); try { await api.patch(`/products/${id}/deactivate`); navigate('/products'); } catch(caught) { setError(getApiErrorMessage(caught, 'The product could not be deactivated.')); setConfirm(false); } finally { setSaving(false); } };
    if (loading) return <div className="app-page"><LoadingState label="Loading product" /></div>;
    return <div className="app-page"><PageHeader title={editing ? 'Edit product' : 'Add product'} description={editing ? 'Update the catalogue details customers and cashiers rely on.' : 'Add an item to the shop catalogue.'} onBack={() => navigate('/products')} />
        <form onSubmit={submit} className="mx-auto max-w-2xl space-y-5" noValidate>
            <TextField label="Product name" hint="Include the brand and size, for example Coca-Cola 500ml." value={form.name} onChange={e=>set('name',e.target.value)} required maxLength={200} />
            <div className="grid gap-4 sm:grid-cols-2"><TextField label="Barcode (optional)" value={form.barcode} onChange={e=>set('barcode',e.target.value)} maxLength={100} /><div><label className="mb-1.5 block text-sm font-medium text-ink">Category</label><select value={form.categoryId} onChange={e=>set('categoryId',e.target.value)} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100"><option value="">Uncategorised</option>{categories.map(c=><option key={c.categoryId} value={c.categoryId}>{c.name}</option>)}</select></div></div>
            <div className="grid gap-4 sm:grid-cols-2"><TextField label="Selling price (R)" type="number" min="0.01" step="0.01" value={form.sellingPrice} onChange={e=>set('sellingPrice',e.target.value)} /><TextField label="Cost price (R, optional)" type="number" min="0" step="0.01" value={form.costPrice} onChange={e=>set('costPrice',e.target.value)} /></div>
            <div className="grid gap-4 sm:grid-cols-2">{!editing && <TextField label="Opening stock" type="number" min="0" step="1" value={form.stockQuantity} onChange={e=>set('stockQuantity',e.target.value)} />}<TextField label="Low-stock alert at" type="number" min="0" step="1" value={form.lowStockThreshold} onChange={e=>set('lowStockThreshold',e.target.value)} /></div>
            {editing && <p className="border-l-4 border-brand-700 bg-brand-50 px-4 py-3 text-sm text-brand-900">Current stock is adjusted through audited stock movements, not by editing product details.</p>}
            <div><label className="mb-1.5 block text-sm font-medium text-ink">Description (optional)</label><textarea value={form.description} onChange={e=>set('description',e.target.value)} maxLength={2000} rows={4} className="w-full rounded-lg border border-line bg-white p-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></div>
            {error && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
            <div className="flex flex-col-reverse gap-3 border-t border-line pt-5 sm:flex-row sm:justify-between">{editing ? <Button type="button" variant="danger" onClick={()=>setConfirm(true)}>Deactivate product</Button> : <span />}<div className="grid grid-cols-2 gap-3 sm:flex"><Button type="button" variant="secondary" onClick={()=>navigate('/products')}>Cancel</Button><Button type="submit" loading={saving}>{saving ? 'Saving' : 'Save product'}</Button></div></div>
        </form><Dialog open={confirm} title="Deactivate this product?" description="It will no longer appear in the catalogue or at checkout. Existing sales records stay unchanged." confirmLabel="Deactivate" confirmVariant="danger" busy={saving} onConfirm={()=>void deactivate()} onClose={()=>setConfirm(false)} /></div>;
}
