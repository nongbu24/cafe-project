# 포인트 API

포인트 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

## 포인트 충전

인증된 사용자를 식별하고 입력받은 금액만큼 포인트를 충전한다.

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
    "userId": 10,
    "chargedAmount": 10000,
    "pointBalance": 13500,
    "chargedAt": "2026-07-16T14:30:00+09:00"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 성공 응답 코드 `SUCCESS` |
| `message` | string | O | 요청 처리 결과 설명 |
| `data.userId` | long | O | 사용자 식별값 |
| `data.chargedAmount` | long | O | 이번에 충전한 포인트 |
| `data.pointBalance` | long | O | 충전 완료 후 잔액 |
| `data.chargedAt` | string | O | 충전 완료 시각 |

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 충전금액이 형식·범위를 벗어남 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | 인증 토큰이 없음 |
| `401 Unauthorized` | `INVALID_TOKEN` | 인증 토큰이 유효하지 않거나 탈퇴한 사용자의 토큰임 |

회원 잔액 갱신과 `CHARGE` 포인트 거래 저장은 하나의 DB 트랜잭션으로 처리한다.
동시 충전이나 결제로 포인트가 유실되지 않도록 회원 행을 비관적 쓰기 잠금으로 조회한다.
