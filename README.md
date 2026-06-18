# 쇼핑몰 프로젝트

> 본 문서는 기존 쇼핑몰 프로젝트 기획 내용을 기반으로, 구현 전 확인사항을 설계에 반영한 GitHub `README.md` 문서입니다.  
> 첨부 ERD 이미지는 **초기 ERD 기준**으로 포함하며, 실제 구현에서는 아래의 **반영된 설계 변경사항**을 적용합니다.

---

## 반영된 설계 변경사항

기존 기획에서 확인된 문제를 다음과 같이 반영합니다.

| 구분 | 기존 내용 | 반영 내용 |
|---|---|---|
| 메시지 큐 표기 | `RabiitMQ` | `RabbitMQ`로 수정 |
| 주문 테이블명 | `Order` | SQL 예약어 충돌 방지를 위해 실제 테이블명은 `orders` 사용 |
| 회원-장바구니 관계 | `Member` ↔ `Cart` = 1:N | 회원 1명당 활성 장바구니 1개 구조로 수정, `Cart.memberId`에 UNIQUE 적용 |
| 상품 재고 | `Product`에 재고 컬럼 없음 | `stock`, `productStatus` 추가 |
| 판매자 정보 | `Product.seller` 문자열 | `sellerId` FK로 `Member.memberId` 참조 |
| 가격 검증 | 주문상품에만 가격 저장 | `CartItem.cartPrice`를 추가하여 장바구니 담은 시점의 가격 저장 |
| 장바구니 중복 상품 | 제약 없음 | `CartItem(cartId, productId)` 복합 UNIQUE 적용 |
| 주문상품 PK | `orderId + productId` 복합 PK | JPA 구현 편의성을 위해 `orderProductId` 단일 PK 추가 |
| 결제 상태 | `complete and cancle` | `READY`, `COMPLETE`, `FAILED`, `CANCELLED`로 수정 |
| 소프트 딜리트 | 컬럼 없음 | `isDeleted`, `deletedAt` 추가 |
| 리뷰/대댓글 | 권한표에는 있으나 ERD 없음 | 2차 기능으로 분리하거나 별도 ERD 확장 필요 |
| 상품 API 경로 | `/api/product` | REST 관례에 맞춰 `/api/products` 사용 권장 |
| Redis 장바구니 | 역할 불명확 | DB를 원본 저장소로 사용하고 Redis는 캐시로 사용 |

---

## 프로젝트 개요

1. 사용자 권한: `USER`, `SELLER`, `ADMIN` 역할 기반 접근 제어(RBAC)
   - `USER` 기준으로 구현
   - 비회원 구매는 추후 확장 기능으로 고려
2. 회원가입, 로그인, 상품 등록, 상품 목록 조회, 장바구니, 주문, 결제 프로세스 구현
3. JWT 인증
   - OAuth 로그인은 추후 확장 기능으로 고려
4. 실시간 가격 및 재고 관리
   - 품절 처리
   - 가격 변동 검증
   - 주문 시점 가격 스냅샷 저장
5. 비동기 메시징 시스템
   - RabbitMQ를 활용한 안정적인 결제 처리
   - 1차 구현에서는 모의 결제 또는 동기 처리로 시작 가능
6. 배포
   - Docker 기반 실행 환경 구성
   - GitHub Actions 기반 CI/CD 파이프라인 구성

---

## 기술 스택

| 카테고리 | 기술 | 용도 | 적용 단계 |
|---|---|---|---|
| Database | MySQL | 메인 DB | 1차 |
| Database | H2 | 테스트 DB | 1차 |
| FE | React | UI 프레임워크 | 1차 |
| Router | React Router | 클라이언트 사이드 라우팅, 페이지 전환 | 1차 |
| UI Library | React Bootstrap | UI 컴포넌트 | 1차 |
| HTTP Client | Axios | 백엔드 API 요청, JWT Authorization 헤더 처리 | 1차 |
| Language | Java | 백엔드 개발 언어 | 1차 |
| Framework | Spring Boot | 백엔드 애플리케이션 프레임워크 | 1차 |
| Security | Spring Security + JWT | 인증, 인가 | 1차 |
| ORM | Spring Data JPA | 객체와 관계형 DB 매핑 | 1차 |
| Query | QueryDSL | 상품 검색, 정렬, 동적 쿼리 | 2차 |
| Cache | Redis | 장바구니 조회 캐싱, 세션성 데이터 캐싱 | 2차 |
| Cache | Ehcache | 상품/카테고리 조회 캐싱 | 2차 |
| Message Queue | RabbitMQ | 비동기 결제 처리 | 2차 |
| CI/CD | GitHub Actions | 빌드/테스트/배포 자동화 | 2차 |
| Container | Docker | 컨테이너 기반 실행 환경 | 2차 |
| Testing | JUnit 5, Mockito | 단위 테스트, 서비스 테스트 | 1차 |

---

## 명명 규칙

Java 코드에서는 카멜 케이스를 사용하고, DB 테이블 및 컬럼은 스네이크 케이스를 권장합니다.

| 구분 | 예시 |
|---|---|
| Java Entity | `Member`, `Product`, `Order` |
| Java Field | `memberId`, `productName`, `orderDate` |
| DB Table | `members`, `products`, `orders` |
| DB Column | `member_id`, `product_name`, `order_date` |

> `Order`는 Java 엔티티명으로 사용할 수 있지만, SQL 예약어 충돌을 피하기 위해 실제 DB 테이블명은 `orders`를 사용합니다.

---

## ERD

초기 ERD 이미지는 아래와 같습니다.

![쇼핑몰 ERD](./docs/images/shopping-erd-original.png)

> 위 ERD 이미지는 초기 설계 기준입니다.  
> 실제 구현에서는 아래의 수정된 테이블 구조를 기준으로 진행합니다.

---

## 수정 반영 테이블 구조

### Member

실제 테이블명: `members`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 회원번호 | `memberId` | `member_id` | PK | BIGINT |
| 회원ID | `loginId` | `login_id` | UNIQUE, NOT NULL | VARCHAR |
| 비밀번호 | `password` | `password` | NOT NULL | VARCHAR |
| 이메일 | `email` | `email` | UNIQUE, NOT NULL | VARCHAR |
| 회원권한 | `role` | `role` | NOT NULL | ENUM |
| 삭제여부 | `isDeleted` | `is_deleted` | NOT NULL | BOOLEAN |
| 가입일자 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정일자 | `updatedAt` | `updated_at` |  | DATETIME |
| 삭제일자 | `deletedAt` | `deleted_at` |  | DATETIME |

권한 ENUM 예시:

```text
USER, SELLER, ADMIN
```

---

### Category

실제 테이블명: `categories`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 카테고리번호 | `categoryId` | `category_id` | PK | BIGINT |
| 카테고리명 | `categoryName` | `category_name` | UNIQUE, NOT NULL | VARCHAR |
| 삭제여부 | `isDeleted` | `is_deleted` | NOT NULL | BOOLEAN |
| 등록날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |
| 삭제일자 | `deletedAt` | `deleted_at` |  | DATETIME |

---

### Product

실제 테이블명: `products`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 상품번호 | `productId` | `product_id` | PK | BIGINT |
| 카테고리번호 | `categoryId` | `category_id` | FK, NOT NULL | BIGINT |
| 판매자번호 | `sellerId` | `seller_id` | FK, NOT NULL | BIGINT |
| 상품명 | `productName` | `product_name` | NOT NULL | VARCHAR |
| 가격 | `price` | `price` | NOT NULL | INT |
| 재고 | `stock` | `stock` | NOT NULL | INT |
| 상품상태 | `productStatus` | `product_status` | NOT NULL | ENUM |
| 삭제여부 | `isDeleted` | `is_deleted` | NOT NULL | BOOLEAN |
| 등록날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |
| 삭제일자 | `deletedAt` | `deleted_at` |  | DATETIME |

상품 상태 ENUM 예시:

```text
SELLING, SOLD_OUT, STOPPED
```

> 기존 `seller` 문자열 컬럼 대신 `sellerId`를 사용합니다.  
> 이를 통해 판매자 본인 상품 여부를 검증할 수 있습니다.

---

### ProductImage

실제 테이블명: `product_images`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 이미지번호 | `imageId` | `image_id` | PK | BIGINT |
| 상품번호 | `productId` | `product_id` | FK, NOT NULL | BIGINT |
| 이미지URL | `imageUrl` | `image_url` | NOT NULL | VARCHAR |
| 정렬순서 | `sortOrder` | `sort_order` | NOT NULL | INT |
| 등록날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |

---

### Cart

실제 테이블명: `carts`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 장바구니번호 | `cartId` | `cart_id` | PK | BIGINT |
| 회원번호 | `memberId` | `member_id` | FK, UNIQUE, NOT NULL | BIGINT |
| 등록날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |

> 회원 1명당 활성 장바구니 1개를 가지는 구조로 구현합니다.

---

### CartItem

실제 테이블명: `cart_items`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 장바구니상품번호 | `cartItemId` | `cart_item_id` | PK | BIGINT |
| 장바구니번호 | `cartId` | `cart_id` | FK, NOT NULL | BIGINT |
| 상품번호 | `productId` | `product_id` | FK, NOT NULL | BIGINT |
| 수량 | `quantity` | `quantity` | NOT NULL | INT |
| 장바구니담은가격 | `cartPrice` | `cart_price` | NOT NULL | INT |
| 등록날짜 | `addedAt` | `added_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |

권장 제약조건:

```sql
UNIQUE (cart_id, product_id)
```

> `cartPrice`는 장바구니에 상품을 담은 시점의 가격입니다.  
> 주문 생성 시 현재 `Product.price`와 비교하여 가격 변경 여부를 검증합니다.

---

### Order

Java 엔티티명: `Order`  
실제 테이블명: `orders`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 주문번호 | `orderId` | `order_id` | PK | BIGINT |
| 회원번호 | `memberId` | `member_id` | FK, NOT NULL | BIGINT |
| 수취인이름 | `name` | `name` | NOT NULL | VARCHAR |
| 수취인 휴대폰번호 | `phoneNumber` | `phone_number` | NOT NULL | VARCHAR |
| 수취인 주소 | `address1` | `address1` | NOT NULL | VARCHAR |
| 수취인 상세주소 | `address2` | `address2` |  | VARCHAR |
| 요청사항 | `requestedTerm` | `requested_term` |  | VARCHAR |
| 총 주문금액 | `totalOrderPrice` | `total_order_price` | NOT NULL | INT |
| 주문상태 | `orderStatus` | `order_status` | NOT NULL | ENUM |
| 주문날짜 | `orderDate` | `order_date` | NOT NULL | DATETIME |
| 주문수정날짜 | `updatedOrderDate` | `updated_order_date` |  | DATETIME |

주문 상태 ENUM 예시:

```text
ORDERED, PAID, CANCELLED, FAILED
```

---

### OrderProduct

실제 테이블명: `order_products`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 주문상품번호 | `orderProductId` | `order_product_id` | PK | BIGINT |
| 주문번호 | `orderId` | `order_id` | FK, NOT NULL | BIGINT |
| 상품번호 | `productId` | `product_id` | FK, NOT NULL | BIGINT |
| 상품명 | `productName` | `product_name` | NOT NULL | VARCHAR |
| 주문수량 | `amount` | `amount` | NOT NULL | INT |
| 상품가격 | `price` | `price` | NOT NULL | INT |
| 주문상품금액 | `orderPrice` | `order_price` | NOT NULL | INT |

> `productName`, `price`, `orderPrice`는 주문 시점의 상품 정보를 저장하는 스냅샷입니다.  
> 상품명이 변경되거나 가격이 변경되어도 과거 주문 내역은 유지됩니다.

---

### Payment

실제 테이블명: `payments`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 결제번호 | `payId` | `pay_id` | PK | BIGINT |
| 주문번호 | `orderId` | `order_id` | FK, UNIQUE, NOT NULL | BIGINT |
| 카드사 | `cardCompany` | `card_company` |  | VARCHAR |
| 카드번호 | `cardNumber` | `card_number` |  | VARCHAR |
| 결제가격 | `payPrice` | `pay_price` | NOT NULL | INT |
| 결제시간 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 결제상태 | `payStatus` | `pay_status` | NOT NULL | ENUM |

결제 상태 ENUM 예시:

```text
READY, COMPLETE, FAILED, CANCELLED
```

> 실제 결제 시스템에서는 카드번호 전체를 저장하면 안 됩니다.  
> 개인 프로젝트에서는 모의 결제 기준으로 사용하되, 실제 서비스 구조에서는 카드번호 마스킹 또는 결제사 거래키만 저장하는 방향이 안전합니다.

---

## 2차 기능 확장 테이블

리뷰와 대댓글 기능은 기존 권한표에 포함되어 있으나 초기 ERD에는 존재하지 않습니다.  
따라서 1차 MVP에서는 제외하고, 2차 기능으로 구현하는 것을 권장합니다.

### Review

실제 테이블명: `reviews`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 리뷰번호 | `reviewId` | `review_id` | PK | BIGINT |
| 회원번호 | `memberId` | `member_id` | FK, NOT NULL | BIGINT |
| 상품번호 | `productId` | `product_id` | FK, NOT NULL | BIGINT |
| 평점 | `rating` | `rating` | NOT NULL | INT |
| 내용 | `content` | `content` | NOT NULL | TEXT |
| 삭제여부 | `isDeleted` | `is_deleted` | NOT NULL | BOOLEAN |
| 작성날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |
| 삭제날짜 | `deletedAt` | `deleted_at` |  | DATETIME |

---

### ReviewReply

실제 테이블명: `review_replies`

| 논리명 | Java 필드명 | DB 컬럼명 | 제약조건 | 타입 |
|---|---|---|---|---|
| 대댓글번호 | `replyId` | `reply_id` | PK | BIGINT |
| 리뷰번호 | `reviewId` | `review_id` | FK, NOT NULL | BIGINT |
| 판매자번호 | `sellerId` | `seller_id` | FK, NOT NULL | BIGINT |
| 내용 | `content` | `content` | NOT NULL | TEXT |
| 삭제여부 | `isDeleted` | `is_deleted` | NOT NULL | BOOLEAN |
| 작성날짜 | `createdAt` | `created_at` | NOT NULL | DATETIME |
| 수정날짜 | `updatedAt` | `updated_at` |  | DATETIME |
| 삭제날짜 | `deletedAt` | `deleted_at` |  | DATETIME |

---

## 주요 테이블 관계

| 관계 | 설명 | 반영 내용 |
|---|---|---|
| `Member` ↔ `Cart` | 회원 1명은 활성 장바구니 1개를 가짐 | 1:1 |
| `Member` ↔ `Order` | 회원 1명은 여러 주문을 생성할 수 있음 | 1:N |
| `Member` ↔ `Product` | 판매자 회원 1명은 여러 상품을 등록할 수 있음 | 1:N |
| `Category` ↔ `Product` | 하나의 카테고리에 여러 상품이 포함됨 | 1:N |
| `Product` ↔ `ProductImage` | 하나의 상품에 여러 이미지가 등록될 수 있음 | 1:N |
| `Cart` ↔ `CartItem` | 하나의 장바구니에 여러 상품이 담김 | 1:N |
| `Product` ↔ `CartItem` | 하나의 상품은 여러 장바구니에 담길 수 있음 | 1:N |
| `Order` ↔ `OrderProduct` | 하나의 주문에 여러 주문상품이 포함됨 | 1:N |
| `Product` ↔ `OrderProduct` | 하나의 상품은 여러 주문상품에 포함될 수 있음 | 1:N |
| `Order` ↔ `Payment` | 하나의 주문은 하나의 결제정보를 가짐 | 1:1 |

---

## 주요 기능

### 핵심 비즈니스 로직

1. 다중 권한 시스템
   - `USER`, `SELLER`, `ADMIN` 역할 기반 접근 제어
2. 상품 관리
   - `SELLER`, `ADMIN`만 상품 등록 가능
   - `SELLER`는 본인이 등록한 상품만 수정/삭제 가능
   - `ADMIN`은 전체 상품 관리 가능
3. 실시간 가격 검증
   - 장바구니에 담은 시점의 가격을 `CartItem.cartPrice`에 저장
   - 주문 생성 시 현재 `Product.price`와 비교
   - 가격이 변경된 경우 주문 생성 차단 또는 사용자 재확인 요청
4. 재고 관리
   - 주문 생성 시 상품 재고 확인
   - 결제 성공 또는 주문 확정 시 재고 차감
   - 재고가 0이면 `SOLD_OUT` 상태로 변경
5. 비동기 메시징 시스템
   - RabbitMQ를 활용한 비동기 결제 처리
   - Dead Letter Queue를 통한 메시지 처리 실패 대응
   - 1차 MVP 이후 적용 권장
6. 캐싱 시스템
   - DB를 장바구니 원본 저장소로 사용
   - Redis는 장바구니 조회 성능 개선용 캐시로 사용
   - Ehcache는 상품/카테고리 조회 캐시로 사용
7. 이미지 관리
   - 로컬 파일 시스템을 통한 상품 이미지 업로드/다운로드/삭제
   - 이미지 정렬 순서 관리
8. 소프트 딜리트
   - 회원 탈퇴, 상품 삭제, 카테고리 삭제 시 물리 삭제 대신 논리 삭제 처리
   - `isDeleted`, `deletedAt` 컬럼 사용

---

## 주문 생성 흐름

```text
1. 사용자가 장바구니에서 주문 생성 요청
2. 서버가 CartItem 목록 조회
3. 각 CartItem의 cartPrice와 현재 Product.price 비교
4. 가격이 변경된 상품이 있으면 409 CONFLICT 응답
5. 현재 Product.stock 확인
6. 재고가 부족하면 409 CONFLICT 응답
7. orders 생성
8. order_products 생성
9. 주문 시점 상품명, 가격, 수량 스냅샷 저장
10. 결제 요청 생성
11. 결제 성공 시 주문상태 PAID, 결제상태 COMPLETE 처리
12. 결제 실패 시 주문상태 FAILED, 결제상태 FAILED 처리
```

---

## API 엔드포인트

### 회원 `/api/members`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/members/signup` | 회원가입 | ALL |
| POST | `/api/members/login` | 로그인 및 JWT 발급 | ALL |
| GET | `/api/members/me` | 내 회원정보 조회 | USER, SELLER, ADMIN |
| PATCH | `/api/members/me` | 내 회원정보 수정 | USER, SELLER, ADMIN |
| DELETE | `/api/members/me` | 회원 탈퇴, 소프트 딜리트 | USER, SELLER, ADMIN |
| GET | `/api/members/{memberId}` | 특정 회원 조회 | ADMIN |
| PATCH | `/api/members/{memberId}/role` | 회원 권한 변경 | ADMIN |

---

### 카테고리 `/api/categories`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/categories` | 카테고리 목록 조회 | ALL |
| GET | `/api/categories/{categoryId}` | 카테고리 단건 조회 | ALL |
| POST | `/api/categories` | 카테고리 등록 | ADMIN |
| PATCH | `/api/categories/{categoryId}` | 카테고리 수정 | ADMIN |
| DELETE | `/api/categories/{categoryId}` | 카테고리 삭제, 소프트 딜리트 | ADMIN |

---

### 상품 `/api/products`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/products` | 상품 목록 조회, 검색, 정렬 | ALL |
| GET | `/api/products/{productId}` | 상품 상세 조회 | ALL |
| POST | `/api/products` | 상품 등록 | SELLER, ADMIN |
| PATCH | `/api/products/{productId}` | 상품 수정 | SELLER, ADMIN |
| DELETE | `/api/products/{productId}` | 상품 삭제, 소프트 딜리트 | SELLER, ADMIN |
| PATCH | `/api/products/{productId}/stock` | 상품 재고 수정 | SELLER, ADMIN |
| POST | `/api/products/{productId}/images` | 상품 이미지 업로드 | SELLER, ADMIN |
| GET | `/api/products/{productId}/images` | 상품 이미지 목록 조회 | ALL |
| DELETE | `/api/products/{productId}/images/{imageId}` | 상품 이미지 삭제 | SELLER, ADMIN |

검색 파라미터 예시:

```text
GET /api/products?categoryId=1&keyword=keyboard&sort=price,asc
GET /api/products?status=SELLING&page=0&size=20
```

---

### 장바구니 `/api/carts`

회원 1명당 활성 장바구니 1개를 기준으로 합니다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/carts/me` | 내 장바구니 조회 | USER, SELLER, ADMIN |
| POST | `/api/carts/items` | 장바구니에 상품 추가 | USER, SELLER, ADMIN |
| PATCH | `/api/carts/items/{cartItemId}` | 장바구니 상품 수량 수정 | USER, SELLER, ADMIN |
| DELETE | `/api/carts/items/{cartItemId}` | 장바구니 상품 삭제 | USER, SELLER, ADMIN |
| DELETE | `/api/carts/items` | 장바구니 전체 비우기 | USER, SELLER, ADMIN |

장바구니 상품 추가 요청 예시:

```json
{
  "productId": 1,
  "quantity": 2
}
```

---

### 주문 `/api/orders`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/orders` | 장바구니 기반 주문 생성 | USER, SELLER, ADMIN |
| GET | `/api/orders` | 내 주문 목록 조회 | USER, SELLER, ADMIN |
| GET | `/api/orders/{orderId}` | 주문 상세 조회 | USER, SELLER, ADMIN |
| PATCH | `/api/orders/{orderId}/cancel` | 주문 취소 | USER, SELLER, ADMIN |
| GET | `/api/orders/{orderId}/products` | 주문상품 목록 조회 | USER, SELLER, ADMIN |

주문 생성 요청 예시:

```json
{
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address1": "서울특별시 강남구",
  "address2": "101동 1001호",
  "requestedTerm": "문 앞에 놓아주세요"
}
```

---

### 결제 `/api/payments`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/orders/{orderId}/payments` | 주문 결제 요청 | USER, SELLER, ADMIN |
| GET | `/api/payments/{payId}` | 결제 정보 조회 | USER, SELLER, ADMIN |
| PATCH | `/api/payments/{payId}/cancel` | 결제 취소 | USER, SELLER, ADMIN |

결제 요청 예시:

```json
{
  "cardCompany": "현대카드",
  "cardNumber": "****-****-****-1234"
}
```

---

## 2차 기능 API

리뷰와 대댓글은 2차 기능으로 분리합니다.

### 리뷰 `/api/reviews`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/products/{productId}/reviews` | 리뷰 작성 | USER |
| GET | `/api/products/{productId}/reviews` | 상품 리뷰 목록 조회 | ALL |
| PATCH | `/api/reviews/{reviewId}` | 리뷰 수정 | USER, ADMIN |
| DELETE | `/api/reviews/{reviewId}` | 리뷰 삭제, 소프트 딜리트 | USER, ADMIN |

### 대댓글 `/api/review-replies`

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/reviews/{reviewId}/replies` | 판매자 대댓글 작성 | SELLER, ADMIN |
| PATCH | `/api/review-replies/{replyId}` | 대댓글 수정 | SELLER, ADMIN |
| DELETE | `/api/review-replies/{replyId}` | 대댓글 삭제 | SELLER, ADMIN |

---

## 권한별 접근 제어

| 기능 | USER | SELLER | ADMIN |
|---|---:|---:|---:|
| 회원가입, 로그인 | O | O | O |
| 상품 등록, 수정, 삭제 | X | O, 본인 상품만 | O |
| 상품 조회, 검색 | O | O | O |
| 상품 재고 관리 | X | O, 본인 상품만 | O |
| 장바구니 관리 | O | O | O |
| 주문 생성, 조회 | O | O | O |
| 결제 요청, 취소 | O | O | O |
| 카테고리 관리 | X | X | O |
| 리뷰 작성, 수정, 삭제 | O, 본인 리뷰만 | X | O |
| 대댓글 작성, 수정 | X | O, 본인 상품 리뷰만 | O |

---

## 권장 구현 순서

처음부터 모든 기술을 적용하기보다 다음 순서로 구현합니다.

### 1차 MVP

1. 프로젝트 생성 및 기본 패키지 구조 구성
2. `Member`, `Category`, `Product` 엔티티 작성
3. 회원가입, 로그인, JWT 인증 구현
4. 권한 구분: `USER`, `SELLER`, `ADMIN`
5. 카테고리 CRUD
6. 상품 CRUD
7. 상품 이미지 업로드
8. 장바구니 상품 추가, 수량 변경, 삭제
9. 주문 생성
10. 주문 시 상품 가격 검증
11. 주문 시 재고 검증 및 차감
12. 결제 정보 저장 및 모의 결제 처리
13. 주문 취소
14. JUnit 5, Mockito 기반 테스트 작성

### 2차 확장

1. QueryDSL 기반 상품 검색/정렬 고도화
2. Redis 장바구니 캐싱
3. Ehcache 상품/카테고리 조회 캐싱
4. RabbitMQ 비동기 결제 처리
5. Dead Letter Queue 처리
6. 리뷰/대댓글 기능
7. Docker 실행 환경 구성
8. GitHub Actions CI/CD 구성
9. OAuth 로그인 추가

---

## 예외 처리 기준

| 상황 | HTTP Status | 예외 코드 예시 |
|---|---:|---|
| 로그인 실패 | 401 | `LOGIN_FAILED` |
| 권한 없음 | 403 | `ACCESS_DENIED` |
| 상품 없음 | 404 | `PRODUCT_NOT_FOUND` |
| 장바구니 상품 없음 | 404 | `CART_ITEM_NOT_FOUND` |
| 가격 변경 | 409 | `PRICE_CHANGED` |
| 재고 부족 | 409 | `OUT_OF_STOCK` |
| 이미 결제된 주문 | 409 | `ALREADY_PAID` |
| 결제 실패 | 400 | `PAYMENT_FAILED` |

---

## README 기준 구현 주의사항

- ERD 이미지는 초기 설계이므로, 실제 구현 전 DB 설계는 본 문서의 수정 반영 테이블 구조를 기준으로 합니다.
- `orders` 테이블명은 반드시 예약어 충돌을 피하기 위해 복수형으로 사용합니다.
- `Product.sellerId`는 `Member.memberId`를 참조해야 합니다.
- `Cart.memberId`에는 UNIQUE 제약조건을 둡니다.
- `CartItem.cartPrice`를 통해 장바구니 담은 시점의 가격을 저장합니다.
- `OrderProduct`에는 주문 시점 상품명과 가격을 반드시 스냅샷으로 저장합니다.
- Redis는 장바구니 원본 저장소가 아니라 캐시로 사용합니다.
- 리뷰와 대댓글은 1차 MVP에 포함하지 않고 2차 기능으로 분리합니다.

