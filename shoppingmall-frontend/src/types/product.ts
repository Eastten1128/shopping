
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
    sellerLoginId: string;
    deleted: boolean;
    createdAt: string;
    updatedAt: string | null;
}

// ProductCreateRequest / ProductUpdateRequest와 동일한 요청 필드. 판매자는 서버 인증 정보로 결정한다.
export interface ProductWriteRequest {
    productName: string;
    categoryId: number;
    price: number;
    stock: number;
    productStatus: ProductStatus;
}
