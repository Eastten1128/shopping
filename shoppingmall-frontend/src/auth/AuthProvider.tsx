import {useState, useCallback, useEffect, type ReactNode,} from 'react'
import { refreshAuth } from '../api/memberApi';
import { setAuthHandlers } from '../api/axiosInstance';
import { AuthContext } from "./AuthContext";

import {
    clearAccessToken,
    setAccessToken,
} from './tokenStore'

import type { AuthUser } from "./authTypes";
import type { AuthResponse } from "../types/member";

interface AuthProviderProps {
    children: ReactNode;
}

//AuthProvider
function AuthProvider({
    children,
}: AuthProviderProps){
    const [user, setUser] = useState<AuthUser | null>(null);

    const [isInitializing, setIsInitializing] = useState(true);

    // Login 정보 설정
    // Login 정보 설정
    const setAuth = useCallback((response: AuthResponse) => {
        //accessToken을 tokenStore로 전달
        setAccessToken(response.accessToken);

        //setUser -> React State 전달
        setUser({
            memberId: response.memberId,
            loginId: response.loginId,
            role: response.role,
        });
    },[]);

    //비로그인 상태로들기
    const clearAuth = useCallback(() => {
        //엑세스 토큰 제거
        clearAccessToken();
        //유저 정보 -> null
        setUser(null);
    }, []);

    useEffect(() => {
    let isActive = true;
    const unsubscribe = setAuthHandlers(setAuth, clearAuth);

    const restoreAuth = async () => {
      try {
        const response =
          await refreshAuth();

        if (isActive) {
          setAuth(response);
        }
      } catch {
        if (isActive) {
          clearAuth();
        }
      } finally {
        if (isActive) {
          setIsInitializing(false);
        }
      }
    };

    void restoreAuth();

    return () => {
      isActive = false;
      unsubscribe();
    };
  }, [setAuth, clearAuth]);

    //AuthProvider 호출 시 로그인 여부 boolean값 설정
    const isAuthenticated = user !== null;

    return (
        <AuthContext.Provider
            value= {{
                user,
                isAuthenticated,
                isInitializing,
                setAuth,
                clearAuth,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export default AuthProvider;
