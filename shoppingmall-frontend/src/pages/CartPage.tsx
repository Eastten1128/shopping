import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import axios from 'axios';

import { getMyCart } from '../api/cartApi';
import type { CartResponse } from '../types/cart';

function CartPage() {
    const [cart, setCart] = useState<CartResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        let isActive = true;
        const fetchCart = async () => {
            try {
                const response = await getMyCart();
                if (isActive) setCart(response);
            } catch (error) {
                if (isActive) {
                    setErrorMessage(axios.isAxiosError<{ message: string }>(error)
                        ? error.response?.data.message ?? '장바구니를 불러오지 못했습니다.'
                        : '알 수 없는 오류가 발생했습니다.');
                }
            } finally {
                if (isActive) setIsLoading(false);
            }
        };
        void fetchCart();
        return () => { isActive = false; };
    }, []);

    if (isLoading) return <p>장바구니를 불러오는 중입니다.</p>;
    if (errorMessage) return <p role="alert">{errorMessage}</p>;
    if (!cart) return <p role="alert">장바구니 정보가 없습니다.</p>;

    return (
        <div>
            <h1>장바구니</h1>
            {cart.items.length === 0 ? <p>장바구니가 비어있습니다</p> : (
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%' }}>
                        <caption>내 장바구니 상품</caption>
                        <thead>
                            <tr>
                                <th scope="col">상품명</th>
                                <th scope="col">현재 가격</th>
                                <th scope="col">수량</th>
                                <th scope="col">담을 당시 가격</th>
                                <th scope="col">상품 합계</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cart.items.map((item) => (
                                <tr key={item.cartItemId}>
                                    <td><Link to={`/products/${item.productId}`}>{item.productName}</Link></td>
                                    <td>{item.currentPrice.toLocaleString()}원</td>
                                    <td>{item.quantity}</td>
                                    <td>{item.cartPrice.toLocaleString()}원</td>
                                    <td>{item.totalPrice.toLocaleString()}원</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <section aria-label="장바구니 총 금액">
                <h2>총 금액: {cart.totalCartPrice.toLocaleString()}원</h2>
                <p>담을 당시 가격을 기준으로 계산한 금액입니다.</p>
            </section>
            <Link to="/products">쇼핑 계속하기</Link>
        </div>
    );
}

export default CartPage;
