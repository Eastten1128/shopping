import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './useAuth';

function ProtectedRoute() {
    const { isAuthenticated, isInitializing } = useAuth();
    const location = useLocation();
    if (isInitializing) return <p>인증 상태를 확인하는 중...</p>;
    return isAuthenticated
        ? <Outlet />
        : <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
}

export default ProtectedRoute;
