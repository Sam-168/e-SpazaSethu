import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowDown, ArrowUp, CheckCircle2, ClipboardCheck, Minus, RefreshCw } from 'lucide-react';
import api from '../services/api';
import type { Stocktake } from '../types';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { getApiErrorMessage } from '../utils/api-error';
import { formatDateTime } from '../utils/format';

export default function StocktakeReviewPage(){
    const {id}=useParams<{id:string}>(); const navigate=useNavigate();
    const [stocktake,setStocktake]=useState<Stocktake|null>(null); const [loading,setLoading]=useState(true); const [busy,setBusy]=useState(false); const [confirmApply,setConfirmApply]=useState(false); const [confirmCancel,setConfirmCancel]=useState(false); const [error,setError]=useState<string|null>(null);
    const load=useCallback(async()=>{setLoading(true);setError(null);try{const {data}=await api.get<Stocktake>(`/stocktakes/${id}`);setStocktake(data);}catch(caught){setError(getApiErrorMessage(caught,'The stocktake could not be loaded.'));}finally{setLoading(false);}},[id]);
    useEffect(()=>{
        // eslint-disable-next-line react-hooks/set-state-in-effect -- entering the route loads the selected stocktake
        void load();
    },[load]);
    const counted=useMemo(()=>stocktake?.items.filter(item=>item.countedQuantity!==null)||[],[stocktake]);
    const differences=useMemo(()=>counted.filter(item=>item.discrepancy!==0),[counted]);
    const unitsDifference=differences.reduce((sum,item)=>sum+(item.discrepancy||0),0); const uncounted=(stocktake?.items.length||0)-counted.length;
    const apply=async()=>{if(!stocktake)return;setBusy(true);setError(null);try{const {data}=await api.post<Stocktake>(`/stocktakes/${stocktake.stocktakeId}/apply`);setStocktake(data);setConfirmApply(false);}catch(caught){setConfirmApply(false);setError(getApiErrorMessage(caught,'The stock corrections could not be applied.'));}finally{setBusy(false);}};
    const cancel=async()=>{if(!stocktake)return;setBusy(true);setError(null);try{await api.post(`/stocktakes/${stocktake.stocktakeId}/cancel`);navigate('/stocktake');}catch(caught){setConfirmCancel(false);setError(getApiErrorMessage(caught,'The stocktake could not be cancelled.'));}finally{setBusy(false);}};
    if(loading)return <div className="app-page"><LoadingState label="Loading stocktake review" /></div>;
    if(!stocktake)return <div className="app-page"><PageHeader title="Stocktake" onBack={()=>navigate('/stocktake')} />{error&&<div role="alert" className="border-y border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700"><p>{error}</p><Button variant="secondary" className="mt-3" leadingIcon={<RefreshCw className="h-4 w-4" />} onClick={()=>void load()}>Try again</Button></div>}</div>;
    const inProgress=stocktake.status==='IN_PROGRESS';
    return <div className="app-page"><PageHeader title={inProgress?'Review stocktake':'Stocktake result'} description={`Started ${formatDateTime(stocktake.startedAt)}`} onBack={()=>navigate('/stocktake')} action={<StatusBadge tone={stocktake.status==='COMPLETED'?'success':stocktake.status==='CANCELLED'?'danger':'warning'}>{stocktake.status==='IN_PROGRESS'?'In progress':stocktake.status==='COMPLETED'?'Completed':'Cancelled'}</StatusBadge>} />
        {error&&<p role="alert" className="mb-4 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
        {stocktake.status==='COMPLETED'&&<div role="status" className="mb-5 flex items-start gap-3 border-l-4 border-brand-700 bg-brand-50 px-4 py-4 text-brand-950"><CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0" /><div><p className="font-semibold">Stocktake applied</p><p className="mt-1 text-sm">Product quantities and the stock movement audit trail have been updated.</p></div></div>}
        {inProgress&&uncounted>0&&<div className="mb-5 border-l-4 border-warning-500 bg-warning-50 px-4 py-4 text-sm text-warning-700"><strong>{uncounted} products still need a count.</strong> Return to counting before applying corrections.</div>}
        <section className="mb-6 grid grid-cols-3 divide-x divide-line border-y border-line bg-white text-center"><div className="px-2 py-4"><p className="text-2xl font-semibold text-ink">{counted.length}</p><p className="mt-1 text-xs text-muted">Counted</p></div><div className="px-2 py-4"><p className="text-2xl font-semibold text-ink">{differences.length}</p><p className="mt-1 text-xs text-muted">Differences</p></div><div className="px-2 py-4"><p className={`text-2xl font-semibold ${unitsDifference<0?'text-red-700':'text-brand-800'}`}>{unitsDifference>0?'+':''}{unitsDifference}</p><p className="mt-1 text-xs text-muted">Net units</p></div></section>
        <section><h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">Differences to apply</h2>{differences.length===0?<EmptyState icon={<ClipboardCheck className="h-8 w-8" />} title="Everything matches" description={counted.length===stocktake.items.length?'Physical counts match the system quantities.':'No differences have been recorded in the counted products.'} />:<ul className="divide-y divide-line border-y border-line bg-white">{differences.map(item=>{const change=item.discrepancy||0;return <li key={item.stocktakeItemId} className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 px-4 py-4"><div className="min-w-0"><p className="truncate font-semibold text-ink">{item.productName}</p><p className="mt-1 text-xs text-muted">System {item.systemQuantity} → counted {item.countedQuantity}</p></div><p className={`flex items-center gap-1 text-lg font-semibold ${change<0?'text-red-700':'text-brand-800'}`}>{change>0?<ArrowUp className="h-4 w-4" />:change<0?<ArrowDown className="h-4 w-4" />:<Minus className="h-4 w-4" />}{change>0?'+':''}{change}</p></li>;})}</ul>}</section>
        {inProgress&&<div className="mt-6 flex flex-col-reverse gap-3 border-t border-line pt-5 sm:flex-row sm:justify-between"><Button variant="danger" onClick={()=>setConfirmCancel(true)}>Cancel stocktake</Button><div className="grid grid-cols-2 gap-3"><Button variant="secondary" onClick={()=>navigate(`/stocktake/${stocktake.stocktakeId}/count`)}>Back to counts</Button><Button disabled={uncounted>0} onClick={()=>setConfirmApply(true)}>Apply corrections</Button></div></div>}
        <Dialog open={confirmApply} title="Apply these stock corrections?" description={`${differences.length} products will be corrected. This completes the stocktake and cannot be undone.`} confirmLabel="Apply and complete" busy={busy} onConfirm={()=>void apply()} onClose={()=>setConfirmApply(false)} />
        <Dialog open={confirmCancel} title="Cancel this stocktake?" description="Saved counts will remain in history, but no product quantities will change." confirmLabel="Cancel stocktake" confirmVariant="danger" busy={busy} onConfirm={()=>void cancel()} onClose={()=>setConfirmCancel(false)} />
    </div>;
}
