export interface CategoryListItem {
    categoryId: number;
    categoryName: string;
}

export interface AdminCategory extends CategoryListItem {
    deleted: boolean;
}
