import { useCallback, useEffect, useState } from 'react';
import { Check, Copy, Printer, RefreshCw, RotateCcw, TriangleAlert } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';
import type { Sale } from '../types';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { formatCurrency, formatDateTime } from '../utils/format';
import { getApiErrorMessage } from '../utils/api-error';

const paymentLabels: Record<string,string>={CASH:'Cash',CARD:'Card',MOBILE_PAYMENT:'Mobile payment'};

export default function SaleDetailPage(){
    const {id}=useParams<{id:string}>(); const navigate=useNavigate(); const {currentUser}=useAuth(); const admin=currentUser?.role==='ADMIN';
    const [sale,setSale]=useState<Sale|null>(null); const [loading,setLoading]=useState(true); const [error,setError]=useState<string|null>(null); const [confirm,setConfirm]=useState(false); const [cancelReason,setCancelReason]=useState(''); const [cancelling,setCancelling]=useState(false); const [copied,setCopied]=useState(false);
    const load=useCallback(async()=>{if(!id)return;setLoading(true);setError(null);try{const {data}=await api.get<Sale>(`/sales/${id}`);setSale(data);}catch(caught){setError(getApiErrorMessage(caught,'This receipt could not be loaded.'));}finally{setLoading(false);}},[id]);
    useEffect(()=>{
        // eslint-disable-next-line react-hooks/set-state-in-effect -- receipt id changes intentionally trigger a remote refresh
        void load();
    },[load]);
    const cancel=async()=>{if(!id||!cancelReason.trim()){setError('Enter a cancellation reason before continuing.');return;}setCancelling(true);setError(null);try{const {data}=await api.patch<Sale>(`/sales/${id}/cancel`,{reason:cancelReason.trim()});setSale(data);setConfirm(false);setCancelReason('');}catch(caught){setError(getApiErrorMessage(caught,'The sale could not be cancelled.'));setConfirm(false);}finally{setCancelling(false);}};
    const copyId=async()=>{if(!sale)return;await navigator.clipboard.writeText(sale.saleId);setCopied(true);window.setTimeout(()=>setCopied(false),1500);};
    if(loading)return <div className="app-page max-w-3xl"><LoadingState label="Loading receipt" /></div>;
    if(error&&!sale)return <div className="app-page flex min-h-[70vh] items-center justify-center"><div className="w-full max-w-md border-y border-line py-10 text-center"><TriangleAlert className="mx-auto h-8 w-8 text-red-600" /><h1 className="mt-3 text-lg font-semibold">Receipt unavailable</h1><p className="mt-2 text-sm text-muted">{error}</p><Button className="mt-5" leadingIcon={<RefreshCw className="h-4 w-4" />} onClick={()=>void load()}>Try again</Button></div></div>;
    if(!sale)return null; const cancelled=sale.status==='CANCELLED'; const units=sale.items.reduce((sum,item)=>sum+item.quantity,0);
    return <div className="app-page max-w-3xl"><div className="print:hidden"><PageHeader title={`Receipt #${sale.saleId.slice(0,8).toUpperCase()}`} description={formatDateTime(sale.saleDateTime)} onBack={()=>navigate('/sales')} action={<Button variant="secondary" leadingIcon={<Printer className="h-4 w-4" />} onClick={()=>window.print()}><span className="hidden sm:inline">Print</span></Button>} /></div>
        <header className="hidden print:block"><h1 className="text-2xl font-semibold">{localStorage.getItem('storeName')||'e-SpazaSethu'} receipt</h1><p className="mt-1 text-sm">#{sale.saleId}</p><p className="text-sm">{formatDateTime(sale.saleDateTime)}</p></header>
        {error&&<p role="alert" className="mb-4 bg-red-50 px-4 py-3 text-sm text-red-700 print:hidden">{error}</p>}
        {cancelled&&<div className="mb-5 flex items-start gap-3 border-l-4 border-red-600 bg-red-50 px-4 py-3 text-sm text-red-800"><RotateCcw className="mt-0.5 h-5 w-5 shrink-0" /><div><p className="font-semibold">Sale cancelled</p><p className="mt-0.5">The sold quantities were returned to stock and recorded in the audit trail.</p>{sale.notes&&<p className="mt-2 font-medium">{sale.notes}</p>}</div></div>}
        <section className="mb-7 grid gap-0 border-y border-line bg-white sm:grid-cols-2"><Meta label="Cashier" value={sale.cashierName} /><Meta label="Payment" value={paymentLabels[sale.paymentMethod]||sale.paymentMethod} /><Meta label="Items" value={`${units} ${units===1?'unit':'units'}`} /><Meta label="Status" value={<StatusBadge tone={cancelled?'danger':'success'}>{cancelled?'Cancelled':'Completed'}</StatusBadge>} /></section>
        <section aria-labelledby="items-heading"><div className="mb-3 flex items-center justify-between"><h2 id="items-heading" className="text-sm font-semibold uppercase tracking-wide text-muted">Items sold</h2><button onClick={()=>void copyId()} className="inline-flex min-h-11 items-center gap-2 text-xs font-semibold text-brand-700 print:hidden">{copied?<Check className="h-4 w-4" />:<Copy className="h-4 w-4" />}{copied?'Copied':'Copy receipt ID'}</button></div>
            <div className="border-y border-line bg-white"><div className="hidden grid-cols-[1fr_5rem_8rem_8rem] gap-3 border-b border-line px-4 py-3 text-xs font-semibold uppercase tracking-wide text-muted sm:grid"><span>Product</span><span className="text-center">Qty</span><span className="text-right">Price</span><span className="text-right">Subtotal</span></div><ul className="divide-y divide-line">{sale.items.map((item,index)=><li key={`${item.productId}-${index}`} className="grid grid-cols-[1fr_auto] gap-2 px-4 py-4 sm:grid-cols-[1fr_5rem_8rem_8rem] sm:items-center sm:gap-3"><div><p className="font-semibold text-ink">{item.productName||'Product unavailable'}</p><p className="mt-0.5 text-xs text-muted sm:hidden">{item.quantity} × {formatCurrency(item.unitPrice)}</p></div><p className="hidden text-center text-sm text-muted sm:block">{item.quantity}</p><p className="hidden text-right text-sm text-muted sm:block">{formatCurrency(item.unitPrice)}</p><p className="text-right font-semibold text-ink">{formatCurrency(item.subtotal)}</p></li>)}</ul></div>
        </section>
        <div className="mt-5 flex items-end justify-between border-y border-line py-5"><div><p className="text-sm text-muted">Total paid</p><p className="mt-1 text-xs text-muted">{paymentLabels[sale.paymentMethod]||sale.paymentMethod}</p></div><p className="text-3xl font-semibold tracking-tight text-ink">{formatCurrency(sale.totalAmount)}</p></div>
        {admin&&!cancelled&&<div className="mt-7 border-t border-line pt-5 print:hidden"><h2 className="text-sm font-semibold text-ink">Administrative action</h2><p className="mt-1 text-sm leading-6 text-muted">Only cancel a sale when the transaction should be reversed. All quantities will be returned to stock.</p><Button variant="danger" className="mt-4" onClick={()=>setConfirm(true)}>Cancel sale and restore stock</Button></div>}
        <Dialog open={confirm} title="Cancel this sale?" description="This returns every sold quantity to stock and creates audit movements. This action cannot be repeated." confirmLabel="Cancel sale" confirmVariant="danger" busy={cancelling} onConfirm={()=>void cancel()} onClose={()=>{setConfirm(false);setCancelReason('');}}><label className="block text-sm font-medium text-ink">Reason<span className="text-red-600"> *</span><textarea value={cancelReason} onChange={event=>setCancelReason(event.target.value)} maxLength={500} rows={3} placeholder="For example: customer returned the items" className="mt-1.5 w-full rounded-lg border border-line bg-white p-3 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label></Dialog>
    </div>;
}
function Meta({label,value}:{label:string;value:React.ReactNode}){return <div className="flex min-h-16 items-center justify-between border-b border-line px-4 py-3 last:border-b-0 sm:[&:nth-child(odd)]:border-r sm:[&:nth-child(n+3)]:border-b-0"><span className="text-sm text-muted">{label}</span><span className="text-sm font-semibold text-ink">{value}</span></div>}
