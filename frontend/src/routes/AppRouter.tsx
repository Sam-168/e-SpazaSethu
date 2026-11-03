import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import AdminRoute from './AdminRoute';
import BottomNav from '../components/layout/BottomNav';

import LoginPage from '../pages/LoginPage';
import DashboardPage from '../pages/DashboardPage';
import SalesHistoryPage from '../pages/SalesHistoryPage';
import SaleDetailPage from '../pages/SaleDetailPage';
import ReportsPage from '../pages/ReportsPage';

const AppRouter = () => (
    <BrowserRouter>
        <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/sales" element={<SalesHistoryPage />} />
                <Route path="/sales/:id" element={<SaleDetailPage />} />

                {/* placeholders — build out as their own issues land */}
                <Route path="/pos" element={<div>POS</div>} />
                <Route path="/products" element={<div>Products</div>} />
                <Route path="/products/new" element={<div>Add Product</div>} />
                <Route path="/products/:id/edit" element={<div>Edit Product</div>} />
                <Route path="/reports" element={<ReportsPage />} />
                <Route path="/stocktake" element={<div>Stocktake</div>} />
                <Route path="/settings" element={<div>Settings</div>} />

                <Route element={<AdminRoute />}>
                    <Route path="/movements" element={<div>Stock Movements</div>} />
                    <Route path="/settings/users" element={<div>User Management</div>} />
                </Route>
            </Route>
        </Routes>
        <BottomNav />
    </BrowserRouter>
);

export default AppRouter;
