# 포인트 API

포인트 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

## 포인트 충전 결제 준비

인증된 사용자를 식별하고 입력받은 금액만큼 포인트를 충전하기 위한 포트원 결제 정보를 생성한다.
이 API는 포인트 잔액을 즉시 증가시키지 않는다.
클라이언트는 응답의 `paymentId`, `storeId`, `channelKey`, `orderName`, `amount`, `currency`를 사용해 포트원 결제창을 호출한다.

### 요청

```http
POST /api/v1/users/me/point-charges
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "amount": 10000
}
```

| 위치 | 필드 | 타입 | 필수 | 제약조건 | 설명 |
|---|---|---|---|---|---|
| header | `Authorization` | string | O | Bearer 토큰 | 충전할 사용자 인증 토큰 |
| body | `amount` | long | O | 1 이상 | 충전할 금액이자 포인트 |

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "paymentId": "point-charge-7f5d3f9b6a8d4d2b8a9c44bb2f6c0c51",
    "storeId": "store-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "channelKey": "channel-key",
    "orderName": "포인트 10000원 충전",
    "amount": 10000,
    "currency": "KRW"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 성공 응답 코드 `SUCCESS` |
| `message` | string | O | 요청 처리 결과 설명 |
| `data.paymentId` | string | O | 서버가 생성한 포트원 결제 식별값 |
| `data.storeId` | string | O | 포트원 상점 식별값 |
| `data.channelKey` | string | O | 포트원 채널 키 |
| `data.orderName` | string | O | 결제창에 표시할 주문명 |
| `data.amount` | long | O | 결제 금액 |
| `data.currency` | string | O | 결제 통화. 현재 `KRW` |

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 충전금액이 형식·범위를 벗어남 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | 인증 토큰이 없음 |
| `401 Unauthorized` | `INVALID_TOKEN` | 인증 토큰이 유효하지 않거나 탈퇴한 사용자의 토큰임 |

충전 결제 준비 시 `point_charge_payment`에 `READY` 상태의 결제 대기 행을 저장한다.
회원 잔액 갱신과 `CHARGE` 포인트 거래 저장은 포트원 결제 완료 웹훅 검증 이후에만 수행한다.

## 포트원 결제 웹훅

포트원 결제 상태 변경 웹훅을 수신한다.
현재 포인트 충전은 `Transaction.Paid` 이벤트만 처리하고, 다른 이벤트 타입은 성공 응답으로 무시한다.

### 요청

```http
POST /api/v1/portone/webhooks/payments
Content-Type: application/json
```

```json
{
  "type": "Transaction.Paid",
  "timestamp": "2026-07-19T10:00:00.000Z",
  "data": {
    "paymentId": "point-charge-7f5d3f9b6a8d4d2b8a9c44bb2f6c0c51",
    "storeId": "store-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "transactionId": "tx-id"
  }
}
```

| 위치 | 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| body | `type` | string | O | 포트원 웹훅 이벤트 타입 |
| body | `timestamp` | string | X | 웹훅 이벤트 발생 시각 |
| body | `data.paymentId` | string | O | 결제 식별값. `Transaction.Paid` 처리 시 필수 |
| body | `data.storeId` | string | O | 상점 식별값. `Transaction.Paid` 처리 시 필수 |
| body | `data.transactionId` | string | X | 포트원 결제 시도 식별값 |

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "paymentId": "point-charge-7f5d3f9b6a8d4d2b8a9c44bb2f6c0c51",
    "status": "PAID",
    "charged": true
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `data.paymentId` | string | O | 처리 대상 결제 식별값 |
| `data.status` | string | O | 처리 결과 상태. `PAID`, `IGNORED`, `NOT_FOUND` |
| `data.charged` | boolean | O | 이번 웹훅 처리로 포인트를 새로 적립했는지 여부 |

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 결제 식별값, 상점 식별값, 결제 상태 또는 결제 금액이 서버의 충전 요청과 일치하지 않음 |

웹훅 수신 시 서버는 포트원 V2 결제 단건 조회 API로 실제 결제 상태를 다시 확인한다.
조회 결과의 결제 상태가 `PAID`이고, 상점 식별값과 결제 금액이 서버에 저장된 충전 대기 정보와 일치할 때만 포인트를 적립한다.
웹훅 재전송으로 같은 `paymentId`가 다시 들어오면 이미 `PAID` 상태인 결제는 중복 적립하지 않는다.
