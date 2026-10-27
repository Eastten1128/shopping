import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import axios from 'axios';

// 실제 TypeScript 모듈과 Axios interceptor를 실행한다. 외부 서버만 adapter로 대체한다.
const moduleUrl = (source) => `data:text/javascript;base64,${Buffer.from(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText).toString('base64')}`;
const tokenUrl = moduleUrl(await readFile(new URL('../src/auth/tokenStore.ts', import.meta.url), 'utf8'));
const tokenStore = await import(tokenUrl);
let refreshCount = 0;
let refreshFailure = false;
let releaseRefresh;
const user = { accessToken: 'fresh', memberId: 1, loginId: 'user', role: 'USER', tokenType: 'Bearer' };
const response = (config, data = user) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const unauthorized = (config) => new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null,
    { ...response(config), status: 401 });
axios.defaults.adapter = async (config) => {
    assert.equal(config.withCredentials, true);
    if (config.url === '/members/refresh') {
        refreshCount++;
        await new Promise((resolve) => { releaseRefresh = resolve; });
        if (refreshFailure) throw unauthorized(config);
    }
    return response(config);
};
const source = (await readFile(new URL('../src/api/axiosInstance.ts', import.meta.url), 'utf8'))
    .replace("'axios'", JSON.stringify(import.meta.resolve('axios')))
    .replace("'../auth/tokenStore'", JSON.stringify(tokenUrl));
const auth = await import(moduleUrl(source));
const client = auth.default;
const waitForRefresh = async () => {
    for (let i = 0; i < 100 && !releaseRefresh; i++) await new Promise((resolve) => setTimeout(resolve, 1));
    assert.ok(releaseRefresh, 'refresh 요청이 시작되어야 한다.');
    const release = releaseRefresh;
    releaseRefresh = undefined;
    release();
};

test('인증 API 통합: 복원, 동시 401, 실패, 재시도 제한, 로그아웃', async () => {
    let authenticated = 0;
    let cleared = 0;
    auth.setAuthHandlers(() => authenticated++, () => cleared++);
    const restore = auth.refreshAuth();
    assert.equal(restore, auth.refreshAuth());
    await waitForRefresh();
    await restore;
    assert.equal(tokenStore.getAccessToken(), 'fresh');
    assert.equal(authenticated, 1);

    tokenStore.setAccessToken('expired');
    client.defaults.adapter = async (config) => {
        assert.equal(config.withCredentials, true);
        if (config.headers.get('Authorization') !== 'Bearer fresh') throw unauthorized(config);
        return response(config);
    };
    const requests = Promise.all([client.get('/members/me'), client.get('/members/me')]);
    await waitForRefresh();
    await requests;
    assert.equal(refreshCount, 2, '동시 401은 refresh 한 번만 추가한다.');

    client.defaults.adapter = async (config) => { throw unauthorized(config); };
    await assert.rejects(client.post('/members/login'));
    assert.equal(refreshCount, 2, '로그인 실패는 재발급하지 않는다.');
    const retry = assert.rejects(client.get('/members/me'));
    await waitForRefresh();
    await retry;
    assert.equal(tokenStore.getAccessToken(), null);

    auth.acceptLogin(user);
    refreshFailure = true;
    const failed = assert.rejects(client.get('/members/me'));
    await waitForRefresh();
    await failed;
    assert.equal(tokenStore.getAccessToken(), null);
    assert.ok(cleared >= 2);

    auth.acceptLogin(user);
    refreshFailure = false;
    const pending = auth.refreshAuth();
    const rejected = assert.rejects(pending);
    const loggingOut = auth.logout();
    await waitForRefresh();
    await rejected;
    await loggingOut;
    assert.equal(tokenStore.getAccessToken(), null, '로그아웃 중 refresh가 인증을 복원하지 않는다.');
    await assert.rejects(auth.refreshAuth());
});
