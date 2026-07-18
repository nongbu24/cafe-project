# 카페 서비스 ERD

이 문서는 카페 서비스 DB 구조의 SSOT(Single Source of Truth)다.
실제 Entity, DDL 또는 마이그레이션을 추가할 때 이 문서와 함께 갱신한다.
DB가 직접 보장하는 테이블, 기본값, CHECK, FK와 인덱스는 Flyway 마이그레이션으로 관리한다.

## 1. 설계 범위

다음 기능에 필요한 데이터 구조를 정의한다.

- 카페 메뉴 목록 조회
- 회원가입, 로그인, 로그아웃과 회원탈퇴
- 회원 포인트 충전
- 메뉴 주문 및 포인트 결제
- 주문 내역의 데이터 수집 플랫폼 전송
- 최근 7일간 인기 메뉴 3개 조회

메뉴 등록·수정, 주문 취소와 환불은 현재 범위에 포함하지 않는다.

## 2. ERD

```mermaid
erDiagram
    USERS ||--o{ POINT_TRANSACTION : "포인트 거래"
    USERS ||--o{ ORDERS : "주문"
    MENUS ||--o{ ORDERS : "주문 대상"
    ORDERS ||--|| ORDER_EVENT_OUTBOX : "전송 이벤트"

    USERS {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR password
        VARCHAR user_status
        BIGINT point_balance
        BOOLEAN is_deleted
        TIMESTAMP created_at
        TIMESTAMP updated_at "NULL until modified"
    }

    MENUS {
        BIGINT id PK
        VARCHAR name
        BIGINT price
        VARCHAR status
        TIMESTAMP created_at
        TIMESTAMP updated_at "NULL until modified"
    }

    POINT_TRANSACTION {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT order_id UK_NULL
        VARCHAR type
        BIGINT amount
        BIGINT balance_after
        TIMESTAMP created_at
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT menu_id FK
        VARCHAR menu_name
        BIGINT payment_amount
        VARCHAR status
        TIMESTAMP paid_at
        TIMESTAMP created_at
    }

    ORDER_EVENT_OUTBOX {
        BIGINT id PK
        BIGINT order_id FK_UK
        VARCHAR event_type
        TEXT payload
        VARCHAR status
        INT retry_count
        TIMESTAMP next_retry_at
        TIMESTAMP sent_at
        TIMESTAMP created_at
    }
```

`UK_NULL`은 nullable 유일 키, `FK_UK`는 외래 키이면서 유일 키임을 의미한다.

JWT 블랙리스트는 관계형 DB 테이블이 아니므로 ERD에 포함하지 않는다.
로그아웃·회원탈퇴 토큰의 `jti`는 Redis의 `auth:blacklist:{jti}` 키로 저장하고 JWT의 남은 유효시간을 TTL로 사용한다.

## 3. 테이블 정의

### 3.1 `users`

회원과 현재 포인트 잔액을 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, 자동 증가 | 사용자 식별값 |
| `username` | `VARCHAR(50)` | NOT NULL, UNIQUE | 로그인 식별값 |
| `password` | `VARCHAR(60)` | NOT NULL | BCrypt 암호화 비밀번호 |
| `user_status` | `VARCHAR(20)` | NOT NULL | `ADMIN` 또는 `USER` |
| `point_balance` | `BIGINT` | NOT NULL, 기본값 0, 0 이상 | 현재 사용 가능한 포인트 |
| `is_deleted` | `BOOLEAN` | NOT NULL, 기본값 `false` | 회원탈퇴 여부 |
| `created_at` | `TIMESTAMP` | NOT NULL | 생성 시각 |
| `updated_at` | `TIMESTAMP` | NULL | 마지막 수정 시각. 생성 이후 수정되지 않았으면 `NULL` |

포인트 잔액 갱신은 동시 요청에서 금액이 유실되지 않도록 회원 행을 비관적 쓰기 잠금으로 조회한 뒤 처리한다.
회원탈퇴는 행을 삭제하지 않고 `is_deleted`를 `true`로 변경하며 탈퇴 회원의 로그인과 인증을 거부한다.

### 3.2 `menus`

카페에서 판매하는 메뉴 정보를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, 자동 증가 | 메뉴 식별값 |
| `name` | `VARCHAR(100)` | NOT NULL | 메뉴 이름 |
| `price` | `BIGINT` | NOT NULL, 1 이상 | 판매 가격, 1원은 1포인트와 동일 |
| `status` | `VARCHAR(20)` | NOT NULL | `AVAILABLE`, `SOLD_OUT`, `DISCONTINUED` |
| `created_at` | `TIMESTAMP` | NOT NULL | 생성 시각 |
| `updated_at` | `TIMESTAMP` | NULL | 마지막 수정 시각. 생성 이후 수정되지 않았으면 `NULL` |

메뉴 상태는 `AVAILABLE`이면 판매중, `SOLD_OUT`이면 품절, `DISCONTINUED`이면 단종을 의미한다.
메뉴 목록 조회에는 판매중과 품절 메뉴만 포함한다.

### 3.3 `point_transaction`

포인트 충전과 사용 이력을 변경 불가능한 원장 형태로 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, 자동 증가 | 포인트 거래 식별값 |
| `user_id` | `BIGINT` | NOT NULL, FK → `users.id` | 거래 회원 |
| `order_id` | `BIGINT` | NULL, UNIQUE | 사용 거래와 연결된 주문 식별값. 단순 참조값이며 DB FK는 두지 않음 |
| `type` | `VARCHAR(20)` | NOT NULL | `CHARGE` 또는 `PAYMENT` |
| `amount` | `BIGINT` | NOT NULL, 1 이상 | 충전하거나 사용한 포인트의 절댓값 |
| `balance_after` | `BIGINT` | NOT NULL, 0 이상 | 거래 완료 직후 잔액 |
| `created_at` | `TIMESTAMP` | NOT NULL | 거래 완료 시각 |

`amount`는 항상 양수로 저장하고 `type`으로 증가와 감소를 구분한다.
`PAYMENT`이면 `order_id`가 반드시 존재하고, `CHARGE`이면 `order_id`가 없어야 한다.

### 3.4 `orders`

결제가 완료된 메뉴 주문을 저장한다. 현재 요구사항에서는 한 주문에 메뉴 한 개만 포함한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, 자동 증가 | 주문 식별값 |
| `user_id` | `BIGINT` | NOT NULL, FK → `users.id` | 주문 회원 |
| `menu_id` | `BIGINT` | NOT NULL, FK → `menus.id` | 주문 메뉴 |
| `menu_name` | `VARCHAR(100)` | NOT NULL | 주문 당시 메뉴 이름 스냅샷 |
| `payment_amount` | `BIGINT` | NOT NULL, 1 이상 | 주문 당시 결제 금액 스냅샷 |
| `status` | `VARCHAR(20)` | NOT NULL | 현재 범위에서는 `PAID` 사용 |
| `paid_at` | `TIMESTAMP` | NOT NULL | 결제 완료 시각이자 인기 메뉴 집계 기준 |
| `created_at` | `TIMESTAMP` | NOT NULL | 주문 생성 시각 |

메뉴 이름과 가격을 주문에 복사하므로 이후 메뉴가 변경되어도 과거 주문 정보가 유지된다.
회원 잔액 차감, 포인트 거래 저장, 주문 저장과 Outbox 저장은 하나의 DB 트랜잭션으로 처리한다.

### 3.5 `order_event_outbox`

결제가 완료된 주문을 외부 데이터 수집 플랫폼으로 안정적으로 전송하기 위한 이벤트를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| `id` | `BIGINT` | PK, 자동 증가 | 이벤트 식별값 |
| `order_id` | `BIGINT` | NOT NULL, UNIQUE, FK → `orders.id` | 전송 대상 주문 |
| `event_type` | `VARCHAR(50)` | NOT NULL | `ORDER_PAID` |
| `payload` | `TEXT` | NOT NULL | 외부 전송 원본 JSON. `eventId`, `eventType`, `occurredAt`, `userId`, `menuId`, `paymentAmount` 포함 |
| `status` | `VARCHAR(20)` | NOT NULL | `PENDING`, `SENDING`, `SENT`, `FAILED` |
| `retry_count` | `INT` | NOT NULL, 기본값 0, 0 이상 | 전송 재시도 횟수 |
| `next_retry_at` | `TIMESTAMP` | NULL | 다음 재시도 예정 시각 |
| `sent_at` | `TIMESTAMP` | NULL | 전송 성공 시각 |
| `created_at` | `TIMESTAMP` | NOT NULL | 이벤트 생성 시각 |

현재 구현은 주문 트랜잭션 커밋 후 mock 전송 지점으로 이벤트를 전송하고, 성공하면 `SENT`로 표시한다.
`FAILED` 처리, `retry_count` 증가, `next_retry_at` 계산과 재시도 작업은 향후 구현 범위다.
`order_id`의 유일 제약으로 같은 주문의 이벤트가 중복 생성되는 것을 막는다.
외부 수신 측에는 `eventId`를 함께 보내 같은 이벤트의 재전송을 식별할 수 있게 한다.

## 4. 인덱스

| 인덱스 | 대상 컬럼 | 목적 |
|---|---|---|
| `idx_orders_paid_at_menu` | `orders(status, paid_at, menu_id)` | 최근 7일 결제 주문을 메뉴별로 집계 |
| `idx_point_transaction_user_created` | `point_transaction(user_id, created_at)` | 회원별 포인트 이력 조회와 감사 |
| `idx_outbox_status_retry` | `order_event_outbox(status, next_retry_at)` | 향후 전송 실패 이벤트 재시도 조회 |

## 5. 인기 메뉴 집계 규칙

- API 요청 처리 중 기준 시각 `asOf`를 한 번 정한다.
- 집계 구간은 `asOf - 7일` 이상, `asOf` 미만이다. 즉, 직전 168시간이다.
- `orders.status = 'PAID'`인 주문만 센다.
- 현재 요구사항에서는 주문 한 건이 메뉴 한 개이므로 주문 행 한 건을 주문 횟수 1회로 센다.
- 메뉴별 주문 횟수 내림차순, 주문 횟수가 같으면 메뉴 ID 오름차순으로 정렬하여 3개를 반환한다.
- 집계 대상 주문이 3개 메뉴보다 적으면 존재하는 메뉴만 반환한다.

DB에는 timezone 없는 `TIMESTAMP` 컬럼에 UTC 기준 `LocalDateTime`을 저장한다.
API 응답은 저장된 UTC 시각을 한국 시간대로 변환하여 ISO 8601 형식과 offset을 함께 반환한다.
