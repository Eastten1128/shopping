import type {
    //AccessToken은 tokenStore에서 담당하기에 넣지 않음
    AuthResponse,
    Role,
} from '../types/member';

//로그인 한 유저
export interface AuthUser {
    memberId: number;
    loginId: string;
    role: Role;
}

//
export interface AuthContextValue {
    user: AuthUser | null;
    isAuthenticated: boolean;
    isInitializing: boolean;

    setAuth: (
        response: AuthResponse
    ) => void;

    clearAuth: () => void;
}
