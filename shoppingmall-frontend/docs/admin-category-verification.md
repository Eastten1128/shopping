# ADMIN 카테고리 관리

## 구현

- `/admin` 접속 시 `/admin/categories`로 이동한다. ADMIN에게만 상단 관리자 링크와 관리자 화면을 제공한다. 좌측 메뉴는 현재 카테고리 한 항목만 세로 배치한다.
- 카테고리 목록은 ID 순으로 일정한 행에 표시한다. 우측 상단 체크박스로 삭제된 카테고리도 조회한다.
- 생성/수정 버튼으로 입력 영역을 열고 확인 시 저장한다. 취소하면 입력을 폐기하고 영역을 닫는다. 이름은 앞뒤 공백을 제거하고 1~255자로 제한한다.
- 삭제 버튼은 native dialog로 확인한다. 예를 눌렀을 때만 기존 DELETE API를 호출하며 아니오/Escape는 요청을 보내지 않는다. 진행 중 중복 요청과 취소를 제한한다.
- 저장·삭제 실패 시 입력과 확인창을 유지해 다시 시도할 수 있다. 중복 이름은 서버 메시지를 표시한다. 목록 실패 시 재조회할 수 있다.
- 삭제된 행에는 상태를 표시하고 반복 삭제를 차단한다. 삭제된 이름을 수정해도 삭제 여부와 삭제 시각은 유지한다.
- 모바일에서는 메뉴와 목록을 세로로 배치한다. 다른 관리 대상 메뉴나 복원 기능은 추가하지 않았다.

## 설계 이유

Entity와 BaseEntity, 기존 컬럼 및 소프트 삭제 메서드는 그대로 사용한다. 공개 카테고리 조회는 계속 활성 행만 반환한다. 삭제된 행을 포함하는 목록은 ADMIN 전용 경로에 분리하고 SecurityConfig에서 `/api/admin/**` 전체를 ADMIN으로 제한했다. 프론트엔드 Route 제한과 서버 보안을 함께 적용한다.

기존 POST/DELETE 및 공개 GET/PATCH 계약을 유지한다. 관리자 수정은 삭제된 행도 처리하도록 별도 경로를 추가했다. CategoryResponse의 `deleted` boolean은 `is_deleted`에서 생성하며 기존 응답 필드는 유지한다. 카테고리명 UNIQUE 제약에 따라 삭제된 행의 이름도 중복 검사의 대상이다. 신규 라이브러리와 DB schema 변경은 없다.

| Method | API | 용도 |
|---|---|---|
| GET | `/api/admin/categories?includeDeleted=false` | 관리자 목록, true이면 삭제 행 포함 |
| PATCH | `/api/admin/categories/{categoryId}` | 관리자 이름 수정, 삭제 상태 유지 |
| POST | `/api/categories` | 기존 카테고리 생성 |
| DELETE | `/api/categories/{categoryId}` | 기존 소프트 삭제 |

## 변경 파일

frontend:

- `src/App.tsx`
- `src/api/categoryApi.ts`
- `src/types/category.ts`
- `src/auth/AdminRoute.tsx`
- `src/pages/AdminLayout.tsx`
- `src/pages/AdminCategoryPage.tsx`
- `src/pages/admin.css`
- `src/components/DeleteCategoryDialog.tsx`
- `tests/admin-category.test.mjs`
- `docs/admin-category-verification.md`

backend:

- `src/main/java/com/jdr1128/shopping/domain/category/controller/AdminCategoryController.java`
- `src/main/java/com/jdr1128/shopping/domain/category/service/CategoryService.java`
- `src/main/java/com/jdr1128/shopping/domain/category/dto/CategoryResponse.java`
- `src/main/java/com/jdr1128/shopping/global/security/SecurityConfig.java`
- `src/test/java/com/jdr1128/shopping/domain/category/controller/AdminCategoryIntegrationTest.java`
- `docs/admin-category-tests.init.gradle`

## 검증 결과 (2026-10-16)

```powershell
# frontend
npm.cmd run build
npm.cmd run lint -- src
$env:PLAYWRIGHT_MODULE_PATH = '<기존 Playwright 설치 경로>/index.mjs'
node --test --test-concurrency=1 tests/auth.test.mjs tests/seller.test.mjs tests/admin-category.test.mjs

# backend
.\gradlew.bat -I docs/admin-category-tests.init.gradle test --tests '*AdminCategoryIntegrationTest' --tests '*CategoryServiceTest' bootJar --offline
```

- TypeScript/Vite 빌드 및 백엔드 bootJar 패키징 성공.
- src lint 오류 없음. 기존 판매자 폼/목록과 상품상세에 있는 set-state-in-effect 경고 3개만 남음. 이번 관리자 파일에는 경고 없음.
- 브라우저 및 인증 테스트 3개 모두 통과. 관리자 테스트는 Headless Edge에서 생성/수정 취소, 공백 검증, 중복 이름, 실패 후 입력 유지와 재시도, 삭제 확인·취소·재시도, 삭제 목록 포함/제외, 삭제 행 이름 수정, 빈 목록, USER/SELLER/비로그인 차단, 좌측 메뉴 위치, 모바일 가로 넘침 및 JavaScript 오류 없음을 검증했다.
- 브라우저 테스트는 실제 React 화면과 Axios를 실행하고 HTTP API만 mock으로 대체한다. Playwright는 도구에 설치된 것을 사용했으며 프로젝트 의존성에는 추가하지 않았다. Edge 설치가 필요하다.
- 백엔드는 실제 Spring Security 필터, Controller, Service, H2 DB를 연결한 통합 테스트 6개와 기존 CategoryServiceTest 3개, 총 9개 통과.
- 서버 통합 테스트에서 ADMIN 생성·수정·삭제, 삭제 flag/시각의 DB 저장, 공개 목록에서 삭제행 제외, 관리자 삭제 목록 포함, 삭제행 수정 시 상태 유지, 중복·공백 거부, 비로그인 401 및 USER/SELLER의 조회·생성·수정·삭제 403을 검증했다.
- 일반 Gradle test 실행은 기존 MemberServiceTest.java:134에서 LoginResult를 LoginResponse에 대입하는 컴파일 오류로 실패한다. 해당 테스트는 변경하지 않고 init script로 카테고리 테스트 소스만 컴파일했다. 전체 테스트 통과를 의미하지 않는다.
- 실제 MySQL 환경의 통합 검증은 수행하지 않았다.

## 다음 단계

기존 MemberServiceTest를 현재 로그인 DTO에 맞춰 수정하고 전체 테스트를 실행한다. 실제 ADMIN 계정과 MySQL 환경에서 카테고리 변경 후 상품 화면의 카테고리 조회까지 확인한다.
