import { useState, type SubmitEvent } from 'react';
import axios from 'axios';
import { signup } from '../api/memberApi';
import type { MemberSignupRequest } from '../types/member';

interface ErrorResponse {
    message: string;
}

function SignupPage() {
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');

    const [successMessage, setSuccessMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');

    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        //Form 태그의 기본 동작인 페이지 새로고침을 방지한다.
        event.preventDefault();

        // 회원가입 요청을 보내기 전에 성공 및 에러 메시지를 초기화한다.
        setSuccessMessage('');
        setErrorMessage('');

        const request: MemberSignupRequest = {
            loginId,
            password,
            email,
        };

        try {
            await signup(request);
            setSuccessMessage('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.');
            // 회원가입 성공 후 로그인 페이지로 이동하는 로직을 추가할 수 있습니다.

            setLoginId('');
            setPassword('');
            setEmail('');
        } catch (error){
            if(axios.isAxiosError<ErrorResponse>(error)){
                setErrorMessage(
                    error.response?.data.message ?? '회원가입에 실패했습니다.'
                );

                return;
            }

            setErrorMessage('알 수 없는 에러 발생.');
        }
    };

    return (
        <div>
            <h1>회원가입</h1>
            
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor = "loginId">아이디</label>
                    <input
                        id="loginId"
                        type="text"
                        value={loginId}
                        onChange={(event) => setLoginId(event.target.value)}
                    />
                </div>
                <div>
                    <label htmlFor = "password">비밀번호</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                    />
                </div>
                <div>
                    <label htmlFor = "email">이메일</label>
                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                    />
                </div>

                <button type="submit">회원가입</button>
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

export default SignupPage;