import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import axios from 'axios';

import { getMyOrder } from '../api/orderApi';
import OrderProductsTable from '../components/OrderProductsTable';
import type { OrderResponse } from '../types/order';

function OrderDetail({ orderId }: { orderId: number }) {
    const [order, setOrder] = useState<OrderResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        let isActive = true;
        const fetchOrder = async () => {
            try {
                const response = await getMyOrder(orderId);
                if (isActive) setOrder(response);
            } catch (error) {
                if (isActive) setErrorMessage(axios.isAxiosError<{ message: string }>(error)
                    ? error.response?.data.message ?? '주문 상세를 불러오지 못했습니다.'
                    : '알 수 없는 오류가 발생했습니다.');
            } finally {
                if (isActive) setIsLoading(false);
            }
        };
        void fetchOrder();
        return () => { isActive = false; };
    }, [orderId]);

    if (isLoading) return <p>주문 상세를 불러오는 중입니다.</p>;
    if (errorMessage) return <div><p role="alert">{errorMessage}</p><Link to="/orders">내 주문 내역</Link></div>;
    if (!order) return <p>주문 정보가 없습니다.</p>;

    return (
        <div>
            <h1>주문 상세</h1>
            <p>주문 번호: {order.orderId}</p>
            <p>주문날짜: <time dateTime={order.orderDate}>{order.orderDate.replace('T', ' ').slice(0, 19)}</time></p>
            <p>주문 상태: {order.orderStatus}</p>
            <OrderProductsTable items={order.orderProducts} />
            <h2>총 가격: {order.totalOrderPrice.toLocaleString()}원</h2>
            <Link to="/orders">내 주문 내역</Link>
        </div>
    );
}

function OrderDetailPage() {
    const { orderId } = useParams<{ orderId: string }>();
    const parsedId = Number(orderId);
    if (!Number.isSafeInteger(parsedId) || parsedId < 1) return <p role="alert">주문 번호가 올바르지 않습니다.</p>;
    return <OrderDetail key={parsedId} orderId={parsedId} />;
}

export default OrderDetailPage;
