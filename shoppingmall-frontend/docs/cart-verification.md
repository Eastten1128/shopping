# Frontend 10단계: 장바구니 페이지

## API와 설계

- `cartApi.getMyCart`: GET `/api/carts/me`
- `cartItemApi.addCartItem`: POST `/api/carts/items`, 본문 `{ productId, quantity }`
- 로그인한 회원은 기존 Axios 인증 헤더로 식별한다. memberId, cartId, 가격을 Frontend에서 지정하지 않는다.
- 기존 `CartService.findOrCreateCart`가 회원의 장바구니를 재사용하거나 생성하고, 같은 상품은 수량을 합산한다.
- `currentPrice`는 현재 단가, `cartPrice`는 담을 당시 단가, `totalPrice`는 품목 합계, `totalCartPrice`는 장바구니 총액이다. 조회 화면은 서버의 계산 결과를 사용한다.
- Entity, BaseEntity, DB schema는 변경하지 않는다. 수량 수정·삭제·주문 생성은 이번 단계에 추가하지 않는다.
- `/cart`는 ProtectedRoute로 보호한다. 비로그인 사용자의 담기 클릭은 로그인으로 안내하고 상품 상세로 복귀시킨다. 로그인 후 자동으로 담지는 않는다.

## 실행한 자동 검증

Frontend 디렉터리:

```sh
npm run build
npx --no-install oxlint src tests
node --test tests/*.test.mjs
```

빌드 통과. 린트 오류 없음, 상품 상세의 effect 내 상태 초기화에 대한 경고 1개 있음. 기존 인증 테스트 1개와 장바구니 API 테스트 3개 통과.

Backend 디렉터리:

```powershell
.\gradlew.bat test --tests com.jdr1128.shopping.domain.cart.service.CartServiceTest --offline
```

기존 장바구니 서비스 테스트 통과. 같은 상품 재담기 시 수량 합산과 스냅샷 가격 기준 합계를 검증한다.

## 브라우저 검증

테스트 전용 메모리 API로 실제 React 화면을 실행했다. 실제 Backend/DB에는 연결하지 않는다.

```sh
node tests/cart-preview.mjs normal 5189
```

`http://127.0.0.1:5189/cart`에서 다음 항목을 확인했다.

- 빈 장바구니 문구와 총액 0원
- 상품 수량 초기값 1 및 감소 버튼 비활성화
- 수량 2에서 60,000원, 재고 상한 3에서 90,000원과 증가 버튼 비활성화
- 담기 성공 메시지 및 상단 장바구니 버튼 이동
- 기존 상품에 1개 추가 후 한 행에 수량 3, 합계 90,000원
- 현재 단가 31,000원과 담을 당시 단가 30,000원을 구분하여 표시
- 재고 초과 시 서버 오류 문구 표시
- 품절 상품의 증가·담기 버튼 비활성화
- 로그아웃 후 장바구니 접근 시 로그인 이동, 테스트 로그인 후 `/cart` 복귀

조회 실패 화면은 `node tests/cart-preview.mjs error 5190`으로 재현할 수 있다. 비로그인 초기 상태는 `guest` 시나리오로 재현할 수 있다.

## 실제 서버에서 추가 확인

Backend와 일반 `npm run dev`를 실행한 뒤, 회원 A의 첫 담기에서 Cart가 생성되는지, 다음 담기에서 같은 Cart가 재사용되는지, 회원 B의 Cart와 분리되는지 DB와 함께 확인한다. 실제 MySQL을 연결한 전체 브라우저 통합 검증은 이번 작업에서 실행하지 않았다.
