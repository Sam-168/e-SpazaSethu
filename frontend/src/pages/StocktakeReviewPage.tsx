import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';
import type { StocktakeDetail, StocktakeItem } from '../types';
import './StocktakeReviewPage.css';

const StocktakeReviewPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [stocktake, setStocktake] = useState<StocktakeDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [applying, setApplying] = useState(false);
    const [error, setError] = useState('');
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [showMatched, setShowMatched] = useState(false);
    const [success, setSuccess] = useState('');

    useEffect(() => {
        let isMounted = true;

        const fetchStocktake = async () => {
            if (!id) {
                if (isMounted) {
                    setError('Stocktake ID is missing.');
                    setLoading(false);
                }
                return;
            }

            try {
                const response = await api.get<StocktakeDetail>(
                    `/stocktakes/${id}`,
                );

                if (isMounted) {
                    setStocktake(response.data);
                    setError('');
                }
            } catch (err: unknown) {
                if (isMounted) {
                    const message =
                        (err as {
                            response?: { data?: { message?: string } };
                        }).response?.data?.message ||
                        'Unable to load the stocktake. Please try again.';

                    setError(message);
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        };

        fetchStocktake();

        return () => {
            isMounted = false;
        };
    }, [id]);

    const items = stocktake?.items ?? [];

    const discrepancies = useMemo(
        () =>
            items.filter(
                (item) =>
                    item.countedQuantity !== null &&
                    item.discrepancy !== null &&
                    item.discrepancy !== 0,
            ),
        [items],
    );

    const matchedProducts = useMemo(
        () =>
            items.filter(
                (item) =>
                    item.countedQuantity !== null &&
                    item.discrepancy === 0,
            ),
        [items],
    );

    const skippedProducts = useMemo(
        () => items.filter((item) => item.countedQuantity === null),
        [items],
    );

    const countedTotal = items.filter(
        (item) => item.countedQuantity !== null,
    ).length;

    const formatDifference = (difference: number) => {
        if (difference > 0) {
            return `+${difference}`;
        }

        return difference.toString();
    };
    const handleApplyAdjustments = async () => {
        if (!id) return;

        try {
            setApplying(true);
            setError('');

            await api.post(`/stocktakes/${id}/apply`);

            setShowConfirmation(false);
            setSuccess(
                'Stocktake completed successfully. Stock quantities have been updated.',
            );

            setTimeout(() => {
                navigate('/stocktake');
            }, 1800);

        } catch (err: unknown) {
            const message =
                (err as {
                    response?: { data?: { message?: string } };
                }).response?.data?.message ||
                'Unable to apply stock adjustments. Please try again.';

            setError(message);
            setShowConfirmation(false);

        } finally {
            setApplying(false);
        }
    };

    const renderProductRow = (item: StocktakeItem) => (
        <tr key={item.stocktakeItemId}>
            <td>
                <span className="review-product-name">
                    {item.productName || 'Unknown product'}
                </span>
                <span className="review-product-id">{item.productId}</span>
            </td>

            <td>{item.systemQuantity}</td>

            <td>{item.countedQuantity ?? 'Skipped'}</td>

            <td>
                {item.discrepancy !== null ? (
                    <span
                        className={`review-difference ${
                            item.discrepancy > 0
                                ? 'difference-positive'
                                : item.discrepancy < 0
                                    ? 'difference-negative'
                                    : 'difference-neutral'
                        }`}
                    >
                        {formatDifference(item.discrepancy)}
                    </span>
                ) : (
                    <span className="review-skipped-label">Skipped</span>
                )}
            </td>
        </tr>
    );

    if (loading) {
        return (
            <div className="stocktake-review-page">
                <div className="review-loading">Loading stocktake review...</div>
            </div>
        );
    }

    if (error && !stocktake) {
        return (
            <div className="stocktake-review-page">
                <div className="review-error">{error}</div>
                <button
                    className="review-secondary-button"
                    onClick={() => navigate('/stocktake')}
                >
                    Back to Stocktakes
                </button>
            </div>
        );
    }

    if (!stocktake) return null;

    const isCompleted = stocktake.status === 'COMPLETED';

    return (
        <div className="stocktake-review-page">
            <div className="review-header">
                <div>
                    <button
                        className="review-back-link"
                        onClick={() => navigate(`/stocktake/${id}/count`)}
                        disabled={isCompleted}
                    >
                        ← Go Back to Count
                    </button>

                    <h1>Review Stocktake</h1>
                    <p>
                        Review the differences between your recorded stock
                        and the physical count before applying corrections.
                    </p>
                </div>

                <span className={`review-status ${stocktake.status.toLowerCase()}`}>
                    {stocktake.status.replace('_', ' ')}
                </span>
            </div>

            {error && <div className="review-error">{error}</div>}

            {success && <div className="review-success">{success}</div>}

            {isCompleted && (
                <div className="review-success">
                    This stocktake has already been completed.
                </div>
            )}

            <div className="review-summary">
                <div className="review-summary-card">
                    <span className="review-summary-label">Products Counted</span>
                    <strong>{countedTotal}</strong>
                </div>

                <div className="review-summary-card">
                    <span className="review-summary-label">Discrepancies</span>
                    <strong>{discrepancies.length}</strong>
                </div>

                <div className="review-summary-card">
                    <span className="review-summary-label">Products Skipped</span>
                    <strong>{skippedProducts.length}</strong>
                </div>
            </div>

            <section className="review-section">
                <div className="review-section-heading">
                    <div>
                        <h2>Stock Discrepancies</h2>
                        <p>
                            These products have a difference between the
                            recorded quantity and the physical count.
                        </p>
                    </div>

                    <span className="review-count">
                        {discrepancies.length} found
                    </span>
                </div>

                {discrepancies.length === 0 ? (
                    <div className="review-empty">
                        No stock discrepancies were found.
                    </div>
                ) : (
                    <div className="review-table-wrapper">
                        <table className="review-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>System Quantity</th>
                                <th>Physical Quantity</th>
                                <th>Difference</th>
                            </tr>
                            </thead>
                            <tbody>
                            {discrepancies.map(renderProductRow)}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <section className="review-section">
                <button
                    className="review-collapse-button"
                    onClick={() => setShowMatched(!showMatched)}
                    aria-expanded={showMatched}
                >
                    <span>
                        Matched Products ({matchedProducts.length})
                    </span>
                    <span>{showMatched ? '−' : '+'}</span>
                </button>

                {showMatched && (
                    matchedProducts.length === 0 ? (
                        <div className="review-empty">
                            No matched products.
                        </div>
                    ) : (
                        <div className="review-table-wrapper">
                            <table className="review-table">
                                <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>System Quantity</th>
                                    <th>Physical Quantity</th>
                                    <th>Difference</th>
                                </tr>
                                </thead>
                                <tbody>
                                {matchedProducts.map(renderProductRow)}
                                </tbody>
                            </table>
                        </div>
                    )
                )}
            </section>

            {skippedProducts.length > 0 && (
                <section className="review-section">
                    <div className="review-section-heading">
                        <div>
                            <h2>Skipped Products</h2>
                            <p>
                                These products were not counted and will not
                                receive stock adjustments.
                            </p>
                        </div>
                        <span className="review-count">
                            {skippedProducts.length} skipped
                        </span>
                    </div>

                    <div className="review-table-wrapper">
                        <table className="review-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>System Quantity</th>
                                <th>Physical Quantity</th>
                                <th>Difference</th>
                            </tr>
                            </thead>
                            <tbody>
                            {skippedProducts.map(renderProductRow)}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            <div className="review-footer">
                <button
                    className="review-secondary-button"
                    onClick={() => navigate('/stocktake')}
                >
                    Back to Stocktakes
                </button>

                <button
                    className="review-primary-button"
                    onClick={() => setShowConfirmation(true)}
                    disabled={applying || isCompleted}
                >
                    Apply Corrections
                </button>
            </div>

            {showConfirmation && (
                <div className="review-modal-overlay">
                    <div
                        className="review-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="review-modal-title"
                    >
                        <h2 id="review-modal-title">Apply Stock Corrections?</h2>

                        <p>
                            You are about to apply {discrepancies.length}{' '}
                            stock correction(s). This will update your product
                            quantities to match the physical counts and
                            complete this stocktake.
                        </p>

                        <p className="review-modal-warning">
                            This action cannot be undone.
                        </p>

                        <div className="review-modal-actions">
                            <button
                                className="review-secondary-button"
                                onClick={() => setShowConfirmation(false)}
                                disabled={applying}
                            >
                                Cancel
                            </button>

                            <button
                                className="review-primary-button"
                                onClick={handleApplyAdjustments}
                                disabled={applying}
                            >
                                {applying ? 'Applying...' : 'Confirm & Apply'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default StocktakeReviewPage;