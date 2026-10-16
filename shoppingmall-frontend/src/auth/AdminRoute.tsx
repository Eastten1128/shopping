import { Outlet } from 'react-router';
import { useAuth } from './useAuth';

function AdminRoute() {
    const { user } = useAuth();
    return user?.role === 'ADMIN' ? <Outlet /> : <p role="alert">관리자만 이용할 수 있습니다.</p>;
}

export default AdminRoute;
