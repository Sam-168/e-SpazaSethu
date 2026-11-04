
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import './StocktakePage.css';

type StocktakeStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

interface StocktakeItem {
    stocktakeItemId: string;
    productId: string;
    productName: string | null;
    systemQuantity: number;
    countedQuantity: number | null;
    discrepancy: number | null;
    adjusted: boolean;
}

interface Stocktake {
    stocktakeId: string;
    conductedBy: string;
    startedAt: string;
    completedAt: string | null;
    status: StocktakeStatus;
    notes: string | null;
    items: StocktakeItem[];
}

export default function StocktakePage() {
    const navigate = useNavigate();

    const [stocktakes, setStocktakes] = useState<Stocktake[]>([]);
    const [loading, setLoading] = useState(true);
    const [starting, setStarting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const unfinishedStocktake =
        stocktakes.find((stocktake) => stocktake.status === 'IN_PROGRESS') ?? null;

    useEffect(() => {
        async function loadStocktakes() {
            try {
                setLoading(true);
                setError(null);

                const response = await api.get<Stocktake[]>('/stocktakes');
                setStocktakes(response.data);
            } catch {
                setError('Unable to load previous stocktakes.');
            } finally {
                setLoading(false);
            }
        }

        void loadStocktakes();
    }, []);

    async function startNewStocktake() {
        try {
            setStarting(true);
            setError(null);

            const response = await api.post<Stocktake>('/stocktakes');
            const nextId = response.data.stocktakeId;

            if (!nextId) {
                setError('Stocktake was created without an identifier.');
                return;
            }

            navigate(`/stocktake/${nextId}/count`);
        } catch {
            setError('Unable to start a new stocktake. Please try again.');
        } finally {
            setStarting(false);
        }
    }
    function handleStocktakeClick(stocktake: Stocktake) {
        const id = stocktake.stocktakeId;

        if (stocktake.status === 'IN_PROGRESS') {
            navigate(`/stocktake/${id}/count`);
        } else if (stocktake.status === 'COMPLETED') {
            navigate(`/stocktake/${id}/review`);
        }
    }

    function resumeStocktake() {
        if (!unfinishedStocktake) return;

        navigate(`/stocktake/${unfinishedStocktake.stocktakeId}/count`);
    }

    function getDiscrepancyCount(stocktake: Stocktake): number {
        return stocktake.items.filter(
            (item) => item.discrepancy !== null && item.discrepancy !== 0
        ).length;
    }

    function formatDate(date: string) {
        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return 'Date unavailable';
        }

        return parsedDate.toLocaleDateString('en-ZA', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
        });
    }

    function getStatusLabel(status: StocktakeStatus) {
        switch (status) {
            case 'IN_PROGRESS':
                return 'In Progress';
            case 'COMPLETED':
                return 'Completed';
            case 'CANCELLED':
                return 'Cancelled';
            default:
                return status;
        }
    }

    return (
        <main className="stocktake-page">
            <header className="page-header">
                <div>
                    <p className="eyebrow">Inventory</p>
                    <h1>Stocktake</h1>

                    <p className="subtext">
                        A stocktake lets you count your physical stock and correct any
                        differences between your records and what's actually on the shelf.
                    </p>
                </div>

                <button
                    type="button"
                    className="primary-button"
                    onClick={() => void startNewStocktake()}
                    disabled={loading || starting || Boolean(unfinishedStocktake)}
                >
                    {starting ? 'Starting...' : 'Start New Stocktake'}
                </button>
            </header>

            {error && (
                <div className="error-banner" role="alert">
                    {error}
                </div>
            )}

            {unfinishedStocktake && (
                <section className="warning-banner" role="status">
                    <div>
                        <strong>You have an unfinished stocktake.</strong>
                        <p>Resume it to continue counting your stock.</p>
                    </div>

                    <button
                        type="button"
                        className="secondary-button"
                        onClick={resumeStocktake}
                    >
                        Resume
                    </button>
                </section>
            )}

            <section className="stocktake-panel">
                <h2>Previous Stocktakes</h2>

                {loading ? (
                    <p className="empty-state">Loading stocktakes...</p>
                ) : stocktakes.length === 0 ? (
                    <p className="empty-state">
                        No stocktakes have been started yet.
                    </p>
                ) : (
                    <div className="stocktake-list">
                        {stocktakes.map((stocktake) => (
                            <button
                                key={stocktake.stocktakeId}
                                type="button"
                                className="stocktake-row"
                                onClick={() => handleStocktakeClick(stocktake)}
                                disabled={stocktake.status === 'CANCELLED'}
                            >
                                <span className="stocktake-date">
                                    {formatDate(stocktake.startedAt)}
                                </span>

                                <span
                                    className={`status-badge status-${stocktake.status.toLowerCase()}`}
                                >
                                    {getStatusLabel(stocktake.status)}
                                </span>

                                <span className="discrepancy-count">
                                    {getDiscrepancyCount(stocktake)} discrepancies
                                </span>
                            </button>
                        ))}
                    </div>
                )}
            </section>
        </main>
    );
}
