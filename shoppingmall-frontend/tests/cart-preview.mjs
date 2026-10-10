// 브라우저 검증용 메모리 API. 실제 Backend/DB에는 요청하지 않는다.
// node tests/cart-preview.mjs [normal|guest|error] [port]
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

const scenario = process.argv[2] ?? 'normal';
const port = Number(process.argv[3] ?? 5179);
let authenticated = scenario !== 'guest';
const items = [];
const user = { memberId: 1, loginId: 'cart-test', role: 'USER', tokenType: 'Bearer', accessToken: 'fixture-access' };
const products = [
    { productId: 1, productName: '테스트 키보드', price: 30000, stock: 3, productStatus: 'SELLING' },
    { productId: 2, productName: '재고 변경 상품', price: 5000, stock: 3, productStatus: 'SELLING' },
    { productId: 3, productName: '품절 상품', price: 1000, stock: 0, productStatus: 'SOLD_OUT' },
];
const server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    configFile: false,
    plugins: [react(), {
        name: 'cart-test-api',
        configureServer(vite) {
            vite.middlewares.use('/api', async (req, res) => {
                const reply = (status, data) => {
                    res.writeHead(status, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify(data));
                };
                const url = req.url?.split('?')[0];
                if (url === '/members/login') {
                    authenticated = true;
                    reply(200, user);
                } else if (url === '/members/refresh') {
                    reply(authenticated ? 200 : 401, authenticated ? user : { message: '로그인이 필요합니다.' });
                } else if (url === '/members/logout') {
                    authenticated = false;
                    reply(200, {});
                } else if (url === '/products') {
                    reply(200, products);
                } else if (url === '/categories') {
                    reply(200, []);
                } else if (url?.startsWith('/products/')) {
                    const product = products.find((entry) => entry.productId === Number(url.split('/').pop()));
                    reply(product ? 200 : 404, product ?? { message: '상품이 없습니다.' });
                } else if (url === '/carts/me') {
                    if (!authenticated) return reply(401, { message: '로그인이 필요합니다.' });
                    if (scenario === 'error') return reply(500, { message: '장바구니 조회 실패 테스트' });
                    reply(200, {
                        cartId: 1, memberId: 1, items,
                        totalCartPrice: items.reduce((total, item) => total + item.totalPrice, 0),
                    });
                } else if (url === '/carts/items' && req.method === 'POST') {
                    if (!authenticated) return reply(401, { message: '로그인이 필요합니다.' });
                    let body = '';
                    for await (const chunk of req) body += chunk;
                    const request = JSON.parse(body);
                    const product = products.find((entry) => entry.productId === request.productId);
                    const existing = items.find((item) => item.productId === request.productId);
                    const quantity = (existing?.quantity ?? 0) + request.quantity;
                    if (!product || product.productId === 2 || quantity > product.stock) {
                        return reply(409, { message: '상품 재고가 부족합니다.' });
                    }
                    const item = existing ?? {
                        ...product, cartItemId: items.length + 1, cartPrice: product.price,
                        currentPrice: product.price + 1000, addedAt: '2026-10-10T12:00:00', updatedAt: null,
                    };
                    item.quantity = quantity;
                    item.totalPrice = item.cartPrice * quantity;
                    if (!existing) items.push(item);
                    reply(201, item);
                } else {
                    reply(404, { message: '테스트 API가 없습니다.' });
                }
            });
        },
    }],
    server: { host: '127.0.0.1', port, strictPort: true },
});
await server.listen();
console.log(`Cart fixture (${scenario}): http://127.0.0.1:${port}`);
