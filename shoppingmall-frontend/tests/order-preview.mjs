// 실제 DB에 연결하지 않는 주문 화면 브라우저 검증 서버.
// node tests/order-preview.mjs [normal|empty|guest|error] [port]
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

const scenario = process.argv[2] ?? 'normal';
const port = Number(process.argv[3] ?? 5191);
let authenticated = scenario !== 'guest';
const user = { memberId: 1, loginId: 'order-test', role: 'USER', tokenType: 'Bearer', accessToken: 'fixture-access' };
const items = scenario === 'empty' ? [] : [
    { cartItemId: 1, productId: 7, productName: '테스트 키보드', quantity: 2, cartPrice: 30000, currentPrice: 30000, totalPrice: 60000 },
    { cartItemId: 2, productId: 8, productName: '테스트 마우스', quantity: 1, cartPrice: 15000, currentPrice: 15000, totalPrice: 15000 },
];
const base = { memberId: 1, name: '테스트', phoneNumber: '010-1234-5678', address1: '테스트 주소', address2: '', requestedTerm: '', updateOrderDate: null };
const orders = scenario === 'empty' ? [] : [
    { ...base, orderId: 1, orderDate: '2026-09-20T12:00:00', orderStatus: 'PAID', totalOrderPrice: 10000,
        orderProducts: [{ orderProductId: 1, productId: 9, productName: '과거 주문 상품', amount: 1, price: 10000, orderPrice: 10000 }] },
    { ...base, orderId: 2, orderDate: '2026-10-08T12:00:00', orderStatus: 'ORDERED', totalOrderPrice: 20000,
        orderProducts: [{ orderProductId: 2, productId: 10, productName: '최근 주문 상품', amount: 2, price: 10000, orderPrice: 20000 }] },
];
const server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)), configFile: false,
    plugins: [react(), {
        name: 'order-test-api',
        configureServer(vite) {
            vite.middlewares.use('/api', async (req, res) => {
                const reply = (status, data) => {
                    res.writeHead(status, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify(data));
                };
                const url = req.url?.split('?')[0];
                if (url === '/members/login') { authenticated = true; return reply(200, user); }
                if (url === '/members/logout') { authenticated = false; return reply(200, {}); }
                if (!authenticated) return reply(401, { message: '로그인이 필요합니다.' });
                if (url === '/members/refresh') return reply(200, user);
                if (scenario === 'error') return reply(500, { message: '주문 조회 실패 테스트' });
                if (url === '/carts/me') return reply(200, {
                    cartId: 1, memberId: 1, items, totalCartPrice: items.reduce((sum, item) => sum + item.totalPrice, 0),
                });
                if (url === '/orders' && req.method === 'GET') return reply(200, orders);
                if (url === '/orders' && req.method === 'POST') {
                    let body = '';
                    for await (const chunk of req) body += chunk;
                    const request = JSON.parse(body);
                    if (!items.length) return reply(409, { message: '장바구니가 비어있습니다.' });
                    if (request.requestedTerm === '가격변경테스트') return reply(409, { message: '상품 가격이 변경되었습니다.' });
                    if (!request.name.trim() || !request.address1.trim() || !/^\d{3}-\d{4}-\d{4}$/.test(request.phoneNumber)) {
                        return reply(400, { message: '배송 정보를 확인해주세요.' });
                    }
                    const order = {
                        ...base, ...request, orderId: 3, orderDate: '2026-10-10T12:00:00', orderStatus: 'ORDERED',
                        orderProducts: items.map((item, index) => ({
                            orderProductId: index + 3, productId: item.productId, productName: item.productName,
                            amount: item.quantity, price: item.cartPrice, orderPrice: item.totalPrice,
                        })),
                        totalOrderPrice: items.reduce((sum, item) => sum + item.totalPrice, 0),
                    };
                    orders.push(order);
                    items.length = 0;
                    return reply(201, order);
                }
                if (url?.startsWith('/orders/')) {
                    const order = orders.find((entry) => entry.orderId === Number(url.split('/').pop()));
                    return reply(order ? 200 : 404, order ?? { message: '주문을 찾을 수 없습니다.' });
                }
                reply(404, { message: '테스트 API가 없습니다.' });
            });
        },
    }],
    server: { host: '127.0.0.1', port, strictPort: true },
});
await server.listen();
console.log(`Order fixture (${scenario}): http://127.0.0.1:${port}`);
