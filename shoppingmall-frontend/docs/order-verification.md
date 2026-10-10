# Frontend 11단계: 주문 생성·내역·상세

## 구현과 API 계약

- `/orders/new`: 주문서, GET `/api/carts/me`의 상품명·quantity·totalPrice·totalCartPrice를 표시한다.
- 주문서는 이름, 주소1, 주소2, 3-4-4자리 전화번호, 요청사항을 받는다. 이름·주소1·전화번호는 필수, 주소2·요청사항은 선택이다.
- 결제 버튼은 POST `/api/orders`에 `{ name, phoneNumber, address1, address2, requestedTerm }`을 전달한다. 전화번호는 `010-1234-5678` 형태로 조합한다.
- 회원, 상품, 수량, 가격은 기존 Backend가 현재 장바구니로 결정한다. Frontend는 이를 주문 생성 본문에 넣지 않는다.
- 기존 OrderService가 가격·재고 확인, orders/order_products 생성, 재고 차감, 장바구니 비우기를 트랜잭션으로 처리한다.
- 주문 생성 성공 후 `/orders/{orderId}`로 이동한다. 중복 클릭은 ref와 버튼 비활성화로 제한한다.
- `/orders`: GET `/api/orders` 결과를 orderDate 내림차순으로 표시한다. 내 인증정보 화면의 내 주문 내역 버튼으로 접근한다.
- `/orders/:orderId`: GET `/api/orders/{orderId}`로 날짜·상태·상품 스냅샷·수량·단가·품목 합계·총액을 표시한다.
- 수량의 실제 주문 응답 필드는 `amount`, 요청사항은 `requestedTerm`이며 DB 컬럼은 `requested_term`이다. 기존 계약을 유지한다.
- 모든 주문 화면은 ProtectedRoute로 보호한다. 실제 결제 API 호출과 PAID 전환은 다음 결제 페이지 단계에서 구현한다.

## Backend 최소 수정

기존 OrderProduct 생성자는 orderPrice를 초기화하지 않아 DB에 품목 합계가 0으로 저장될 수 있었다. 생성 시 `price * amount`를 저장하도록 한 줄 보완하고 단위 테스트를 추가했다. Entity 매핑, BaseEntity, DB schema와 API 계약은 변경하지 않았다.

## 실행한 검증

Frontend:

```sh
npm run build
npx --no-install oxlint src tests
node --test tests/*.test.mjs
```

- 빌드 통과.
- 전체 린트 오류 없음. 이전 ProductDetailPage의 effect 내 상태 초기화 경고 1개 유지. 이번 주문 변경 파일은 별도 린트 검사에서 경고 없음.
- 인증 1개, 장바구니 3개, 주문 4개 테스트 통과.
- 주문 테스트는 생성 본문/인증 헤더, 내역 정렬, 스냅샷 표 렌더링, 409·404 오류 전달을 검증한다.

Backend:

```powershell
.\gradlew.bat test --tests com.jdr1128.shopping.domain.order.entity.OrderProductTest --tests com.jdr1128.shopping.domain.cart.service.CartServiceTest --offline
```

두 테스트 통과. 주문상품 3개 × 30,000원의 합계 90,000원이 Entity에 저장되는지 검증했다.

## 브라우저 검증 재현

```sh
node tests/order-preview.mjs normal 5191
```

메모리 API를 이용해 실제 React 화면을 실행했다. `/cart`에서 주문하기를 누른 뒤 다음을 확인했다.

- 장바구니의 키보드 2개(60,000원), 마우스 1개(15,000원), 총 75,000원 표시
- 필수 입력 누락과 3-4-4자리 전화번호 형식 오류의 제출 제한
- 요청사항에 `가격변경테스트` 입력 시 409 오류 표시와 입력 유지
- 요청사항을 수정하여 제출하면 주문 상세로 이동, 두 품목과 총 75,000원 표시
- 내 인증정보 → 내 주문 내역 → 개별 주문 상세 이동
- 주문날짜 10월 10일, 10월 8일, 9월 20일 순서로 표시
- 생성 후 빈 장바구니와 주문하기 비활성화
- 빈 장바구니로 주문서 직접 접근 시 제출 차단
- 주문 화면에서 로그아웃 시 로그인으로 이동

빈 상태는 `empty`, 비로그인은 `guest`, 조회 실패는 `error` 시나리오로 재현할 수 있다. 실제 MySQL 및 인증 쿠키를 연결한 주문 전체 통합 검증은 실행하지 않았다.

## 다음 단계

실제 서버에서 회원별 주문 접근 제한, 생성된 주문상품 합계와 재고 차감·장바구니 삭제를 확인한다. 이후 결제 페이지와 모의 결제 API를 연결한다.
