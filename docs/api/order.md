# 주문 API

주문 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

## 즉시 메뉴 주문 및 결제

로그인한 사용자가 메뉴 하나와 수량을 바로 주문하고, 로그인한 사용자의 포인트에서 주문 금액만큼 차감한다.
클라이언트는 주문할 회원 ID를 요청 본문으로 전달하지 않는다.

### 요청

```http
POST /api/v1/orders
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "menuId": 2,
  "quantity": 2
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|---|---|---|---|---|
| `menuId` | long | O | 1 이상 | 주문할 메뉴 식별값 |
| `quantity` | int | O | 1 이상 | 주문 수량 |

주문 사용자 식별값은 JWT 인증 결과에서만 사용한다. 요청 본문에 `userId`가 포함되어도 주문 주체로 사용하지 않는다.
한 번에 여러 메뉴를 주문하려면 장바구니에 항목을 담은 뒤 장바구니 주문 API를 사용한다.

### 성공 응답

`201 Created`

응답 헤더:

```http
Location: /api/v1/orders/1001
```

```json
{
  "code": "SUCCESS",
  "message": "주문이 완료되었습니다.",
  "data": {
    "orderId": 1001,
    "userId": 10,
    "items": [
      {
        "menuId": 2,
        "name": "카페라테",
        "unitPrice": 5000,
        "quantity": 2,
        "lineAmount": 10000
      },
      {
        "menuId": 3,
        "name": "아메리카노",
        "unitPrice": 4500,
        "quantity": 1,
        "lineAmount": 4500
      }
    ],
    "paymentAmount": 14500,
    "pointBalance": 8500,
    "status": "PAID",
    "paidAt": "2026-07-16T14:35:00+09:00"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 성공 응답 코드 `SUCCESS` |
| `message` | string | O | 주문 처리 결과 설명 |
| `data.orderId` | long | O | 주문 식별값 |
| `data.userId` | long | O | 사용자 식별값 |
| `data.items[].menuId` | long | O | 주문한 메뉴 식별값 |
| `data.items[].name` | string | O | 주문 당시 메뉴 이름 |
| `data.items[].unitPrice` | long | O | 주문 당시 메뉴 단가 |
| `data.items[].quantity` | int | O | 주문 수량 |
| `data.items[].lineAmount` | long | O | `unitPrice * quantity` |
| `data.paymentAmount` | long | O | 차감한 총 포인트 |
| `data.pointBalance` | long | O | 결제 완료 후 잔액 |
| `data.status` | string | O | 결제 완료 상태 `PAID` |
| `data.paidAt` | string | O | 결제 완료 시각 |

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 수량이 형식 또는 범위를 벗어남 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | Authorization 헤더가 없거나 Bearer 토큰 형식이 아님 |
| `401 Unauthorized` | `INVALID_TOKEN` | 토큰이 유효하지 않거나 토큰의 사용자가 존재하지 않거나 탈퇴 상태임 |
| `401 Unauthorized` | `BLACKLISTED_TOKEN` | 로그아웃되어 사용할 수 없는 토큰으로 요청함 |
| `404 Not Found` | `MENU_NOT_FOUND` | 메뉴가 존재하지 않음 |
| `409 Conflict` | `MENU_NOT_AVAILABLE` | 메뉴가 품절 또는 단종 상태라 주문할 수 없음 |
| `409 Conflict` | `INSUFFICIENT_POINTS` | 사용자의 포인트가 총 주문 금액보다 적음 |

### 트랜잭션과 동시성

다음 처리는 하나의 DB 트랜잭션으로 실행한다.

1. JWT에서 인증된 회원과 주문 가능한 메뉴를 조회한다. 메뉴 상태가 `AVAILABLE`이 아니면 주문하지 못한다.
2. 주문 금액을 계산한다.
3. 회원의 현재 포인트가 주문 금액 이상인지 확인한다.
4. 포인트에서 주문 금액을 차감한다.
5. `PAID` 주문, 주문 항목들과 `PAYMENT` 포인트 거래를 저장한다.
6. `ORDER_PAID` Outbox 이벤트를 저장한다.

같은 회원의 결제 요청이 동시에 들어와 잔액보다 많은 포인트가 사용되지 않도록 회원 행을 비관적 쓰기 잠금으로 조회한다.
어느 한 단계라도 실패하면 전체 트랜잭션을 롤백한다.

## 장바구니 주문 및 결제

로그인한 사용자의 장바구니에 담긴 모든 항목을 주문하고 결제한다.
요청 본문 없이 장바구니의 현재 항목을 기준으로 주문한다.

### 요청

```http
POST /api/v1/orders/from-cart
Authorization: Bearer {accessToken}
```

요청 본문은 없다.

### 성공 응답

즉시 주문과 같은 응답 구조를 사용한다. 주문이 완료되면 해당 회원의 장바구니 항목 전체를 삭제한다.

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 장바구니에 담긴 메뉴가 없음 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | Authorization 헤더가 없거나 Bearer 토큰 형식이 아님 |
| `401 Unauthorized` | `INVALID_TOKEN` | 토큰이 유효하지 않거나 토큰의 사용자가 존재하지 않거나 탈퇴 상태임 |
| `401 Unauthorized` | `BLACKLISTED_TOKEN` | 로그아웃되어 사용할 수 없는 토큰으로 요청함 |
| `404 Not Found` | `MENU_NOT_FOUND` | 장바구니에 담긴 메뉴가 존재하지 않음 |
| `409 Conflict` | `MENU_NOT_AVAILABLE` | 장바구니에 담긴 메뉴가 품절 또는 단종 상태라 주문할 수 없음 |
| `409 Conflict` | `INSUFFICIENT_POINTS` | 사용자의 포인트가 총 주문 금액보다 적음 |

주문 생성 중 실패하면 장바구니는 변경하지 않는다.

## 데이터 수집 플랫폼 전송

주문 트랜잭션이 커밋되면 Outbox 이벤트를 가능한 즉시 비동기로 전송한다.
`order_event_outbox.payload`에는 외부 플랫폼으로 전송할 원본 JSON을 저장한다.
저장 payload와 외부 플랫폼으로 전송하는 JSON 계약은 다음과 같다.

```json
{
  "eventId": 501,
  "eventType": "ORDER_PAID",
  "occurredAt": "2026-07-16T14:35:00+09:00",
  "userId": 10,
  "items": [
    {
      "menuId": 2,
      "quantity": 2
    },
    {
      "menuId": 3,
      "quantity": 1
    }
  ],
  "paymentAmount": 14500
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `eventId` | long | O | Outbox 이벤트 식별값. 중복 전송 판별에 사용 |
| `eventType` | string | O | `ORDER_PAID` |
| `occurredAt` | string | O | 결제 완료 시각 |
| `userId` | long | O | 사용자 식별값 |
| `items[].menuId` | long | O | 메뉴 식별값 |
| `items[].quantity` | int | O | 주문 수량 |
| `paymentAmount` | long | O | 결제금액 |

현재 구현은 실제 외부 플랫폼 대신 mock 전송 지점으로 즉시 전송하고, 성공하면 Outbox 이벤트를 `SENT`로 표시한다.
전송에 실패하면 주문과 결제는 되돌리지 않고 `retry_count`를 1 증가시킨 뒤 `next_retry_at`에 다음 재시도 시각을 저장한다.
재전송 작업은 재시도 시각이 지난 `PENDING` 이벤트를 주기적으로 다시 전송한다.
3회 실패한 이벤트는 `FAILED`로 표시하고 더 이상 자동으로 재전송하지 않는다.

재전송은 이미 완료된 주문과 결제를 취소하지 않고, 적어도 한 번(at-least-once) 전달을 보장하는 방향으로 동작한다.
따라서 외부 수신 측은 같은 `eventId`를 중복 집계하지 않아야 한다.
