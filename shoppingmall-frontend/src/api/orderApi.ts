import axiosInstance from './axiosInstance';
import type { OrderCreateRequest, OrderResponse } from '../types/order';

export const createOrder = async (request: OrderCreateRequest): Promise<OrderResponse> => {
    const response = await axiosInstance.post<OrderResponse>('/orders', request);
    return response.data;
};

export const getMyOrders = async (): Promise<OrderResponse[]> => {
    const response = await axiosInstance.get<OrderResponse[]>('/orders');
    return [...response.data].sort((left, right) =>
        right.orderDate.localeCompare(left.orderDate) || right.orderId - left.orderId);
};

export const getMyOrder = async (orderId: number): Promise<OrderResponse> => {
    const response = await axiosInstance.get<OrderResponse>(`/orders/${orderId}`);
    return response.data;
};
