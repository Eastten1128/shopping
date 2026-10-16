import { Link, Navigate, Route, Routes, useNavigate } from 'react-router';
import { useState } from 'react';
import { logout } from './api/memberApi';
import ProtectedRoute from './auth/ProtectedRoute';
import AdminRoute from './auth/AdminRoute';
import AdminLayout from './pages/AdminLayout';
import AdminCategoryPage from './pages/AdminCategoryPage';

import { useAuth } from './auth/useAuth';
//import axiosInstance from './api/axiosInstance';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import ProductListPage from './pages/ProductListPage';
import ProductDetailPage from './pages/ProductDetailPage';
import CartPage from './pages/CartPage';
import OrderCreatePage from './pages/OrderCreatePage';
import OrderListPage from './pages/OrderListPage';
import OrderDetailPage from './pages/OrderDetailPage';

// App 컴포넌트는 React의 함수형 컴포넌트로, 쇼핑몰 애플리케이션의 메인 화면을 렌더링한다.
function App() {
  const navigate = useNavigate();
  const { isInitializing, isAuthenticated, user } = useAuth();
  const [logoutError, setLogoutError] = useState('');
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const handleLogout = async () => {
    setLogoutError('');
    setIsLoggingOut(true);
    try {
      await logout();
    } catch {
      setLogoutError('서버 로그아웃에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoggingOut(false);
    }
  };

  if (isInitializing) {
    return (
      <p>
        인증 상태를 확인하는 중...
      </p>
    )
  }
  return (
    <div>
      <nav>
        <Link to="/">Home</Link>
        {" | "}
        <Link to="/products">상품</Link>
        {" | "}
        <button type="button" onClick={() => navigate('/cart')}>장바구니</button>
        {" | "}
        {isAuthenticated ? <>
          <span>{user?.loginId}님</span>{" | "}
          <Link to="/account">내 인증 정보</Link>{" | "}
          {user?.role === 'ADMIN' && <><Link to="/admin/categories">관리자 페이지</Link>{" | "}</>}
          <button onClick={() => void handleLogout()} disabled={isLoggingOut}>로그아웃</button>
        </> : <>
          <Link to="/login">로그인</Link>{" | "}
          <Link to="/signup">회원가입</Link>
        </>}
      </nav>
      {logoutError && <p role="alert">{logoutError} <button onClick={() => void handleLogout()} disabled={isLoggingOut}>로그아웃 재시도</button></p>}

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/account" element={<HomePage />} />
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Navigate to="categories" replace />} />
              <Route path="categories" element={<AdminCategoryPage />} />
            </Route>
          </Route>
          <Route path="/cart" element={<CartPage />} />
          <Route path="/orders/new" element={<OrderCreatePage />} />
          <Route path="/orders" element={<OrderListPage />} />
          <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        </Route>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/products" element={<ProductListPage/>}/>
        <Route path="/products/:productId" element={<ProductDetailPage/>}/>
      </Routes>
    </div>
  )
}

export default App;
