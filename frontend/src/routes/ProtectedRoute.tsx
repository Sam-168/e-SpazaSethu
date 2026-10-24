import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = () => {
    const { token, currentUser } = useAuth();
    const location = useLocation();

    if (!token || !currentUser) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    return <Outlet />;
};

export default ProtectedRoute;
