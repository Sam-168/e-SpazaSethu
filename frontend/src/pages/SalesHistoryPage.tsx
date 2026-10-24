import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, History, RefreshCw, Search } from 'lucide-react';
import api from '../services/api';
import type { Sale, SalesReportSummary } from '../types';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { formatCurrency, formatDateTime } from '../utils/format';
import { getApiErrorMessage } from '../utils/api-error';

type Preset = 'today' | 'yesterday' | 'last7' | 'custom';
function localDate(date: Date): string { const y=date.getFullYear(); const m=String(date.getMonth()+1).padStart(2,'0'); const d=String(date.getDate()).padStart(2,'0'); return `${y}-${m}-${d}`; }
function rangeFor(preset: Exclude<Preset,'custom'>) { const end=new Date(); const start=new Date(end); if(preset==='yesterday'){start.setDate(start.getDate()-1);end.setDate(end.getDate()-1);} if(preset==='last7')start.setDate(start.getDate()-6); return {from:localDate(start),to:localDate(end)}; }
const paymentLabels: Record<string,string>={CASH:'Cash',CARD:'Card',MOBILE_PAYMENT:'Mobile'};

export default function SalesHistoryPage(){
    const initial=rangeFor('today'); const [preset,setPreset]=useState<Preset>('today'); const [from,setFrom]=useState(initial.from); const [to,setTo]=useState(initial.to);
    const [sales,setSales]=useState<Sale[]>([]); const [summary,setSummary]=useState<SalesReportSummary|null>(null); const [query,setQuery]=useState(''); const [loading,setLoading]=useState(true); const [error,setError]=useState<string|null>(null);
    const applyPreset=(next:Exclude<Preset,'custom'>)=>{const range=rangeFor(next);setPreset(next);setFrom(range.from);setTo(range.to);};
    const load=useCallback(async()=>{if(!from||!to)return;if(from>to){setError('The start date cannot be after the end date.');setLoading(false);return;}setLoading(true);setError(null);try{const [saleResponse,summaryResponse]=await Promise.all([api.get<Sale[]>('/sales',{params:{from,to}}),api.get<SalesReportSummary>('/reports/summary',{params:{from,to}})]);setSales(saleResponse.data);setSummary(summaryResponse.data);}catch(caught){setError(getApiErrorMessage(caught,'Sales could not be loaded for this period.'));}finally{setLoading(false);}},[from,to]);
    useEffect(()=>{
        // eslint-disable-next-line react-hooks/set-state-in-effect -- range changes intentionally trigger a remote data refresh
        void load();
    },[load]);
    const visible=useMemo(()=>{const term=query.trim().toLowerCase();if(!term)return sales;return sales.filter(sale=>sale.saleId.toLowerCase().includes(term)||sale.cashierName.toLowerCase().includes(term)||sale.paymentMethod.toLowerCase().includes(term));},[query,sales]);
    const completed=sales.filter(s=>s.status==='COMPLETED').length; const cancelled=sales.filter(s=>s.status==='CANCELLED').length;
    return <div className="app-page"><PageHeader title="Sales history" description="Review completed and cancelled shop transactions." />
        <div className="mb-5 flex gap-2 overflow-x-auto pb-1">{([['today','Today'],['yesterday','Yesterday'],['last7','Last 7 days']] as const).map(([value,label])=><button key={value} onClick={()=>applyPreset(value)} className={`min-h-11 shrink-0 rounded-full border px-4 text-sm font-semibold ${preset===value?'border-brand-700 bg-brand-700 text-white':'border-line bg-white text-muted hover:border-brand-500'}`}>{label}</button>)}</div>
        <div className="mb-6 grid gap-3 sm:grid-cols-[1fr_auto_1fr]"><label><span className="mb-1.5 block text-sm font-medium text-ink">From</span><input type="date" value={from} max={to} onChange={e=>{setFrom(e.target.value);setPreset('custom');}} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label><span className="hidden self-end pb-3 text-sm text-muted sm:block">to</span><label><span className="mb-1.5 block text-sm font-medium text-ink">To</span><input type="date" value={to} min={from} onChange={e=>{setTo(e.target.value);setPreset('custom');}} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label></div>
        {summary&&!loading&&!error&&<section aria-label="Sales summary" className="mb-7 grid grid-cols-2 border-y border-line sm:grid-cols-4"><Summary label="Sales" value={formatCurrency(summary.totalAmount)} /><Summary label="Profit" value={formatCurrency(summary.totalProfit)} /><Summary label="Completed" value={String(completed)} /><Summary label="Cancelled" value={String(cancelled)} last /></section>}
        <label className="relative mb-4 block"><span className="sr-only">Find a receipt</span><Search className="absolute left-3.5 top-3.5 h-5 w-5 text-muted" /><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Find receipt, cashier or payment" className="min-h-12 w-full rounded-lg border border-line bg-white pl-11 pr-4 outline-none focus:border-brand-700 focus:ring-3 focus:ring-brand-100" /></label>
        {loading&&<LoadingState label="Loading sales" />}{error&&<div role="alert" className="border-y border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700"><p>{error}</p><Button variant="secondary" className="mt-3" leadingIcon={<RefreshCw className="h-4 w-4" />} onClick={()=>void load()}>Try again</Button></div>}
        {!loading&&!error&&visible.length===0&&<EmptyState icon={<History className="h-8 w-8" />} title={query?'No matching receipts':'No sales in this period'} description={query?'Try a receipt number, cashier name or payment method.':'Completed sales will appear here after checkout.'} />}
        {!loading&&!error&&visible.length>0&&<div className="border-y border-line bg-white"><div className="hidden grid-cols-[minmax(0,1.2fr)_0.8fr_0.7fr_0.7fr_auto] gap-4 border-b border-line px-4 py-3 text-xs font-semibold uppercase tracking-wide text-muted md:grid"><span>Date and receipt</span><span>Cashier</span><span>Payment</span><span className="text-right">Total</span><span className="w-24">Status</span></div><ul className="divide-y divide-line">{visible.map(sale=><li key={sale.saleId}><Link to={`/sales/${sale.saleId}`} className="grid gap-2 px-4 py-4 hover:bg-brand-50 md:grid-cols-[minmax(0,1.2fr)_0.8fr_0.7fr_0.7fr_auto] md:items-center md:gap-4"><div className="min-w-0"><p className="font-semibold text-ink">{formatDateTime(sale.saleDateTime)}</p><p className="mt-0.5 truncate text-xs text-muted">#{sale.saleId.slice(0,8).toUpperCase()}</p></div><p className="text-sm text-muted">{sale.cashierName}</p><p className="text-sm text-muted">{paymentLabels[sale.paymentMethod]||sale.paymentMethod}</p><p className="text-base font-semibold text-ink md:text-right">{formatCurrency(sale.totalAmount)}</p><div className="flex items-center justify-between gap-3 md:w-24"><StatusBadge tone={sale.status==='COMPLETED'?'success':'danger'}>{sale.status==='COMPLETED'?'Completed':'Cancelled'}</StatusBadge><ChevronRight className="h-4 w-4 text-gray-400 md:hidden" /></div></Link></li>)}</ul></div>}
    </div>;
}
function Summary({label,value,last=false}:{label:string;value:string;last?:boolean}){return <div className={`px-3 py-5 sm:px-5 ${last?'':'border-r border-line'} [&:nth-child(2)]:border-r-0 sm:[&:nth-child(2)]:border-r`}><p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p><p className="mt-2 text-xl font-semibold text-ink sm:text-2xl">{value}</p></div>}
