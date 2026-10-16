import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';

const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText).toString('base64')}`;
const tokenUrl = moduleUrl(await readFile(new URL('../src/auth/tokenStore.ts', import.meta.url), 'utf8'));
const tokens = await import(tokenUrl);
const clientUrl = moduleUrl((await readFile(new URL('../src/api/axiosInstance.ts', import.meta.url), 'utf8'))
    .replace("'axios'", JSON.stringify(import.meta.resolve('axios')))
    .replace("'../auth/tokenStore'", JSON.stringify(tokenUrl)));
const { default: client } = await import(clientUrl);
const api = await import(moduleUrl((await readFile(new URL('../src/api/productApi.ts', import.meta.url), 'utf8'))
    .replace("'./axiosInstance'", JSON.stringify(clientUrl))));

test('상품 API는 백엔드와 일치하는 요청 필드·경로·인증을 사용한다', async () => {
    tokens.setAccessToken('seller-access');
    const calls = [];
    const request = { productName: '상품', categoryId: 3, price: 10000, stock: 5, productStatus: 'SELLING' };
    const product = { ...request, productId: 7, sellerId: 1, sellerLoginId: 'seller', deleted: false, createdAt: '2026-10-16T12:00:00', updatedAt: null };
    client.defaults.adapter = async config => {
        assert.equal(config.headers.get('Authorization'), 'Bearer seller-access');
        assert.equal(config.withCredentials, true);
        calls.push(config);
        return { config, data: product, status: 200, statusText: 'OK', headers: {} };
    };
    assert.deepEqual(await api.createProduct(request), product);
    assert.deepEqual(JSON.parse(calls.at(-1).data), request);
    assert.equal(calls.at(-1).url, '/products');
    await api.updateSellerProduct(7, request);
    assert.equal(calls.at(-1).method, 'patch');
    assert.equal(calls.at(-1).url, '/seller/products/7');
    assert.deepEqual(JSON.parse(calls.at(-1).data), request);
    await api.getSellerProducts(true);
    assert.deepEqual(calls.at(-1).params, { includeStopped: true });
    await api.getSellerProduct(7);
    assert.equal(calls.at(-1).url, '/seller/products/7');
    await api.stopProduct(7);
    assert.equal(calls.at(-1).method, 'delete');
    assert.equal(calls.at(-1).url, '/products/7');
    await api.getProducts(3);
    assert.deepEqual(calls.at(-1).params, { categoryId: 3 });
});
