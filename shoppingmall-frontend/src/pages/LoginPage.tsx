import {useState, type SubmitEvent } from 'react';
import { useLocation, useNavigate } from 'react-router';
import axios from 'axios';

import { login } from '../api/memberApi';
import type { MemberLoginRequest } from '../types/member';
import { useAuth } from '../auth/useAuth';

interface ErrorResponse {
    message: string;
}

function LoginPage() {
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');

    const {setAuth} = useAuth();

    const navigate = useNavigate();
    const location = useLocation();

    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();

        setSuccessMessage('');
        setErrorMessage('');

        const request: MemberLoginRequest = {
            loginId,
            password,
        };

        try {
            //로그인
            const response = await login(request);

            //Access Token은 메모리에, 사용자 정보는 React 상태에 저장한다.
            setAuth(response);

            const from = location.state?.from;
            navigate(typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/', { replace: true });
            //setSuccessMessage(`${response.loginId}님, 로그인 성공! (${response.role})`);
            //setPassword('');
        } catch (error) {
            if(axios.isAxiosError<ErrorResponse>(error)){
                setErrorMessage(
                    error.response?.data.message ?? '로그인에 실패했습니다.'
                );
                return;
            }

            setErrorMessage('알 수 없는 에러 발생.');
        }
    };

    return (
        <div>
      <h1>로그인</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="loginId">
            아이디
          </label>

          <input
            id="loginId"
            type="text"
            value={loginId}
            onChange={(event) =>
              setLoginId(event.target.value)
            }
            required
          />
        </div>

        <div>
          <label htmlFor="password">
            비밀번호
          </label>

          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) =>
              setPassword(event.target.value)
            }
            required
          />
        </div>

        <button type="submit">
          로그인
        </button>
      </form>

      {successMessage && (
        <p>{successMessage}</p>
      )}

      {errorMessage && (
        <p>{errorMessage}</p>
      )}
    </div>
    )
}

export default LoginPage;
