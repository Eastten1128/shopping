# Refresh Token 하드 삭제

## 처리 흐름

- 로그인: 해당 회원의 기존 Refresh Token 행을 DELETE한 뒤 새 토큰의 SHA-256 해시를 저장한다.
- 재발급: JWT 서명·만료·타입을 검증하고 DB 행을 잠금 조회한다. 회원과 저장된 토큰을 검증한 뒤 기존 행 삭제와 새 행 저장을 같은 트랜잭션에서 수행한다.
- 이전 토큰 재사용: 삭제된 해시에 해당하는 행이 없으므로 재발급을 거부한다.
- 로그아웃: 쿠키 토큰의 해시에 해당하는 행을 DELETE한다. 쿠키가 없거나 이미 삭제됐으면 안전하게 종료한다.
- 만료 정리: 서버 시작 시 및 이후 기본 1시간 간격으로 `expires_at <= 현재시각` 또는 `revoked = true`인 행을 DELETE한다. 재접속하지 않는 회원의 토큰과 기존 소프트 삭제 행도 정리된다.

JWT 시각은 초 단위이므로 같은 초에 발급한 토큰이 동일해질 수 있다. Refresh Token에 발급마다 UUID `jti`를 넣어 교체 후 이전 토큰이 다시 유효해지는 것을 방지한다.

## 설계

`BaseEntity`, 테이블명, FK, 해시 UNIQUE 제약조건, `revoked`/`revoked_at` 컬럼 매핑을 유지한다. 새 회수 경로에서는 revoked 상태를 변경하지 않는다. 스키마 변경이나 새 라이브러리는 없다. HttpOnly 쿠키 및 로그인/재발급/로그아웃 API 계약도 유지한다.

스케줄러는 별도 서비스 빈의 쓰기 트랜잭션을 호출한다. 만료 JWT 요청이 거부되어도 주기 정리가 독립적으로 실행된다. 기본 간격은 이전 정리 완료 후 1시간이며 다음 속성으로 변경할 수 있다.

- `jwt.refresh-token-cleanup-delay-milliseconds` (기본 `3600000`)
- `jwt.refresh-token-cleanup-initial-delay-milliseconds` (기본 `0`)

토큰 누적은 주로 DB 저장 공간과 인덱스 부담을 늘린다. 만료 행은 정리 주기까지 잠시 남을 수 있지만 인증에는 사용할 수 없다. 서버가 중지된 동안에는 정리되지 않으며 재시작 시 다시 정리한다.

## 검증

2026-10-12 현재 작업 트리 기준:

```powershell
# shoppingmall-backend
.\gradlew.bat test --tests '*RefreshTokenRepositoryTest' --tests '*RefreshTokenServiceTest' --tests '*JwtTokenProviderTest' bootJar --offline
# shoppingmall-frontend
node --test tests/auth.test.mjs
```

- 백엔드 14개 테스트 통과, Java 및 테스트 컴파일/bootJar 성공.
- H2에서 실제 DELETE, 회원별 삭제 범위, 반복 로그아웃, 만료 경계, 기존 revoked 행 정리, 로그인 시 교체, 재발급과 이전 토큰 거부, 잘못된 타입/토큰 거부를 검증했다.
- 실제 JWT 생성기를 사용해 연속 발급의 고유성과 새 Access/Refresh Token 유효성을 검증했다.
- 프론트엔드 인증 테스트 1개 통과. 이 테스트는 mock API 기반이며 실제 브라우저·MySQL 통합 검증은 포함하지 않는다.
- 기존 작업 트리에 인증 기반 코드 등 미커밋 변경이 있다. 이 검증은 해당 변경을 포함한 작업 트리 기준이며, 별도의 깨끗한 checkout 빌드는 수행하지 않았다.

다음 검증은 실행 중인 MySQL 환경에서 로그인·재발급·로그아웃 전후 행 수와 주기 정리 실행을 확인하는 것이다.
