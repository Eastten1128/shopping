import { useEffect, useRef, useState, type SubmitEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import axios from 'axios';

import { getMyCart } from '../api/cartApi';
import { createOrder } from '../api/orderApi';
import type { CartResponse } from '../types/cart';

function OrderCreatePage() {
    const navigate = useNavigate();
    const [cart, setCart] = useState<CartResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [submitError, setSubmitError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const submittingRef = useRef(false);

    useEffect(() => {
        let isActive = true;
        const fetchCart = async () => {
            try {
                const response = await getMyCart();
                if (isActive) setCart(response);
            } catch (error) {
                if (isActive) setLoadError(axios.isAxiosError<{ message: string }>(error)
                    ? error.response?.data.message ?? '주문 상품을 불러오지 못했습니다.'
                    : '알 수 없는 오류가 발생했습니다.');
            } finally {
                if (isActive) setIsLoading(false);
            }
        };
        void fetchCart();
        return () => { isActive = false; };
    }, []);

    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!cart?.items.length || submittingRef.current) return;
        const form = new FormData(event.currentTarget);
        const value = (key: string) => String(form.get(key) ?? '').trim();
        const request = {
            name: value('name'),
            address1: value('address1'),
            address2: value('address2'),
            phoneNumber: `${value('phone1')}-${value('phone2')}-${value('phone3')}`,
            requestedTerm: value('requestedTerm'),
        };
        if (!request.name || !request.address1 || !/^\d{3}-\d{4}-\d{4}$/.test(request.phoneNumber)) {
            setSubmitError('이름, 주소1, 전화번호(3-4-4자리)를 확인해주세요.');
            return;
        }
        submittingRef.current = true;
        setIsSubmitting(true);
        setSubmitError('');
        try {
            const order = await createOrder(request);
            navigate(`/orders/${order.orderId}`, { replace: true });
        } catch (error) {
            setSubmitError(axios.isAxiosError<{ message: string }>(error)
                ? error.response?.data.message ?? '주문을 생성하지 못했습니다.'
                : '알 수 없는 오류가 발생했습니다.');
            submittingRef.current = false;
            setIsSubmitting(false);
        }
    };

    if (isLoading) return <p>주문 상품을 불러오는 중입니다.</p>;
    if (loadError) return <p role="alert">{loadError}</p>;
    if (!cart?.items.length) return <div><h1>주문서</h1><p>장바구니가 비어있습니다</p><Link to="/products">상품 보러 가기</Link></div>;

    return (
        <div>
            <h1>주문서</h1>
            <form onSubmit={(event) => void handleSubmit(event)}>
                <fieldset disabled={isSubmitting}>
                    <legend>배송 정보</legend>
                    <p><label htmlFor="order-name">이름</label>{' '}
                        <input id="order-name" name="name" autoComplete="shipping name" required pattern=".*\S.*" /></p>
                    <p><label htmlFor="order-address1">주소1</label>{' '}
                        <textarea id="order-address1" name="address1" autoComplete="shipping address-line1" required /></p>
                    <p><label htmlFor="order-address2">주소2</label>{' '}
                        <textarea id="order-address2" name="address2" autoComplete="shipping address-line2" /></p>
                    <div role="group" aria-label="전화번호">
                        <span>전화번호 </span>
                        <input aria-label="전화번호 앞 3자리" name="phone1" type="tel" inputMode="numeric" pattern="[0-9]{3}" minLength={3} maxLength={3} size={3} required />
                        {' - '}
                        <input aria-label="전화번호 중간 4자리" name="phone2" type="tel" inputMode="numeric" pattern="[0-9]{4}" minLength={4} maxLength={4} size={4} required />
                        {' - '}
                        <input aria-label="전화번호 끝 4자리" name="phone3" type="tel" inputMode="numeric" pattern="[0-9]{4}" minLength={4} maxLength={4} size={4} required />
                    </div>
                    <p><label htmlFor="order-request">요청사항</label>{' '}
                        <textarea id="order-request" name="requestedTerm" /></p>
                </fieldset>
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%' }}>
                        <caption>주문할 장바구니 상품</caption>
                        <thead><tr><th scope="col">상품명</th><th scope="col">수량</th><th scope="col">상품 합계</th></tr></thead>
                        <tbody>{cart.items.map((item) => <tr key={item.cartItemId}>
                            <td>{item.productName}</td><td>{item.quantity}</td><td>{item.totalPrice.toLocaleString()}원</td>
                        </tr>)}</tbody>
                    </table>
                </div>
                <h2>총 주문금액: {cart.totalCartPrice.toLocaleString()}원</h2>
                {submitError && <p role="alert">{submitError}</p>}
                <button type="submit" disabled={isSubmitting}>{isSubmitting ? '주문 생성 중...' : '결제'}</button>
            </form>
            <Link to="/cart">장바구니로 돌아가기</Link>
        </div>
    );
}

export default OrderCreatePage;
