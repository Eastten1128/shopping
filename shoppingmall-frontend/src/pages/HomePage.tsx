import { useAuth } from '../auth/useAuth';
import { useNavigate } from 'react-router';

function HomePage() {
  const navigate = useNavigate();
  const {
    user,
    isAuthenticated,
  } = useAuth();

  return (
    <div>
      <h1>쇼핑몰 홈</h1>

      {isAuthenticated && user ? (
        <div>
          <p>
            {user.loginId}님 로그인 중입니다.
          </p>

          <p>
            권한: {user.role}
          </p>
          <button type="button" onClick={() => navigate('/orders')}>내 주문 내역</button>
        </div>
      ) : (
        <p>
          로그인하지 않은 상태입니다.
        </p>
      )}
    </div>
  );
}

export default HomePage;
