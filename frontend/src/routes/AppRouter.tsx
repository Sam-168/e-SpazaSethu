import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import AdminRoute from './AdminRoute';
import AppShell from '../components/layout/AppShell';
import PlaceholderPage from '../components/layout/PlaceholderPage';
import { BarChart3, ClipboardCheck, Settings } from 'lucide-react';

import LoginPage from '../pages/LoginPage';
import SetupOwnerPage from '../pages/SetupOwnerPage';
import DashboardPage from '../pages/DashboardPage';
import SalesHistoryPage from '../pages/SalesHistoryPage';
import SaleDetailPage from '../pages/SaleDetailPage';
import UserManagementPage from '../pages/UserManagementPage';
import ProductsPage from '../pages/ProductsPage';
import ProductFormPage from '../pages/ProductFormPage';
import CategoryManagementPage from '../pages/CategoryManagementPage';
import PosPage from '../pages/PosPage';
import StockMovementsPage from '../pages/StockMovementsPage';

const AppRouter = () => (
    <BrowserRouter>
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
                    <Route path="/reports" element={<PlaceholderPage title="Reports" description="Understand sales, profit and product performance." feature="F10" icon={BarChart3} />} />
                    <Route path="/settings" element={<PlaceholderPage title="Settings" description="Manage shop and account preferences." feature="F12" icon={Settings} />} />
                    <Route element={<AdminRoute />}>
                        <Route path="/products/new" element={<ProductFormPage />} />
                        <Route path="/products/:id/edit" element={<ProductFormPage />} />
                        <Route path="/products/categories" element={<CategoryManagementPage />} />
                        <Route path="/stocktake" element={<PlaceholderPage title="Stocktake" description="Count physical stock and review discrepancies." feature="F09" icon={ClipboardCheck} />} />
                        <Route path="/movements" element={<StockMovementsPage />} />
                        <Route path="/settings/users" element={<UserManagementPage />} />
                    </Route>
                </Route>
            </Route>
        </Routes>
    </BrowserRouter>
);

export default AppRouter;
