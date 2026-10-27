export type OrderStatus = 'ORDERED' | 'PAID' | 'CANCELLED' | 'FAILED';

export interface OrderCreateRequest {
    name: string;
    phoneNumber: string;
    address1: string;
    address2: string;
    requestedTerm: string;
}

export interface OrderProductResponse {
    orderProductId: number;
    productId: number;
    productName: string;
    amount: number;
    price: number;
    orderPrice: number;
}

export interface OrderResponse {
    orderId: number;
    memberId: number;
    name: string;
    phoneNumber: string;
    address1: string;
    address2: string | null;
    requestedTerm: string | null;
    totalOrderPrice: number;
    orderStatus: OrderStatus;
    orderDate: string;
    updateOrderDate: string | null;
    orderProducts: OrderProductResponse[];
}
