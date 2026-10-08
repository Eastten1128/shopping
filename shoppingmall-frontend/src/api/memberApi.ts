import axiosInstance, { acceptLogin } from './axiosInstance';
import type {
    MemberSignupRequest,
    MemberLoginRequest,
    AuthResponse,
} from '../types/member';


//회원가입 API
export const signup = async (request: MemberSignupRequest) => {
    const response = await axiosInstance.post('/members/signup', request);

    return response.data;
}

//로그인 API
export const login = async (request: MemberLoginRequest): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>
    ('/members/login', request);

    acceptLogin(response.data);
    return response.data;
}



export { refreshAuth, logout } from './axiosInstance';

//내 정보 출력 API
export const getMyInfo = async (): Promise<void> => {
   await axiosInstance.get('/members/me');
};
