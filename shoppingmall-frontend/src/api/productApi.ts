import axiosInstance from './axiosInstance';
import type { ProductListItem, ProductDetail } from '../types/product';
import type { SellerProduct, ProductWriteRequest } from '../types/product';

export const getSellerProducts = async (includeStopped = false): Promise<SellerProduct[]> => {
  const response = await axiosInstance.get<SellerProduct[]>('/seller/products', { params: { includeStopped } });
  return response.data;
};

export const getSellerProduct = async (productId: number): Promise<SellerProduct> => {
  const response = await axiosInstance.get<SellerProduct>(`/seller/products/${productId}`);
  return response.data;
};

export const createProduct = async (request: ProductWriteRequest): Promise<SellerProduct> => {
  const response = await axiosInstance.post<SellerProduct>('/products', request);
  return response.data;
};

export const updateSellerProduct = async (productId: number, request: ProductWriteRequest): Promise<SellerProduct> => {
  const response = await axiosInstance.patch<SellerProduct>(`/seller/products/${productId}`, request);
  return response.data;
};

export const stopProduct = async (productId: number): Promise<void> => {
  await axiosInstance.delete(`/products/${productId}`);
};

export const getProducts = async (
  categoryId?: number
): Promise<ProductListItem[]> => {
  const response =
    await axiosInstance.get<ProductListItem[]>(
      '/products',
      {
        params:
          categoryId === undefined
            ? undefined
            : { categoryId },
      }
    );

  return response.data;
};

export const getProductById = async(
    productId: number
): Promise<ProductDetail> => {
    const response = await axiosInstance.get<ProductDetail>(
        `/products/${productId}`
    );

    return response.data;
};
