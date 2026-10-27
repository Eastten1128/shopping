import type { OrderProductResponse } from '../types/order';

function OrderProductsTable({ items }: { items: OrderProductResponse[] }) {
    return (
        <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%' }}>
                <caption>주문 상품</caption>
                <thead>
                    <tr>
                        <th scope="col">상품명</th>
                        <th scope="col">수량</th>
                        <th scope="col">상품 단가</th>
                        <th scope="col">상품 합계</th>
                    </tr>
                </thead>
                <tbody>
                    {items.map((item) => (
                        <tr key={item.orderProductId}>
                            <td>{item.productName}</td>
                            <td>{item.amount}</td>
                            <td>{item.price.toLocaleString()}원</td>
                            <td>{item.orderPrice.toLocaleString()}원</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default OrderProductsTable;
