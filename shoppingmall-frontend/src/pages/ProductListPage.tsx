import { useEffect, useState, type ChangeEvent } from 'react';
import { Link } from 'react-router';
import axios from 'axios';

import { getProducts } from '../api/productApi';
import { getCategories } from '../api/categoryApi';

import type { ProductListItem } from '../types/product';
import type { CategoryListItem } from '../types/category';


function ProductListPage() {

    const [categories, setCategories] = useState<CategoryListItem[]>([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);

    const [products, setProducts] = useState<ProductListItem[]>([]);

    const [isCategoryLoading, setIsCategoryLoading] = useState(true);
    const [isProductLoading, setIsProductLoading] =useState(true);

    const [categoryErrorMessage, setCategoryErrorMessage] = useState('');
    const [productErrorMessage, setProductErrorMessage] = useState('');

    //카테고리 Fetch
    useEffect(()=>{
        const fetchCategories = async () => {
            try {
                const response = await getCategories();

                setCategories(response);
            } catch(error) {
                if (axios.isAxiosError(error)){
                    setCategoryErrorMessage(
                        error.response?.data.message ??
                        '카테고리를 불러오지 못했습니다.'
                    );

                    return;
                }
                setCategoryErrorMessage("알 수 없는 에러 발생");
            } finally {
                setIsCategoryLoading(false);
            }
        };

        fetchCategories();
    }, []);

    //상품 Fetch
    useEffect(() => {
        const fetchProducts = async () => {
            setIsProductLoading(true);
            setProductErrorMessage('');

            try{
                const response = await getProducts(
                    selectedCategoryId ?? undefined
                );
                setProducts(response);
            } catch (error) {
                if (axios.isAxiosError(error)) {
                    setProductErrorMessage(
                        error.response?.data.message ??
                        '상품 목록을 불러오지 못했습니다.'
                    );

                    return;
                }
                setProductErrorMessage('알 수 없는 에러 발생.');
            } finally {
                setIsProductLoading(false);
            }
        };
        fetchProducts();
    }, [selectedCategoryId]);

    //카테고리를 바꿈
    const handleCategoryChange = (
        event: ChangeEvent<HTMLSelectElement>
    ) => {
        const value = event.target.value;

        if (value === '') {
            setSelectedCategoryId(null);
            return;
        }
        setSelectedCategoryId(Number(value));
    };


    return (
        <div>
      <h1>상품 목록</h1>

      <div>
        <label htmlFor="category">
          카테고리
        </label>

        {isCategoryLoading ? (
          <p>카테고리를 불러오는 중입니다.</p>
        ) : categoryErrorMessage ? (
          <p>{categoryErrorMessage}</p>
        ) : (
          <select
            id="category"
            value={selectedCategoryId ?? ''}
            onChange={handleCategoryChange}
          >
            <option value="">
              전체
            </option>

            {categories.map((category) => (
              <option
                key={category.categoryId}
                value={category.categoryId}
              >
                {category.categoryName}
              </option>
            ))}
          </select>
        )}
      </div>

      {isProductLoading ? (
        <p>상품을 불러오는 중입니다.</p>
      ) : productErrorMessage ? (
        <p>{productErrorMessage}</p>
      ) : products.length === 0 ? (
        <p>등록된 상품이 없습니다.</p>
      ) : (
        <ul>
          {products.map((product) => (
            <li key={product.productId}>
              <h2>
                {product.productName}
              </h2>

              <p>
                가격:{' '}
                {product.price.toLocaleString()}원
              </p>

              <p>
                재고: {product.stock}
              </p>

              <p>
                상태: {product.productStatus}
              </p>

              <Link
                to={`/products/${product.productId}`}
              >
                상세 보기
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
    ); //End of return
}//EOF

export default ProductListPage;
