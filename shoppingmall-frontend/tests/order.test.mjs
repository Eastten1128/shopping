import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import axios from 'axios';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const moduleUrl = (source, jsx = false) => `data:text/javascript;base64,${Buffer.from(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX },
    fileName: jsx ? 'component.tsx' : 'module.ts',
}).outputText.replace('"react/jsx-runtime"', JSON.stringify(import.meta.resolve('react/jsx-runtime')))).toString('base64')}`;
const tokenUrl = moduleUrl(await readFile(new URL('../src/auth/tokenStore.ts', import.meta.url), 'utf8'));
const tokenStore = await import(tokenUrl);
const clientUrl = moduleUrl((await readFile(new URL('../src/api/axiosInstance.ts', import.meta.url), 'utf8'))
    .replace("'axios'", JSON.stringify(import.meta.resolve('axios')))
    .replace("'../auth/tokenStore'", JSON.stringify(tokenUrl)));
const { default: client } = await import(clientUrl);
const api = await import(moduleUrl((await readFile(new URL('../src/api/orderApi.ts', import.meta.url), 'utf8'))
    .replace("'./axiosInstance'", JSON.stringify(clientUrl))));
const { default: OrderProductsTable } = await import(moduleUrl(
    await readFile(new URL('../src/components/OrderProductsTable.tsx', import.meta.url), 'utf8'), true));
const response = (config, data, status = 200) => ({ config, data, status, statusText: 'OK', headers: {} });
const snapshot = { orderProductId: 1, productId: 7, productName: '주문 당시 상품명', amount: 3, price: 30000, orderPrice: 90000 };

test('주문 생성은 배송 정보만 전송하고 상품·총액·회원 정보는 서버가 결정한다.', async () => {
    tokenStore.setAccessToken('order-access');
    const request = { name: '테스트', phoneNumber: '010-1234-5678', address1: '테스트 주소', address2: '', requestedTerm: '문 앞' };
    client.defaults.adapter = async (config) => {
        assert.equal(config.method, 'post');
        assert.equal(config.url, '/orders');
        assert.equal(config.withCredentials, true);
        assert.equal(config.headers.get('Authorization'), 'Bearer order-access');
        assert.deepEqual(JSON.parse(config.data), request);
        return response(config, { orderId: 10, orderProducts: [snapshot], totalOrderPrice: 90000 }, 201);
    };
    assert.equal((await api.createOrder(request)).orderId, 10);
});

test('주문 내역은 날짜 내림차순이며 서버 배열을 변경하지 않는다.', async () => {
    const orders = [
        { orderId: 1, orderDate: '2026-09-10T12:00:00' },
        { orderId: 2, orderDate: '2026-10-10T12:00:00' },
        { orderId: 3, orderDate: '2026-10-08T12:00:00' },
    ];
    client.defaults.adapter = async (config) => {
        assert.equal(config.url, '/orders');
        assert.equal(config.method, 'get');
        return response(config, orders);
    };
    assert.deepEqual((await api.getMyOrders()).map((order) => order.orderId), [2, 3, 1]);
    assert.deepEqual(orders.map((order) => order.orderId), [1, 2, 3]);
});

test('주문 상세는 기존 상품 조회 없이 주문 스냅샷을 표시한다.', async () => {
    client.defaults.adapter = async (config) => {
        assert.equal(config.url, '/orders/10');
        return response(config, { orderId: 10, orderProducts: [snapshot] });
    };
    const order = await api.getMyOrder(10);
    const html = renderToStaticMarkup(React.createElement(OrderProductsTable, { items: order.orderProducts }));
    assert.match(html, /주문 당시 상품명/);
    assert.match(html, /<td>3<\/td>/);
    assert.match(html, /30,000/);
    assert.match(html, /90,000/);
});

test('주문 충돌·상세 조회 실패는 재시도 없이 호출자에게 전달한다.', async () => {
    let requests = 0;
    client.defaults.adapter = async (config) => {
        requests++;
        const status = config.method === 'post' ? 409 : 404;
        throw new axios.AxiosError('Failure', 'ERR_BAD_REQUEST', config, null,
            response(config, { message: '주문 테스트 오류' }, status));
    };
    await assert.rejects(api.createOrder({}), (error) => error.response.status === 409);
    await assert.rejects(api.getMyOrder(999), (error) => error.response.status === 404);
    assert.equal(requests, 2);
});
