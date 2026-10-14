
//판매 상태
export type ProductStatus =
    | 'SELLING'
    | 'SOLD_OUT'
    | 'STOPPED';

//상품 표현 정보
export interface ProductListItem {
    productId: number;
    productName: string;
    price: number;
    stock: number;
    productStatus: ProductStatus;
}

//상품 상세정보
export interface ProductDetail {
    productId: number;
    productName: string;
    price: number;
    stock: number;
    productStatus: ProductStatus;
}

export interface SellerProduct extends ProductDetail {
    categoryId: number;
    sellerId: number;
    deleted: boolean;
}

export interface ProductWriteRequest {
    productName: string;
    categoryId: number;
    price: number;
    stock: number;
    productStatus: ProductStatus;
}
