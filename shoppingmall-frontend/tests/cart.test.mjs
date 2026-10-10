import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import axios from 'axios';

const moduleUrl = (source) => `data:text/javascript;base64,${Buffer.from(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText).toString('base64')}`;
const tokenUrl = moduleUrl(await readFile(new URL('../src/auth/tokenStore.ts', import.meta.url), 'utf8'));
const tokenStore = await import(tokenUrl);
const clientUrl = moduleUrl((await readFile(new URL('../src/api/axiosInstance.ts', import.meta.url), 'utf8'))
    .replace("'axios'", JSON.stringify(import.meta.resolve('axios')))
    .replace("'../auth/tokenStore'", JSON.stringify(tokenUrl)));
const { default: client } = await import(clientUrl);
const loadApi = async (name) => import(moduleUrl(
    (await readFile(new URL(`../src/api/${name}.ts`, import.meta.url), 'utf8'))
        .replace("'./axiosInstance'", JSON.stringify(clientUrl)),
));
const { getMyCart } = await loadApi('cartApi');
const { addCartItem } = await loadApi('cartItemApi');
const response = (config, data, status = 200) => ({ config, data, status, statusText: 'OK', headers: {} });

test('장바구니 조회는 인증 헤더를 보내고 서버의 스냅샷 가격과 총액을 유지한다.', async () => {
    tokenStore.setAccessToken('cart-access');
    const cart = { cartId: 7, memberId: 1, items: [
        { productId: 2, quantity: 2, cartPrice: 10000, currentPrice: 12000, totalPrice: 20000 },
    ], totalCartPrice: 20000 };
    client.defaults.adapter = async (config) => {
        assert.equal(config.url, '/carts/me');
        assert.equal(config.method, 'get');
        assert.equal(config.withCredentials, true);
        assert.equal(config.headers.get('Authorization'), 'Bearer cart-access');
        return response(config, cart);
    };
    assert.deepEqual(await getMyCart(), cart);
});

test('담기는 상품 ID·수량만 전송하고 서버의 기존 장바구니 합산 결과를 반환한다.', async () => {
    const item = { cartItemId: 9, productId: 2, quantity: 3, cartPrice: 10000, totalPrice: 30000 };
    client.defaults.adapter = async (config) => {
        assert.equal(config.url, '/carts/items');
        assert.equal(config.method, 'post');
        assert.deepEqual(JSON.parse(config.data), { productId: 2, quantity: 1 });
        return response(config, item, 201);
    };
    assert.deepEqual(await addCartItem({ productId: 2, quantity: 1 }), item);
});

test('빈 장바구니와 서버 재고 오류를 그대로 호출자에게 전달한다.', async () => {
    client.defaults.adapter = async (config) => response(config, { cartId: 7, memberId: 1, items: [], totalCartPrice: 0 });
    assert.equal((await getMyCart()).items.length, 0);
    client.defaults.adapter = async (config) => {
        throw new axios.AxiosError('Conflict', 'ERR_BAD_REQUEST', config, null,
            response(config, { message: '상품 재고가 부족합니다.' }, 409));
    };
    await assert.rejects(addCartItem({ productId: 2, quantity: 4 }), (error) =>
        error.response.status === 409 && error.response.data.message === '상품 재고가 부족합니다.');
});
