import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import AdminRoute from './AdminRoute';
import AppShell from '../components/layout/AppShell';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const SetupOwnerPage = lazy(() => import('../pages/SetupOwnerPage'));
const DashboardPage = lazy(() => import('../pages/DashboardPage'));
const SalesHistoryPage = lazy(() => import('../pages/SalesHistoryPage'));
const SaleDetailPage = lazy(() => import('../pages/SaleDetailPage'));
const UserManagementPage = lazy(() => import('../pages/UserManagementPage'));
const ProductsPage = lazy(() => import('../pages/ProductsPage'));
const ProductFormPage = lazy(() => import('../pages/ProductFormPage'));
const CategoryManagementPage = lazy(() => import('../pages/CategoryManagementPage'));
const PosPage = lazy(() => import('../pages/PosPage'));
const StockMovementsPage = lazy(() => import('../pages/StockMovementsPage'));
const StocktakePage = lazy(() => import('../pages/StocktakePage'));
const StocktakeCountPage = lazy(() => import('../pages/StocktakeCountPage'));
const StocktakeReviewPage = lazy(() => import('../pages/StocktakeReviewPage'));
const ReportsPage = lazy(() => import('../pages/ReportsPage'));
const SettingsPage = lazy(() => import('../pages/SettingsPage'));

const AppRouter = () => (
    <BrowserRouter>
        <Suspense fallback={<div role="status" className="grid min-h-screen place-items-center bg-white text-sm text-muted">Opening page...</div>}>
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/setup-owner" element={<SetupOwnerPage />} />

            <Route element={<ProtectedRoute />}>
                <Route element={<AppShell />}>
                    <Route path="/" element={<DashboardPage />} />
                    <Route path="/sales" element={<SalesHistoryPage />} />
                    <Route path="/sales/:id" element={<SaleDetailPage />} />
                    <Route path="/pos" element={<PosPage />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/reports" element={<ReportsPage />} />
                    <Route path="/settings" element={<SettingsPage />} />
                    <Route element={<AdminRoute />}>
                        <Route path="/products/new" element={<ProductFormPage />} />
                        <Route path="/products/:id/edit" element={<ProductFormPage />} />
                        <Route path="/products/categories" element={<CategoryManagementPage />} />
                        <Route path="/stocktake" element={<StocktakePage />} />
                        <Route path="/stocktake/:id/count" element={<StocktakeCountPage />} />
                        <Route path="/stocktake/:id/review" element={<StocktakeReviewPage />} />
                        <Route path="/movements" element={<StockMovementsPage />} />
                        <Route path="/settings/users" element={<UserManagementPage />} />
                    </Route>
                </Route>
            </Route>
        </Routes>
        </Suspense>
    </BrowserRouter>
);

export default AppRouter;
