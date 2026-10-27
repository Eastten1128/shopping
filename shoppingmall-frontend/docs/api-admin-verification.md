# 상품 API 검사 및 ADMIN 페이지 복원

## 점검 결과와 구현

작업 브랜치는 feature/frontend-jdr이다. 루트 AGENTS.md와 README.md가 없어 기존 1309d60 커밋의 문서를 읽었고, 현재 frontend README와 백엔드 Entity/DTO를 함께 확인했다.

- ProductWriteRequest와 SellerProduct는 이미 선언되어 있었다. ProductWriteRequest의 categoryId, productName, price, stock, productStatus는 Backend 생성/수정 DTO와 일치하므로 요청 구조를 바꾸지 않았다. sellerId는 인증 사용자에서 결정하므로 요청에 넣지 않는다.
- SellerProduct의 sellerLoginId, createdAt, updatedAt 응답 필드를 보완했다. updatedAt은 null을 허용한다.
- 현재 브랜치에 /api/seller/products 조회·상세·수정 경로와 deleted 응답이 없었다. 기존 211050d의 관련 Controller/Service/Repository 구현을 반영해 실제 API와 타입을 맞췄다. 판매자는 본인 상품만 조회·수정하며, 삭제된 상품 수정 시 STOPPED와 삭제 상태를 유지한다.
- ProductResponse에서 productName과 sellerLoginId가 뒤바뀌어 전달되는 버그를 수정했다.
- getProducts가 보내는 categoryId를 서버가 무시하던 문제를 수정했다. 미지정 시 기존 전체 활성 상품 목록을 반환한다.
- ADMIN 메뉴·페이지·Route가 누락되어 있었다. 기존 85dc57c의 관리자 화면 및 필요한 카테고리 API를 현재 브랜치에 연결했다. 기존 장바구니·주문 라우트는 유지했다.
- 관리자 페이지는 좌측 카테고리 메뉴, 생성·수정 확인/취소, 삭제 확인, 삭제된 목록 필터, 실패 재시도와 반응형 배치를 제공한다.
- /api/admin/**는 서버에서도 ADMIN으로 제한한다. 삭제된 카테고리 이름을 수정해도 삭제 상태와 시각을 유지한다.

## 인증 연결이 함께 필요한 이유

기존 브랜치의 일부 인증 파일이 빈 클래스이거나 현재 JWT 인터페이스와 맞지 않아 백엔드가 컴파일되지 않았다. 이 상태에서는 ADMIN API를 사용할 수 없어, 필요한 연결만 기존 1309d60 구현에서 복원했다.

- LoginResult, RefreshTokenCookieProvider, TokenType와 Refresh Token 예외/오류 코드 복원.
- 로그인에서 memberId가 포함된 Access Token과 HttpOnly Refresh Cookie 발급.
- /members/refresh와 /members/logout 연결 및 인증 없이 쿠키로 호출할 수 있는 보안 설정.
- JWT 필터는 validateAccessToken을 사용해 Refresh Token을 Authorization으로 받지 않는다.
- 기존 jwt.expiration-milliseconds 설정을 Access Token의 fallback으로 지원한다. Refresh 유효기간과 쿠키 설정은 새 설정이 없으면 로컬 개발용 기본값(7일, refreshToken, /api/members, HttpOnly, SameSite=Lax, Secure=false)을 사용한다. 기존 설정값이 있으면 그 값을 우선한다.
- 기존 Refresh Token 하드 삭제 서비스는 유지했다.

Entity/BaseEntity 및 DB schema는 수정하지 않았다. 설정 파일과 비밀값을 변경하거나 커밋하지 않았다. 다른 브랜치 전체를 병합하지 않고 요청 기능과 그 기능을 실행하는 데 필요한 파일만 반영했다.

## 검증 (2026-10-16)

```powershell
# frontend
npm.cmd run build
npm.cmd run lint -- src
$env:PLAYWRIGHT_MODULE_PATH = '<기존 Playwright 설치 폴더>/index.mjs'
node --test --test-concurrency=1 tests/auth.test.mjs tests/cart.test.mjs tests/order.test.mjs tests/product-api.test.mjs tests/admin-category.test.mjs

# backend
.\gradlew.bat test --tests '*ApiContractIntegrationTest' --tests '*AdminCategoryIntegrationTest' --tests '*CategoryServiceTest' --tests '*SellerProduct*Test' --tests '*ProductServiceTest' --tests '*RefreshTokenServiceTest' --tests '*JwtTokenProviderTest' bootJar --offline
```

- frontend TypeScript/Vite 빌드 성공. src lint 오류 없음, 기존 ProductDetailPage 경고 1개만 남음.
- frontend 테스트 10개 통과: 인증, 장바구니, 주문, 상품 API 요청 경로·필드·헤더·쿠키, ADMIN 브라우저 흐름.
- ADMIN 화면 테스트는 실제 Headless Edge와 React를 사용한다. HTTP 응답만 mock이며 생성·수정·삭제·취소·실패 재시도·삭제 목록·권한 차단·모바일 배치를 검사한다. 신규 라이브러리를 프로젝트에 설치하지 않았다.
- backend 테스트 34개 통과, bootJar 패키징 성공. 별도 테스트 소스 필터 없이 기본 Gradle 컴파일을 사용했다.
- 새 ApiContractIntegrationTest는 실제 Spring Security, JWT, HttpOnly Cookie와 H2를 사용한다. 상품명/판매자명 응답 매핑, 판매자 등록·수정·삭제·타인 접근 차단, 카테고리 필터, ADMIN 로그인·재발급·관리자 접근·로그아웃과 이전 토큰 거부를 검증한다.
- 기존 카테고리/판매자/Refresh Token/JWT 테스트도 함께 실행했다.
- 전체 테스트 실행 및 실제 MySQL 환경 검증은 수행하지 않았다.

## 변경 파일

- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/auth/exception/InvalidRefreshTokenException.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/auth/exception/RefreshTokenNotFoundException.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/auth/exception/RevokedRefreshTokenException.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/category/controller/AdminCategoryController.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/category/dto/CategoryResponse.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/category/service/CategoryService.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/member/controller/MemberController.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/member/dto/LoginResult.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/member/service/MemberService.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/product/controller/ProductController.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/product/controller/SellerProductController.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/product/dto/ProductResponse.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/product/repository/ProductRepository.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/domain/product/service/ProductService.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/exception/ErrorCode.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/JwtAuthenticationFilter.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/JwtTokenProvider.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/RefreshTokenCookieProvider.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/SecurityConfig.java`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/TokenType.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/domain/category/controller/AdminCategoryIntegrationTest.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/domain/product/repository/SellerProductRepositoryTest.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/domain/product/service/SellerProductServiceTest.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/global/security/ApiContractIntegrationTest.java`
- `shoppingmall-frontend/docs/api-admin-verification.md`
- `shoppingmall-frontend/src/App.tsx`
- `shoppingmall-frontend/src/api/categoryApi.ts`
- `shoppingmall-frontend/src/auth/AdminRoute.tsx`
- `shoppingmall-frontend/src/components/DeleteCategoryDialog.tsx`
- `shoppingmall-frontend/src/pages/AdminCategoryPage.tsx`
- `shoppingmall-frontend/src/pages/AdminLayout.tsx`
- `shoppingmall-frontend/src/pages/admin.css`
- `shoppingmall-frontend/src/types/category.ts`
- `shoppingmall-frontend/src/types/product.ts`
- `shoppingmall-frontend/tests/admin-category.test.mjs`
- `shoppingmall-frontend/tests/product-api.test.mjs`

## 다음 단계

실제 MySQL 및 ADMIN/SELLER 계정으로 기능을 확인한다. 브랜치 전환 시 기능이 다시 누락되지 않도록 frontend/backend 브랜치의 공통 기반을 정리한다.
