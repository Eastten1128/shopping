import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import axios from 'axios';

import { getMyOrders } from '../api/orderApi';
import OrderProductsTable from '../components/OrderProductsTable';
import type { OrderResponse } from '../types/order';

function OrderListPage() {
    const navigate = useNavigate();
    const [orders, setOrders] = useState<OrderResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        let isActive = true;
        const fetchOrders = async () => {
            try {
                const response = await getMyOrders();
                if (isActive) setOrders(response);
            } catch (error) {
                if (isActive) setErrorMessage(axios.isAxiosError<{ message: string }>(error)
                    ? error.response?.data.message ?? '주문 내역을 불러오지 못했습니다.'
                    : '알 수 없는 오류가 발생했습니다.');
            } finally {
                if (isActive) setIsLoading(false);
            }
        };
        void fetchOrders();
        return () => { isActive = false; };
    }, []);

    if (isLoading) return <p>주문 내역을 불러오는 중입니다.</p>;
    if (errorMessage) return <p role="alert">{errorMessage}</p>;

    return (
        <div>
            <h1>내 주문 내역</h1>
            {orders.length === 0 && <p>주문 내역이 없습니다.</p>}
            {orders.map((order) => (
                <section key={order.orderId} aria-label={`주문 ${order.orderId}`}>
                    <h2>주문 번호: {order.orderId}</h2>
                    <p>주문날짜: <time dateTime={order.orderDate}>{order.orderDate.replace('T', ' ').slice(0, 19)}</time></p>
                    <p>주문 상태: {order.orderStatus}</p>
                    <button type="button" onClick={() => navigate(`/orders/${order.orderId}`)}>주문 상세</button>
                    <OrderProductsTable items={order.orderProducts} />
                    <p>총 가격: {order.totalOrderPrice.toLocaleString()}원</p>
                </section>
            ))}
        </div>
    );
}

export default OrderListPage;
