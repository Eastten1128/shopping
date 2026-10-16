import { useContext } from "react";

import { AuthContext } from "./AuthContext";

/**
 * 부모 컴포넌트로부터
 * 깊이 중첩된 자식 컴포넌트까지 
 * 데이터를 직접 공유할 수 있게 해주는 훅(Hook)
 */
export const useAuth = () => {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error(
            'useAuth는 AuthProvider 내부에서 사용해야 한다.'
        );
    }

    return context;
};