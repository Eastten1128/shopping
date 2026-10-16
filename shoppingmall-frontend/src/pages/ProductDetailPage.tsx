import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import axios from 'axios';

import { getProductById } from '../api/productApi';
import { addCartItem } from '../api/cartItemApi';
import { useAuth } from '../auth/useAuth';
import type { ProductDetail } from '../types/product';

interface ErrorResponse {
    message: string;
}

function ProductDetailPage(){
    const { isAuthenticated } = useAuth();
    const navigate = useNavigate();
    //productId
    const { productId } = useParams<{
        productId: string;
    }>();

    const [product, setProduct] = useState<ProductDetail | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [isAdding, setIsAdding] = useState(false);
    const [cartMessage, setCartMessage] = useState('');
    const [cartError, setCartError] = useState('');
    const addingRef = useRef(false);

    useEffect(() => {
        let isActive = true;
        setProduct(null);
        setIsLoading(true);
        setErrorMessage('');
        setQuantity(1);
        setCartMessage('');
        setCartError('');
        const fetchProduct = async () => {
            if (!productId) {
                setErrorMessage(
                    '상품 번호가 올바르지 않습니다.'
                );
                setIsLoading(false);
                return;
            }

            const parsedProductId = Number(productId);

            if (!Number.isSafeInteger(parsedProductId) || parsedProductId < 1) {
                setErrorMessage(
                    '상품 번호가 올바르지 않습니다.'
                );
                setIsLoading(false);
                return;
            }

            try {
                const response =
                    await getProductById(parsedProductId);

                if (isActive) setProduct(response);
            } catch (error) {
                if (!isActive) return;
                if (axios.isAxiosError<ErrorResponse>(error)) {
                    setErrorMessage(
                        error.response?.data.message ??
                        '상품 정보를 불러오지 못했습니다.'
                    );

                    return;
                }

                setErrorMessage(
                    '알 수 없는 오류가 발생했습니다.'
                );
            } finally {
                if (isActive) setIsLoading(false);
            }
        };

        void fetchProduct();
        return () => { isActive = false; };
    }, [productId]);

    const canAdd = product?.productStatus === 'SELLING' && product.stock >= quantity;
    const handleAddToCart = async () => {
        if (!product || !canAdd || addingRef.current) return;
        if (!isAuthenticated) {
            navigate('/login', { state: { from: `/products/${product.productId}` } });
            return;
        }
        addingRef.current = true;
        setIsAdding(true);
        setCartMessage('');
        setCartError('');
        try {
            await addCartItem({ productId: product.productId, quantity });
            setCartMessage('장바구니에 담았습니다.');
        } catch (error) {
            setCartError(axios.isAxiosError<ErrorResponse>(error)
                ? error.response?.data.message ?? '장바구니에 담지 못했습니다.'
                : '알 수 없는 오류가 발생했습니다.');
        } finally {
            addingRef.current = false;
            setIsAdding(false);
        }
    };

    if (isLoading) {
        return <p>상품 정보를 불러오는 중입니다.</p>;
    }

    if (errorMessage) {
        return (
            <div>
                <p>{errorMessage}</p>

                <Link to="/products">
                    상품 목록으로
                </Link>
            </div>
        );
    }

    if (!product) {
        return <p>상품 정보가 없습니다.</p>;
    }

    return (
        <div>
            <h1>{product.productName}</h1>

            <p>
                상품 번호: {product.productId}
            </p>

            <p>
                가격: {product.price.toLocaleString()}원
            </p>

            <p>
                재고: {product.stock}
            </p>

            <p>
                상태: {product.productStatus}
            </p>

            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '1rem' }}>
                <div role="group" aria-label="상품 수량 선택">
                    <button type="button" aria-label="수량 줄이기" disabled={quantity <= 1 || isAdding}
                        onClick={() => setQuantity((value) => Math.max(1, value - 1))}>-</button>
                    <output aria-label="선택 수량" style={{ margin: '0 0.5rem' }}>{quantity}</output>
                    <button type="button" aria-label="수량 늘리기"
                        disabled={!canAdd || quantity >= product.stock || isAdding}
                        onClick={() => setQuantity((value) => Math.min(product.stock, value + 1))}>+</button>
                </div>
                <span aria-live="polite">상품 금액: {(product.price * quantity).toLocaleString()}원</span>
            </div>
            <button type="button" disabled={!canAdd || isAdding} onClick={() => void handleAddToCart()}>
                {isAdding ? '담는 중...' : '장바구니 담기'}
            </button>
            {!canAdd && <p>현재 구매할 수 없는 상품입니다.</p>}
            {cartMessage && <p role="status">{cartMessage} <Link to="/cart">장바구니 확인</Link></p>}
            {cartError && <p role="alert">{cartError}</p>}

            <Link to="/products">
                상품 목록으로
            </Link>
        </div>
    );
}

export default ProductDetailPage
