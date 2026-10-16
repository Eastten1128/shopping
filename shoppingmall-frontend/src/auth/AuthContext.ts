import { createContext } from "react";

import type {
    AuthContextValue,
} from './authTypes'

export const AuthContext =
    //여러 컴포넌트가 같은 인증 상태를 공유할 수 있도록 context를 생성
    createContext<AuthContextValue | null> (null);
    //인자는 AuthProvider가 공급한다.