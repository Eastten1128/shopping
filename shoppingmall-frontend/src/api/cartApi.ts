import axiosInstance from './axiosInstance';
import type { CartResponse } from '../types/cart';

export const getMyCart = async (): Promise<CartResponse> => {
    const response = await axiosInstance.get<CartResponse>('/carts/me');
    return response.data;
};
