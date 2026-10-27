import axiosInstance from './axiosInstance';
import type { CartItemAddRequest, CartItemResponse } from '../types/cart';

// 회원 정보와 가격은 서버에서 결정하고, 기존 장바구니가 있으면 재사용한다.
export const addCartItem = async (request: CartItemAddRequest): Promise<CartItemResponse> => {
    const response = await axiosInstance.post<CartItemResponse>('/carts/items', request);
    return response.data;
};
