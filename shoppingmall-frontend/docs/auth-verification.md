# Frontend 6.5 인증 확인

Access Token은 tokenStore 메모리에만 저장한다. Refresh Token은 서버가 발급하는 HttpOnly Cookie로 브라우저가 관리한다. Entity, BaseEntity, DB schema는 변경하지 않는다.

## 자동 검증

```sh
npm run build
npm run lint
node --test tests/auth.test.mjs
```

Axios adapter 기반 통합 테스트는 초기 복원, refreshPromise 공유, Authorization 자동 추가, 동시 401 재시도, 로그인 실패 제외, 재시도 제한, 재발급 실패와 로그아웃 경합을 검증한다. 실제 브라우저의 쿠키 저장 정책이나 DB 연동을 대신하지 않는다.

## 실제 서버에서 확인할 항목

1. Backend와 `npm run dev`를 실행한다.
2. 로그인 응답 본문에 accessToken이 있고 Set-Cookie에 HttpOnly refreshToken이 있는지 확인한다. localStorage/sessionStorage에 토큰이 없어야 한다.
3. 로그인 상태에서 F5를 누르면 POST /api/members/refresh 이후 사용자 표시가 복원되는지 확인한다.
4. 만료된 Access Token으로 인증 API를 동시에 호출하면 refresh 요청 하나 이후 새 Authorization으로 재시도되는지 확인한다.
5. Refresh Cookie를 삭제하고 F5를 누르면 비로그인 메뉴로 전환되는지 확인한다.
6. 비로그인 상태로 /account에 접근하면 /login으로 이동하고 로그인 후 원래 경로로 돌아오는지 확인한다.
7. 로그아웃 시 POST /api/members/logout, 쿠키 삭제, 메뉴 전환을 확인한다. F5 후에도 비로그인이어야 한다.
8. 로그아웃 API 실패 시 오류와 재시도 버튼이 표시되는지 확인한다.

`/account`는 기존 HomePage의 인증 정보를 보호 라우트로 재사용한다. 마이페이지 API나 별도 회원 기능은 추가하지 않는다.
