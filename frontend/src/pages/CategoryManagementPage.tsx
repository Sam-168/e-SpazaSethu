import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Folder, Pencil, Plus, Trash2 } from 'lucide-react';
import api from '../services/api';
import type { Category } from '../types';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { TextField } from '../components/ui/TextField';
import { getApiErrorMessage } from '../utils/api-error';

export default function CategoryManagementPage() {
    const navigate=useNavigate(); const [items,setItems]=useState<Category[]>([]); const [loading,setLoading]=useState(true); const [error,setError]=useState<string|null>(null);
    const [editing,setEditing]=useState<Category|null>(null); const [open,setOpen]=useState(false); const [name,setName]=useState(''); const [description,setDescription]=useState(''); const [saving,setSaving]=useState(false); const [deleting,setDeleting]=useState<Category|null>(null);
    const load=async()=>{setLoading(true);try{const {data}=await api.get<Category[]>('/categories');setItems(data);}catch(c){setError(getApiErrorMessage(c,'Categories could not be loaded.'));}finally{setLoading(false);}};
    useEffect(()=>{ let active=true; api.get<Category[]>('/categories').then(({data})=>{if(active)setItems(data);}).catch(c=>{if(active)setError(getApiErrorMessage(c,'Categories could not be loaded.'));}).finally(()=>{if(active)setLoading(false);}); return ()=>{active=false;}; },[]);
    const show=(category?:Category)=>{setEditing(category||null);setName(category?.name||'');setDescription(category?.description||'');setError(null);setOpen(true);};
    const save=async(e:FormEvent)=>{e.preventDefault();if(!name.trim()){setError('Enter a category name.');return;}setSaving(true);setError(null);try{if(editing)await api.put(`/categories/${editing.categoryId}`,{name:name.trim(),description:description.trim()||null});else await api.post('/categories',{name:name.trim(),description:description.trim()||null});setOpen(false);await load();}catch(c){setError(getApiErrorMessage(c,'The category could not be saved.'));}finally{setSaving(false);}};
    const remove=async()=>{if(!deleting)return;setSaving(true);try{await api.delete(`/categories/${deleting.categoryId}`);setDeleting(null);await load();}catch(c){setError(getApiErrorMessage(c,'This category could not be deleted. Remove it from active products first.'));setDeleting(null);}finally{setSaving(false);}};
    return <div className="app-page"><PageHeader title="Categories" description="Organise products so they are quicker to find." onBack={()=>navigate('/products')} action={<Button onClick={()=>show()} leadingIcon={<Plus className="h-5 w-5" />}>Add</Button>} />
        {error&&!open&&<p role="alert" className="mb-4 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}{loading&&<LoadingState label="Loading categories" />}{!loading&&items.length===0&&<EmptyState icon={<Folder className="h-8 w-8" />} title="No categories yet" description="Create a category such as Drinks, Bread or Household." />}
        {!loading&&items.length>0&&<ul className="divide-y divide-line border-y border-line bg-white">{items.map(c=><li key={c.categoryId} className="flex items-center gap-3 px-4 py-4"><div className="min-w-0 flex-1"><p className="font-semibold text-ink">{c.name}</p>{c.description&&<p className="mt-0.5 text-sm text-muted">{c.description}</p>}</div><button aria-label={`Edit ${c.name}`} onClick={()=>show(c)} className="grid h-11 w-11 place-items-center rounded-lg text-muted hover:bg-gray-100"><Pencil className="h-5 w-5" /></button><button aria-label={`Delete ${c.name}`} onClick={()=>setDeleting(c)} className="grid h-11 w-11 place-items-center rounded-lg text-red-700 hover:bg-red-50"><Trash2 className="h-5 w-5" /></button></li>)}</ul>}
        <Dialog open={open} title={editing?'Edit category':'Add category'} onClose={()=>!saving&&setOpen(false)}><form onSubmit={save} className="space-y-4"><TextField label="Category name" value={name} onChange={e=>setName(e.target.value)} maxLength={100}/><TextField label="Description (optional)" value={description} onChange={e=>setDescription(e.target.value)} maxLength={500}/>{error&&<p role="alert" className="bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}<div className="grid grid-cols-2 gap-3"><Button type="button" variant="secondary" onClick={()=>setOpen(false)}>Cancel</Button><Button type="submit" loading={saving}>Save</Button></div></form></Dialog>
        <Dialog open={Boolean(deleting)} title="Delete category?" description="This is only allowed when no active products use it." confirmLabel="Delete" confirmVariant="danger" busy={saving} onConfirm={()=>void remove()} onClose={()=>setDeleting(null)} />
    </div>;
}
