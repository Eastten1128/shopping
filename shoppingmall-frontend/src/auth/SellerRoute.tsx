import { Outlet } from 'react-router';
import { useAuth } from './useAuth';

function SellerRoute() {
    const { user } = useAuth();
    return user?.role === 'SELLER'
        ? <Outlet />
        : <p role="alert">판매자만 이용할 수 있습니다.</p>;
}

export default SellerRoute;
