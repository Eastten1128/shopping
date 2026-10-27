# 환경 설정 및 브랜치 통합

## 통합 기준

- 기준: `feature/frontend-jdr`의 `0c35987`.
- 병합 대상: `feature/backend-jdr`의 `85dc57c`.
- 프론트엔드 브랜치에서 backend를 병합하고 충돌 구간은 frontend 구현을 우선했다. 충돌하지 않는 SELLER 기능과 CORS 설정 등은 보존했다.
- 자동 병합으로 중복된 ADMIN/로그아웃 보안 규칙을 정리하고, ADMIN·장바구니·주문 라우트를 유지하면서 SELLER 메뉴와 라우트를 연결했다.
- main의 `22ef896`은 두 기능 브랜치와 공통 조상이 없는 별도 이력이다. 이력을 지우거나 재작성하지 않고 통합 브랜치를 main에 병합한다. main 병합에서도 통합 구현을 우선한다.
- main의 초기 JS용 `vite.config.js`는 통합된 TypeScript `vite.config.ts`와 충돌하는 설정이므로 제거한다. 실제 앱의 `/api` 프록시를 가진 TS 설정을 사용한다.
- 원격 push, force push, 이력 재작성, DB schema 변경은 수행하지 않는다.

## .env 사용

실제 DB/JWT/쿠키/프론트엔드 origin 설정을 루트 `.env`로 이동했다. `.env`는 로컬에만 있으며 `.env.example`에는 비밀번호와 JWT 키를 비워두었다. 새 환경에서는 예제 파일을 복사한 뒤 값을 채운다.

```powershell
Copy-Item .env.example .env
# .env에서 DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET 등을 설정
cd shoppingmall-backend
.\gradlew.bat bootRun
```

- `.env`에는 Java properties 형식의 `KEY=value`를 작성한다. 값 주위에 따옴표를 붙이지 않는다. 역슬래시와 줄바꿈 등은 Java properties 규칙대로 이스케이프한다.
- `spring.config.import`의 `.env[.properties]` 확장자 힌트를 사용한다. 루트에서 실행하면 `./.env`, backend에서 실행하면 `../.env`를 읽는다. 별도 dotenv 라이브러리와 터미널 export 작업은 필요 없다.
- 운영 환경에서는 같은 이름의 OS 환경변수를 사용할 수 있다. JWT 키는 충분히 긴 무작위 값으로 설정하고 HTTPS 배포 시 `JWT_REFRESH_COOKIE_SECURE=true`를 지정한다.
- 비밀값을 Vite의 `VITE_` 변수로 만들지 않는다. 프론트엔드는 기존 `/api` 프록시를 사용한다.
- 실제 DB 설정과 JWT 키에는 안전하지 않은 하드코딩 기본값을 두지 않았다. 설정 누락은 서버 시작 시 드러난다.
- `.env`, `.env.*`는 모든 경로에서 무시하며 `.env.example` 등 예제만 추적한다.

주요 변수는 `.env.example`에 정의되어 있다. DB 접속 4개, DDL 정책, JWT 키·유효기간·쿠키 설정, 프론트엔드 origin과 SQL 로그 옵션을 포함한다.

## 테스트 및 검증

```powershell
# root
node tools/verify-env.mjs

# backend: 전체 테스트는 자동으로 test 프로필 사용
cd shoppingmall-backend
.\gradlew.bat test bootJar --offline

# frontend
cd ../shoppingmall-frontend
npm.cmd run build
npm.cmd run lint -- src
$env:PLAYWRIGHT_MODULE_PATH = '<기존 Playwright 설치 폴더>/index.mjs'
node --test --test-concurrency=1 tests/auth.test.mjs tests/cart.test.mjs tests/order.test.mjs tests/product-api.test.mjs tests/admin-category.test.mjs tests/seller.test.mjs
```

2026-10-27 검증:

- 전체 백엔드 테스트 **50개 통과**, bootJar 빌드 성공.
- `EnvironmentConfigurationTest`는 추가 라이브러리 없이 `.env`를 읽는 것과, `.env` 없는 test 환경에서 H2·테스트 키를 사용하는 것을 검증한다.
- 기존 application-test.properties에 있던 실제 DB/키를 제거했다. Gradle test는 test 프로필을 지정하며 운영 MySQL에 접속하지 않는다. 테스트의 H2는 DB schema 변경 없이 격리된 임시 DB다.
- 실제 설정의 JWT 키가 재사용되던 단위 테스트를 공개 테스트 전용 fixture로 변경했다.
- 프론트엔드 테스트 **11개 통과**. 인증·장바구니·주문·상품 API와 ADMIN/SELLER 브라우저 흐름을 확인했다. 브라우저는 실제 Headless Edge, HTTP API 응답만 mock이다.
- 프론트엔드 빌드 성공. src lint 오류 없음, 기존 set-state-in-effect 경고 3개 유지.
- 환경변수 참조, ignore 규칙, 추적된 .env 여부, 고신뢰 credential 패턴과 로컬 JWT 키 복사 여부를 값 출력 없이 검사했다.
- 실제 MySQL 데이터 변경 및 운영 API에 대한 쓰기 검증은 하지 않았다.

## 주요 수정 파일

- `.env` (로컬 전용), `.env.example`, 루트 `.gitignore`
- `shoppingmall-backend/src/main/resources/application.properties`
- `shoppingmall-backend/src/main/resources/application-test.properties`
- `shoppingmall-backend/build.gradle`
- `shoppingmall-backend/src/main/java/com/jdr1128/shopping/global/security/SecurityConfig.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/global/security/JwtTokenProviderTest.java`
- `shoppingmall-backend/src/test/java/com/jdr1128/shopping/global/security/EnvironmentConfigurationTest.java`
- `shoppingmall-frontend/src/App.tsx`, main의 초기 `shoppingmall-frontend/vite.config.js` 제거
- `tools/verify-env.mjs`, `README.md`, 이 문서

위 수정 외에 backend 브랜치의 기존 파일들은 Git 병합 이력으로 함께 보존된다. Entity와 BaseEntity 구현은 frontend 브랜치 기준을 유지했다.

## 과거 이력의 비밀값

현재 파일에서 비밀값을 제거해도 과거 Git 커밋에는 값이 남는다. 이번 작업은 이력을 재작성하지 않는다. 기존 DB 비밀번호와 JWT 키를 교체한 뒤 새 값을 로컬 `.env`에 반영해야 과거 값의 악용을 막을 수 있다. 교체 및 원격 반영은 별도 운영 작업이다.
