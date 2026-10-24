import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import AdminRoute from './AdminRoute';
import AppShell from '../components/layout/AppShell';
import PlaceholderPage from '../components/layout/PlaceholderPage';
import { BarChart3, ClipboardCheck, Package, Settings, ShoppingBasket, SlidersHorizontal } from 'lucide-react';

import LoginPage from '../pages/LoginPage';
import SetupOwnerPage from '../pages/SetupOwnerPage';
import DashboardPage from '../pages/DashboardPage';
import SalesHistoryPage from '../pages/SalesHistoryPage';
import SaleDetailPage from '../pages/SaleDetailPage';
import UserManagementPage from '../pages/UserManagementPage';

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
                    <Route path="/pos" element={<PlaceholderPage title="New sale" description="Find products and complete a sale." feature="F05" icon={ShoppingBasket} />} />
                    <Route path="/products" element={<PlaceholderPage title="Products" description="Manage the shop catalogue and stock levels." feature="F04" icon={Package} />} />
                    <Route path="/products/new" element={<PlaceholderPage title="Add product" description="Add a product to the shop catalogue." feature="F04" icon={Package} />} />
                    <Route path="/products/:id/edit" element={<PlaceholderPage title="Edit product" description="Update product details and availability." feature="F04" icon={Package} />} />
                    <Route path="/reports" element={<PlaceholderPage title="Reports" description="Understand sales, profit and product performance." feature="F10" icon={BarChart3} />} />
                    <Route path="/settings" element={<PlaceholderPage title="Settings" description="Manage shop and account preferences." feature="F12" icon={Settings} />} />
                    <Route element={<AdminRoute />}>
                        <Route path="/stocktake" element={<PlaceholderPage title="Stocktake" description="Count physical stock and review discrepancies." feature="F09" icon={ClipboardCheck} />} />
                        <Route path="/movements" element={<PlaceholderPage title="Stock movements" description="Review and adjust the shop's stock audit trail." feature="F08" icon={SlidersHorizontal} />} />
                        <Route path="/settings/users" element={<UserManagementPage />} />
                    </Route>
                </Route>
            </Route>
        </Routes>
    </BrowserRouter>
);

export default AppRouter;
