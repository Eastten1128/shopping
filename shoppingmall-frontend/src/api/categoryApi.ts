import axiosInstance from "./axiosInstance";

import type { CategoryListItem } from "../types/category";


export const getCategories =
    async (): Promise<CategoryListItem[]> => {
        const response =
            await axiosInstance.get<CategoryListItem[]>(
                '/categories'
            );

        return response.data;
    };
