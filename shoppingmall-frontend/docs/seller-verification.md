# SELLER 상품 관리

## 구현과 설계

- `/seller/products`: 본인 상품명, 가격, 재고, 상태, 수정/판매중지 버튼. 기본적으로 삭제되지 않은 상품만 표시하고 체크박스로 판매중지 행도 조회한다.
- `/seller/products/new`: 상품명, 카테고리 select, 가격, 재고 등록.
- `/seller/products/:productId/edit`: 본인 상품 정보 수정 및 판매중지.
- 로그인 복원은 기존 ProtectedRoute를 사용하며 SellerRoute와 메뉴는 SELLER에게만 제공한다. 서버에서도 SELLER와 인증 회원 ID를 검증해 타인의 상품 조회·수정을 차단한다.
- 판매중지는 공통 native dialog의 `예`를 눌렀을 때만 기존 DELETE API를 호출한다. 기존 Product.delete()가 `is_deleted=true`, `product_status=STOPPED`, `deleted_at`을 함께 설정한다. 실패 시 재시도할 수 있다.
- 판매중지 상품도 정보를 수정할 수 있지만 STOPPED와 삭제 상태를 유지한다. 판매 재개 기능은 추가하지 않았다.
- 가격과 재고는 기존 INT 범위의 0 이상 정수로 입력한다. 재고 0은 SOLD_OUT, 양수는 SELLING으로 전송한다. 판매중지 상품은 STOPPED를 유지한다.
- Entity, BaseEntity, DB 컬럼과 기존 공개 조회·등록·수정·삭제 API는 유지한다. 새 라이브러리를 프로젝트 의존성에 추가하지 않았다.

기존 API만으로는 삭제된 본인 상품을 조회할 수 없어 다음 경로를 추가했다. sellerId는 요청 본문이나 쿼리에서 받지 않고 인증 정보에서 가져온다.

| Method | API | 용도 |
|---|---|---|
| GET | `/api/seller/products?includeStopped=false` | 본인 판매상품 목록 |
| GET | `/api/seller/products/{productId}` | 본인 상품 수정 정보, 중지상품 포함 |
| PATCH | `/api/seller/products/{productId}` | 본인 상품 수정, 중지상태 유지 |
| POST | `/api/products` | 기존 등록 API 사용 |
| DELETE | `/api/products/{productId}` | 기존 소프트 삭제 API 사용 |

ProductResponse에 `deleted` boolean을 추가해 DB 삭제 상태를 명시한다. 다른 응답 필드는 유지한다.

## 검증 결과 (2026-10-14)

프론트엔드:

```powershell
npm.cmd run build
npm.cmd run lint -- src
node --test tests/auth.test.mjs
$env:PLAYWRIGHT_MODULE_PATH = '<Playwright 설치 폴더>/index.mjs'
node --test tests/seller.test.mjs
```

- TypeScript 및 Vite 프로덕션 빌드 성공.
- src lint 오류 없음, set-state-in-effect 경고 3개(판매자 화면 2개와 복원한 상품상세 화면 1개).
- 인증 회귀 테스트 1개 통과.
- 실제 Headless Edge에서 브라우저 테스트 1개 통과: 등록, 음수 입력 차단, 저장 실패 후 입력 유지·재시도, 수정, 재고 0, 판매중지 취소·확인·실패 재시도, 목록/수정화면 양쪽 판매중지, 중지상품 필터와 수정, 빈 목록, 타인 상품 수정화면 거부, USER/ADMIN/비로그인 차단. 페이지 JavaScript 오류 없음.
- 브라우저 테스트는 실제 React 화면과 Axios를 실행하고 HTTP API만 mock으로 대체한다. Playwright는 도구의 기존 설치를 사용했으며 프로젝트 의존성에 추가하지 않았다. 실행에는 Edge가 필요하다.

백엔드:

```powershell
.\gradlew.bat -I docs/seller-tests.init.gradle test --tests '*SellerProduct*Test' --tests '*ProductServiceTest' bootJar --offline
```

- 신규 서비스 테스트 8개, 신규 H2 저장소 테스트 2개, 기존 상품 서비스 테스트 2개 통과.
- H2에서 판매자별 조회 분리, 삭제행 포함 여부, 타인 상세 조회 제외, 실제 소프트 삭제 저장과 STOPPED·삭제시각 유지 검증.
- Java 컴파일 및 bootJar 패키징 성공.
- 일반 test 실행은 기존 MemberServiceTest.java:134의 LoginResult/LoginResponse 반환 타입 불일치로 테스트 컴파일 실패. 해당 테스트를 변경하지 않고 init script로 상품 테스트 소스만 컴파일했다. 전체 테스트 통과를 의미하지 않는다.
- 기본 npm lint는 node_modules까지 검사해 실패하므로 위의 src 범위 lint 결과를 사용했다. lint 설정은 변경하지 않았다.

## 기존 작업 트리와 커밋 범위

작업 시작 브랜치는 feature/backend-jdr이며 기존 staged 변경이 다수 있었다. 누락된 선행 파일 때문에 빌드가 불가능해 이전 작업 커밋에서 다음 10개 파일을 작업 트리에 복원했다. 이 파일들은 이번 판매자 기능 커밋에서 제외했다.

- b90562e: `src/pages/HomePage.tsx`, `src/pages/ProductDetailPage.tsx`, `src/api/cartApi.ts`, `src/api/cartItemApi.ts`, `src/types/cart.ts` (frontend).
- 7731520: `domain/auth/entity/RefreshToken.java`, `domain/auth/repository/RefreshTokenRepository.java`, `domain/auth/service/RefreshTokenService.java`, `domain/auth/service/RefreshTokenCleanupScheduler.java`, `global/security/JwtTokenProvider.java` (backend main Java).

JwtTokenProvider는 교체 전 HEAD 대비 사용자 수정이 없는 것을 확인했다. 다른 복원 대상은 파일이 없는 것을 확인했다. 기존 사용자 변경은 보존했으며 application.properties 등의 설정은 커밋하지 않았다. 이미 staged였던 상품 API/타입 및 ProductService/ProductResponse는 이번 작업과 직접 관련된 파일이므로 기존 구현을 포함해 커밋한다.

빌드와 테스트는 이 선행 파일 및 기존 미커밋 변경을 포함한 작업 트리 기준이다. 이 커밋만 별도 checkout한 상태의 빌드나 실제 MySQL·실서버 API 통합 검증은 수행하지 않았다.

## 다음 단계

선행 인증·프론트엔드 파일을 현재 브랜치에 정식 반영하고 기존 MemberServiceTest 반환 타입을 맞춘 뒤 전체 테스트를 실행한다. 이후 실제 SELLER 계정 두 개로 상품 등록·수정·판매중지 및 소유권 차단을 확인한다.
