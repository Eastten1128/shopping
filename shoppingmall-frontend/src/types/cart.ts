import type { ProductStatus } from './product';

export interface CartItemAddRequest {
    productId: number;
    quantity: number;
}

export interface CartItemResponse {
    cartItemId: number;
    productId: number;
    productName: string;
    quantity: number;
    cartPrice: number;
    currentPrice: number;
    totalPrice: number;
    productStatus: ProductStatus;
    addedAt: string;
    updatedAt: string | null;
}

export interface CartResponse {
    cartId: number;
    memberId: number;
    items: CartItemResponse[];
    totalCartPrice: number;
}
