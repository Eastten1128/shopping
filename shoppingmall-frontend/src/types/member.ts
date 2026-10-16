export type Role =
    | 'USER'
    | 'SELLER'
    | 'ADMIN';

export interface MemberSignupRequest {
    loginId: string;
    password: string;
    email: string;
};

export interface MemberLoginRequest {
    loginId: string;
    password: string;
};

export interface AuthResponse {
    tokenType: string;
    accessToken: string;
    memberId: number;
    loginId: string;
    role: Role;
};
