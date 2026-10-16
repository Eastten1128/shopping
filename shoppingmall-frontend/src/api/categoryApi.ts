import axiosInstance from "./axiosInstance";

import type { CategoryListItem } from "../types/category";
import type { AdminCategory } from '../types/category';

export const getAdminCategories = async (includeDeleted = false): Promise<AdminCategory[]> => {
    const response = await axiosInstance.get<AdminCategory[]>('/admin/categories', { params: { includeDeleted } });
    return response.data;
};

export const createCategory = async (categoryName: string): Promise<AdminCategory> => {
    const response = await axiosInstance.post<AdminCategory>('/categories', { categoryName });
    return response.data;
};

export const updateCategory = async (categoryId: number, categoryName: string): Promise<AdminCategory> => {
    const response = await axiosInstance.patch<AdminCategory>(`/admin/categories/${categoryId}`, { categoryName });
    return response.data;
};

export const deleteCategory = async (categoryId: number): Promise<void> => {
    await axiosInstance.delete(`/categories/${categoryId}`);
};


export const getCategories =
    async (): Promise<CategoryListItem[]> => {
        const response = 
            await axiosInstance.get<CategoryListItem[]>(
                '/categories'
            );

        return response.data;
    };
