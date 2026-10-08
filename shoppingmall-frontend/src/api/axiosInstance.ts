import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '../types/member';

import { getAccessToken, setAccessToken, clearAccessToken } from '../auth/tokenStore';

const authClient = axios.create({ baseURL: '/api', withCredentials: true });
let refreshPromise: Promise<AuthResponse> | null = null;
let onAuthenticated: (response: AuthResponse) => void = () => {};
let onUnauthenticated: () => void = () => {};
let sessionVersion = 0;
let isLoggedOut = false;

export const acceptLogin = (response: AuthResponse) => {
    sessionVersion += 1;
    isLoggedOut = false;
    setAccessToken(response.accessToken);
};

export const setAuthHandlers = (
    authenticated: typeof onAuthenticated,
    unauthenticated: typeof onUnauthenticated,
) => {
    onAuthenticated = authenticated;
    onUnauthenticated = unauthenticated;
    return () => {
        onAuthenticated = () => {};
        onUnauthenticated = () => {};
    };
};

export const invalidateSession = () => {
    sessionVersion += 1;
    clearAccessToken();
    onUnauthenticated();
};

// 초기 복원과 동시에 발생한 401 요청은 동일한 재발급 요청을 공유한다.
export const refreshAuth = (): Promise<AuthResponse> => {
    if (isLoggedOut) return Promise.reject(new Error('로그아웃된 세션입니다.'));
    if (!refreshPromise) {
        const version = sessionVersion;
        refreshPromise = authClient.post<AuthResponse>('/members/refresh')
            .then(({ data }) => {
                if (version !== sessionVersion) throw new Error('인증 상태가 변경되었습니다.');
                setAccessToken(data.accessToken);
                onAuthenticated(data);
                return data;
            })
            .catch((error: unknown) => {
                if (version === sessionVersion) invalidateSession();
                throw error;
            })
            .finally(() => { refreshPromise = null; });
    }
    return refreshPromise;
};

export const logout = async (): Promise<void> => {
    // 진행 중인 rotation의 Set-Cookie가 로그아웃 쿠키 삭제보다 먼저 완료되게 한다.
    isLoggedOut = true;
    invalidateSession();
    await refreshPromise?.catch(() => {});
    await authClient.post('/members/logout');
};

//공통 설정을 가진 Axios 인스턴스를 생성한다.
const axiosInstance = axios.create({
    // baseURL은 API 서버의 기본 URL을 설정한다. 이 URL은 모든 요청에 대해 기본적으로 사용된다.
    baseURL: '/api',
    //인증 관련 Cookie를 사용하는 요청임을 Axios에 알려준다.
    withCredentials: true,
});

//interceptors를 사용하여 요청과 응답을 가로채고 처리할 수 있다.
//axiosInstance가 요청하기 전에 이 함수를 실행하라.
axiosInstance.interceptors.request.use(
    (config) => {
        (config as RetryConfig)._sessionVersion = sessionVersion;
        const accessToken = getAccessToken();

        //accessToken이 존재하면 요청 헤더에 Authorization 헤더를 추가한다.
        if (accessToken) {
            config.headers.set(
                `Authorization`,
                `Bearer ${accessToken}`
            );
        }

        return config;
    }
);

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean; _sessionVersion?: number };

axiosInstance.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
        if (!axios.isAxiosError(error)) throw error;
        const config = error.config as RetryConfig | undefined;
        const isAuthRequest = /^\/members\/(login|signup|refresh|logout)$/.test(config?.url ?? '');
        if (error.response?.status !== 401 || !config || isAuthRequest) throw error;
        if (isLoggedOut || config._sessionVersion !== sessionVersion) throw error;
        if (config._retry) {
            invalidateSession();
            throw error;
        }
        config._retry = true;
        const failedToken = config.headers.get('Authorization');
        const currentToken = getAccessToken();
        // 늦게 도착한 이전 토큰의 401은 이미 갱신한 토큰으로 재시도한다.
        if (!currentToken || failedToken === `Bearer ${currentToken}`) await refreshAuth();
        if (isLoggedOut || config._sessionVersion !== sessionVersion) throw error;
        config.headers.set('Authorization', `Bearer ${getAccessToken()}`);
        return axiosInstance(config);
    },
);

export default axiosInstance;
