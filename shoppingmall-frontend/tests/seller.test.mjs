import assert from 'node:assert/strict';
import { test } from 'node:test';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';

// Playwright 설치 경로를 환경변수로 전달하면 프로젝트 의존성을 추가하지 않고 실행할 수 있다.
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE_PATH
    ? pathToFileURL(process.env.PLAYWRIGHT_MODULE_PATH).href : 'playwright');

test('SELLER 상품 등록·수정·중지·필터 및 접근 제한', { timeout: 90000 }, async () => {
    let role = 'SELLER';
    let loggedIn = true;
    let failSave = false;
    let failStop = false;
    let deletes = 0;
    const writes = [];
    const products = [
        { productId: 1, categoryId: 1, sellerId: 1, productName: '기존 상품', price: 1000, stock: 5, productStatus: 'SELLING', deleted: false },
        { productId: 2, categoryId: 1, sellerId: 1, productName: '중지된 상품', price: 2000, stock: 2, productStatus: 'STOPPED', deleted: true },
        { productId: 99, categoryId: 1, sellerId: 9, productName: '다른 판매자 상품', price: 3000, stock: 1, productStatus: 'SELLING', deleted: false },
    ];
    const server = await createServer({
        root: fileURLToPath(new URL('..', import.meta.url)),
        configFile: false,
        plugins: [react(), {
            name: 'seller-test-api',
            configureServer(vite) {
                vite.middlewares.use(async (req, res, next) => {
                    const url = new URL(req.url, 'http://localhost');
                    if (!url.pathname.startsWith('/api/')) return next();
                    const send = (status, data) => {
                        res.statusCode = status;
                        res.setHeader('Content-Type', 'application/json');
                        res.end(JSON.stringify(data));
                    };
                    if (url.pathname === '/api/members/refresh') return send(loggedIn ? 200 : 401,
                        { accessToken: 'test-access', memberId: 1, loginId: 'seller', role, tokenType: 'Bearer' });
                    if (url.pathname === '/api/categories') return send(200, [{ categoryId: 1, categoryName: '키보드' }, { categoryId: 2, categoryName: '마우스' }]);
                    if (!loggedIn || role !== 'SELLER') return send(403, { message: 'Forbidden' });
                    assert.equal(req.headers.authorization, 'Bearer test-access');
                    if (url.pathname === '/api/seller/products' && req.method === 'GET') {
                        return send(200, products.filter(p => p.sellerId === 1 && (url.searchParams.get('includeStopped') === 'true' || !p.deleted)));
                    }
                    const id = Number(url.pathname.split('/').at(-1));
                    const product = products.find(p => p.productId === id && p.sellerId === 1);
                    if (req.method === 'GET') return send(product ? 200 : 404, product ?? {});
                    if (req.method === 'DELETE') {
                        deletes++;
                        if (failStop) return send(500, {});
                        if (!product) return send(404, {});
                        product.deleted = true; product.productStatus = 'STOPPED';
                        return send(204);
                    }
                    let body = '';
                    for await (const chunk of req) body += chunk;
                    const data = JSON.parse(body);
                    writes.push(data);
                    assert.equal('sellerId' in data, false);
                    if (failSave) return send(500, {});
                    if (req.method === 'POST' && url.pathname === '/api/products') {
                        const created = { ...data, productId: 3, sellerId: 1, deleted: false };
                        products.push(created);
                        return send(201, created);
                    }
                    if (req.method === 'PATCH' && product) {
                        Object.assign(product, data, { productStatus: product.deleted ? 'STOPPED' : data.productStatus });
                        return send(200, product);
                    }
                    send(404, {});
                });
            },
        }],
        server: { host: '127.0.0.1', port: 0 },
    });
    let browser;
    try {
        await server.listen();
        const origin = `http://127.0.0.1:${server.httpServer.address().port}`;
        browser = await chromium.launch({ channel: 'msedge', headless: true });
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(`${origin}/seller/products`);
        await page.getByRole('heading', { name: '기존 상품', exact: true }).waitFor();
        assert.equal(await page.getByText('다른 판매자 상품', { exact: true }).count(), 0);
        assert.equal(await page.getByRole('heading', { name: '중지된 상품', exact: true }).count(), 0);
        await page.getByLabel('판매중지된 상품 보기').check();
        await page.getByRole('heading', { name: '중지된 상품', exact: true }).waitFor();

        await page.getByRole('link', { name: '상품 등록', exact: true }).first().click();
        await page.getByLabel('상품명', { exact: true }).fill('신규 상품');
        await page.getByLabel('카테고리', { exact: true }).selectOption('2');
        await page.getByLabel('가격', { exact: true }).fill('-1');
        await page.getByLabel('재고', { exact: true }).fill('3');
        await page.getByRole('button', { name: '상품 등록', exact: true }).click();
        assert.equal(writes.length, 0, '음수 가격은 전송하지 않는다');
        await page.getByLabel('가격', { exact: true }).fill('15000');
        failSave = true;
        await page.getByRole('button', { name: '상품 등록', exact: true }).click();
        await page.getByRole('alert').filter({ hasText: '상품 저장에 실패' }).waitFor();
        assert.equal(await page.getByLabel('상품명', { exact: true }).inputValue(), '신규 상품');
        failSave = false;
        await page.getByRole('button', { name: '상품 등록', exact: true }).click();
        await page.getByRole('heading', { name: '신규 상품', exact: true }).waitFor();
        assert.deepEqual(writes.at(-1), { productName: '신규 상품', categoryId: 2, price: 15000, stock: 3, productStatus: 'SELLING' });

        await page.locator('li').filter({ has: page.getByRole('heading', { name: '신규 상품', exact: true }) }).getByRole('link', { name: '상품수정' }).click();
        await page.getByLabel('상품명', { exact: true }).fill('수정 상품');
        await page.getByLabel('재고', { exact: true }).fill('0');
        await page.getByRole('button', { name: '수정 저장' }).click();
        await page.getByRole('heading', { name: '수정 상품', exact: true }).waitFor();
        assert.equal(writes.at(-1).productStatus, 'SOLD_OUT');
        const row = page.locator('li').filter({ has: page.getByRole('heading', { name: '수정 상품', exact: true }) });
        await row.getByRole('button', { name: '판매중지', exact: true }).click();
        await page.getByRole('dialog').getByRole('button', { name: '아니오', exact: true }).click();
        assert.equal(deletes, 0);
        await row.getByRole('button', { name: '판매중지', exact: true }).click();
        failStop = true;
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await page.getByRole('dialog').getByRole('alert').waitFor();
        failStop = false;
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await page.getByRole('heading', { name: '수정 상품', exact: true }).waitFor({ state: 'detached' });
        await page.getByLabel('판매중지된 상품 보기').check();
        await page.getByRole('heading', { name: '수정 상품', exact: true }).waitFor();
        assert.equal(await row.getByRole('button', { name: '판매중지', exact: true }).isDisabled(), true);

        await row.getByRole('link', { name: '상품수정' }).click();
        await page.getByText('판매중지된 상품입니다.', { exact: false }).waitFor();
        await page.getByLabel('상품명', { exact: true }).fill('중지 후 수정');
        await page.getByRole('button', { name: '수정 저장' }).click();
        await page.getByRole('heading', { name: '내 판매상품', exact: true }).waitFor();
        assert.equal(products.find(p => p.productId === 3).deleted, true);
        assert.equal(writes.at(-1).productStatus, 'STOPPED');

        await page.goto(`${origin}/seller/products/1/edit`);
        await page.getByRole('button', { name: '판매중지', exact: true }).click();
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await page.getByText('등록된 상품이 없습니다.', { exact: true }).waitFor();
        await page.goto(`${origin}/seller/products/99/edit`);
        await page.getByRole('alert').filter({ hasText: '본인 상품인지' }).waitFor();
        assert.equal(await page.getByRole('button', { name: '수정 저장' }).count(), 0);

        for (const blockedRole of ['USER', 'ADMIN']) {
            role = blockedRole;
            await page.goto(`${origin}/seller/products/new`);
            await page.getByRole('alert').filter({ hasText: '판매자만 이용' }).waitFor();
            assert.equal(await page.getByRole('link', { name: '내 판매상품', exact: true }).count(), 0);
        }
        loggedIn = false;
        await page.goto(`${origin}/seller/products/new`);
        await page.getByRole('heading', { name: '로그인', exact: true }).waitFor();
        assert.deepEqual(errors, []);
    } finally {
        await browser?.close();
        await server.close();
    }
});
