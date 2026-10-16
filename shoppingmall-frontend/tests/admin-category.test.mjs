import assert from 'node:assert/strict';
import { test } from 'node:test';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';

const { chromium } = await import(process.env.PLAYWRIGHT_MODULE_PATH
    ? pathToFileURL(process.env.PLAYWRIGHT_MODULE_PATH).href : 'playwright');

test('ADMIN 카테고리 생성·수정·삭제·필터·권한 제한', { timeout: 90000 }, async () => {
    let role = 'ADMIN';
    let loggedIn = true;
    let failList = true;
    let failSave = false;
    let failDelete = false;
    let writes = 0;
    let deletes = 0;
    const categories = [
        { categoryId: 1, categoryName: '키보드', deleted: false },
        { categoryId: 2, categoryName: '삭제된분류', deleted: true },
    ];
    const server = await createServer({
        root: fileURLToPath(new URL('..', import.meta.url)), configFile: false,
        plugins: [react(), {
            name: 'admin-category-test-api',
            configureServer(vite) {
                vite.middlewares.use(async (req, res, next) => {
                    const url = new URL(req.url, 'http://localhost');
                    if (!url.pathname.startsWith('/api/')) return next();
                    const send = (status, data) => {
                        res.statusCode = status; res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(data));
                    };
                    if (url.pathname === '/api/members/refresh') return send(loggedIn ? 200 : 401,
                        { accessToken: 'test-access', memberId: 1, loginId: 'admin', role, tokenType: 'Bearer' });
                    if (url.pathname === '/api/categories' && req.method === 'GET') return send(200, categories.filter(c => !c.deleted));
                    if (!loggedIn || role !== 'ADMIN') return send(403, {});
                    assert.equal(req.headers.authorization, 'Bearer test-access');
                    if (url.pathname === '/api/admin/categories' && req.method === 'GET') {
                        return failList ? send(500, {}) : send(200, categories.filter(c => !c.deleted || url.searchParams.get('includeDeleted') === 'true'));
                    }
                    const id = Number(url.pathname.split('/').at(-1));
                    const category = categories.find(c => c.categoryId === id);
                    if (req.method === 'DELETE') {
                        deletes++;
                        if (failDelete) return send(500, {});
                        if (!category || category.deleted) return send(404, {});
                        category.deleted = true;
                        return send(204);
                    }
                    let body = '';
                    for await (const chunk of req) body += chunk;
                    const data = JSON.parse(body);
                    assert.deepEqual(Object.keys(data), ['categoryName']);
                    writes++;
                    if (failSave) return send(500, {});
                    if (categories.some(c => c.categoryName === data.categoryName && c.categoryId !== id)) return send(409, { message: '이미 존재하는 카테고리명입니다.' });
                    if (req.method === 'POST' && url.pathname === '/api/categories') {
                        const created = { categoryId: 3, categoryName: data.categoryName, deleted: false };
                        categories.push(created); return send(201, created);
                    }
                    if (req.method === 'PATCH' && category) {
                        category.categoryName = data.categoryName; return send(200, category);
                    }
                    send(404, {});
                });
            },
        }], server: { host: '127.0.0.1', port: 0 },
    });
    let browser;
    try {
        await server.listen();
        const origin = `http://127.0.0.1:${server.httpServer.address().port}`;
        browser = await chromium.launch({ channel: 'msedge', headless: true });
        const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const row = name => page.locator('.admin-category-row').filter({ hasText: name });
        await page.goto(`${origin}/admin`);
        await page.getByRole('alert').waitFor();
        failList = false;
        await page.getByRole('button', { name: '다시 시도', exact: true }).click();
        await row('키보드').waitFor();
        assert.equal(await row('삭제된분류').count(), 0);
        assert.match(page.url(), /\/admin\/categories$/);
        const sidebar = await page.getByRole('complementary').boundingBox();
        const main = await page.getByRole('main').boundingBox();
        assert.ok(sidebar.x + sidebar.width <= main.x, '관리자 메뉴는 왼쪽에 표시한다');

        await page.getByRole('button', { name: '카테고리 생성', exact: true }).click();
        await page.getByLabel('카테고리명', { exact: true }).fill('취소할 이름');
        await page.getByRole('button', { name: '취소', exact: true }).click();
        assert.equal(writes, 0);
        assert.equal(await page.getByLabel('카테고리명', { exact: true }).count(), 0);
        await page.getByRole('button', { name: '카테고리 생성', exact: true }).click();
        await page.getByLabel('카테고리명', { exact: true }).fill('   ');
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await page.getByRole('alert').filter({ hasText: '1~255자' }).waitFor();
        assert.equal(writes, 0);
        await page.getByLabel('카테고리명', { exact: true }).fill('  모니터  ');
        failSave = true;
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await page.getByRole('alert').filter({ hasText: '저장에 실패' }).waitFor();
        assert.equal(await page.getByLabel('카테고리명', { exact: true }).inputValue(), '  모니터  ');
        failSave = false;
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await row('모니터').waitFor();
        assert.equal(categories.at(-1).categoryName, '모니터');

        const beforeCancel = writes;
        await row('모니터').getByRole('button', { name: '수정', exact: true }).click();
        await page.getByLabel('카테고리명', { exact: true }).fill('취소');
        await page.getByRole('button', { name: '취소', exact: true }).click();
        assert.equal(writes, beforeCancel);
        await row('모니터').getByRole('button', { name: '수정', exact: true }).click();
        assert.equal(await page.getByLabel('카테고리명', { exact: true }).inputValue(), '모니터');
        await page.getByLabel('카테고리명', { exact: true }).fill('키보드');
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await page.getByRole('alert').filter({ hasText: '이미 존재' }).waitFor();
        await page.getByLabel('카테고리명', { exact: true }).fill('수정된분류');
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await row('수정된분류').waitFor();
        await row('수정된분류').getByRole('button', { name: '삭제', exact: true }).click();
        await page.getByRole('dialog').getByRole('button', { name: '아니오', exact: true }).click();
        assert.equal(deletes, 0);
        await row('수정된분류').getByRole('button', { name: '삭제', exact: true }).click();
        failDelete = true;
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await page.getByRole('dialog').getByRole('alert').waitFor();
        failDelete = false;
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await row('수정된분류').waitFor({ state: 'detached' });
        await page.getByLabel('삭제된 카테고리 보기').check();
        await row('수정된분류').waitFor();
        await row('삭제된분류').waitFor();
        assert.equal(await row('수정된분류').getByRole('button', { name: '삭제', exact: true }).isDisabled(), true);
        await row('수정된분류').getByRole('button', { name: '수정', exact: true }).click();
        await page.getByLabel('카테고리명', { exact: true }).fill('삭제 후 수정');
        await page.getByRole('button', { name: '확인', exact: true }).click();
        await row('삭제 후 수정').waitFor();
        assert.equal(categories.at(-1).deleted, true);
        await page.getByLabel('삭제된 카테고리 보기').uncheck();
        await row('삭제 후 수정').waitFor({ state: 'detached' });
        await row('키보드').getByRole('button', { name: '삭제', exact: true }).click();
        await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click();
        await page.getByText('카테고리가 없습니다.', { exact: true }).waitFor();
        await page.setViewportSize({ width: 375, height: 812 });
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), '모바일에서 가로 넘침 없음');

        for (const blockedRole of ['USER', 'SELLER']) {
            role = blockedRole;
            await page.goto(`${origin}/admin/categories`);
            await page.getByRole('alert').filter({ hasText: '관리자만 이용' }).waitFor();
            assert.equal(await page.getByRole('link', { name: '관리자 페이지', exact: true }).count(), 0);
        }
        loggedIn = false;
        await page.goto(`${origin}/admin/categories`);
        await page.getByRole('heading', { name: '로그인', exact: true }).waitFor();
        assert.deepEqual(errors, []);
    } finally {
        await browser?.close(); await server.close();
    }
});
