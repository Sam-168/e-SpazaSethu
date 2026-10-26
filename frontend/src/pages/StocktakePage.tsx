import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ClipboardCheck, Play, RefreshCw } from 'lucide-react';
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

const statusLabel={IN_PROGRESS:'In progress',COMPLETED:'Completed',CANCELLED:'Cancelled'} as const;

export default function StocktakePage(){
    const navigate=useNavigate();
    const [stocktakes,setStocktakes]=useState<Stocktake[]>([]); const [loading,setLoading]=useState(true); const [error,setError]=useState<string|null>(null); const [startOpen,setStartOpen]=useState(false); const [starting,setStarting]=useState(false);
    const load=useCallback(async()=>{setLoading(true);setError(null);try{const {data}=await api.get<Stocktake[]>('/stocktakes');setStocktakes(data);}catch(caught){setError(getApiErrorMessage(caught,'Stocktakes could not be loaded.'));}finally{setLoading(false);}},[]);
    useEffect(()=>{
        // eslint-disable-next-line react-hooks/set-state-in-effect -- entering the route loads the remote stocktake history
        void load();
    },[load]);
    const active=stocktakes.find(stocktake=>stocktake.status==='IN_PROGRESS');
    const start=async()=>{setStarting(true);setError(null);try{const {data}=await api.post<Stocktake>('/stocktakes');setStartOpen(false);navigate(`/stocktake/${data.stocktakeId}/count`);}catch(caught){setStartOpen(false);setError(getApiErrorMessage(caught,'The stocktake could not be started.'));void load();}finally{setStarting(false);}};
    return <div className="app-page"><PageHeader title="Stocktake" description="Count the shelves, review differences, then correct stock." action={<Button leadingIcon={<Play className="h-5 w-5" />} disabled={Boolean(active)} onClick={()=>setStartOpen(true)}><span className="hidden sm:inline">Start stocktake</span><span className="sm:hidden">Start</span></Button>} />
        {error&&<div role="alert" className="mb-5 border-y border-red-200 bg-red-50 px-4 py-4 text-sm text-red-700"><p>{error}</p><Button variant="secondary" className="mt-3" leadingIcon={<RefreshCw className="h-4 w-4" />} onClick={()=>void load()}>Try again</Button></div>}
        {active&&<section className="mb-6 border-l-4 border-brand-700 bg-brand-50 px-4 py-4"><div className="flex flex-col gap-4 sm:flex-row sm:items-center"><div className="min-w-0 flex-1"><p className="font-semibold text-brand-950">A stocktake is in progress</p><p className="mt-1 text-sm text-brand-900">{active.items.filter(item=>item.countedQuantity!==null).length} of {active.items.length} products counted · started {formatDateTime(active.startedAt)}</p></div><Button onClick={()=>navigate(`/stocktake/${active.stocktakeId}/count`)}>Continue counting</Button></div></section>}
        {loading&&<LoadingState label="Loading stocktakes" />}
        {!loading&&!error&&stocktakes.length===0&&<EmptyState icon={<ClipboardCheck className="h-8 w-8" />} title="No stocktakes yet" description="Start with a physical count of every active product." action={<Button onClick={()=>setStartOpen(true)}>Start first stocktake</Button>} />}
        {!loading&&stocktakes.length>0&&<section><h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">History</h2><ul className="divide-y divide-line border-y border-line bg-white">{stocktakes.map(stocktake=>{const counted=stocktake.items.filter(item=>item.countedQuantity!==null).length;const differences=stocktake.items.filter(item=>item.discrepancy!==null&&item.discrepancy!==0).length;return <li key={stocktake.stocktakeId}><button type="button" onClick={()=>navigate(stocktake.status==='IN_PROGRESS'?`/stocktake/${stocktake.stocktakeId}/count`:`/stocktake/${stocktake.stocktakeId}/review`)} className="grid min-h-20 w-full grid-cols-[1fr_auto] items-center gap-4 px-4 py-3 text-left hover:bg-brand-50"><span><span className="flex items-center gap-2"><span className="font-semibold text-ink">{formatDateTime(stocktake.startedAt)}</span><StatusBadge tone={stocktake.status==='COMPLETED'?'success':stocktake.status==='CANCELLED'?'danger':'warning'}>{statusLabel[stocktake.status]}</StatusBadge></span><span className="mt-1 block text-xs text-muted">{stocktake.status==='IN_PROGRESS'?`${counted} of ${stocktake.items.length} counted`:`${stocktake.items.length} products · ${differences} ${differences===1?'difference':'differences'}`}</span></span>{stocktake.status==='COMPLETED'?<CheckCircle2 className="h-5 w-5 text-brand-700" />:<ClipboardCheck className="h-5 w-5 text-muted" />}</button></li>;})}</ul></section>}
        <Dialog open={startOpen} title="Start a new stocktake?" description="The system will snapshot every active product. Pause sales while counting so quantities do not change before you apply the result." confirmLabel="Start counting" busy={starting} onConfirm={()=>void start()} onClose={()=>setStartOpen(false)} />
    </div>;
}
