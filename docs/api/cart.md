# 장바구니 API

장바구니 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

회원은 회원가입 시 빈 장바구니를 자동으로 가진다. 장바구니 항목은 메뉴와 수량으로 구성된다.

## 장바구니 조회

로그인한 회원의 현재 장바구니를 조회한다.

### 요청

```http
GET /api/v1/carts/me
Authorization: Bearer {accessToken}
```

요청 본문은 없다.

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "장바구니 조회가 완료되었습니다.",
  "data": {
    "userId": 10,
    "items": [
      {
        "menuId": 2,
        "name": "카페라테",
        "unitPrice": 5000,
        "quantity": 2,
        "lineAmount": 10000
      }
    ],
    "totalAmount": 10000
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `data.userId` | long | O | 장바구니 소유 회원 식별값 |
| `data.items` | array | O | 장바구니 항목 목록. 비어 있으면 빈 배열 |
| `data.items[].menuId` | long | O | 메뉴 식별값 |
| `data.items[].name` | string | O | 현재 메뉴 이름 |
| `data.items[].unitPrice` | long | O | 현재 메뉴 가격 |
| `data.items[].quantity` | int | O | 장바구니 수량 |
| `data.items[].lineAmount` | long | O | `unitPrice * quantity` |
| `data.totalAmount` | long | O | 장바구니 항목 총 금액 |

## 장바구니 항목 수량 변경

로그인한 회원의 장바구니에서 특정 메뉴의 수량을 변경한다.
수량이 `1` 이상이면 해당 메뉴를 장바구니에 담거나 기존 수량을 변경한다.
수량이 `0`이면 해당 메뉴를 장바구니에서 삭제한다.

### 요청

```http
PATCH /api/v1/carts/me/items/{menuId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "quantity": 3
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|---|---|---|---|---|
| `menuId` | long | O | 1 이상 | 변경할 메뉴 식별값 |
| `quantity` | int | O | 0 이상 | 변경할 수량. `0`이면 항목 삭제 |

### 성공 응답

수량이 `1` 이상이면 `200 OK`와 변경된 항목을 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "장바구니 수량이 변경되었습니다.",
  "data": {
    "menuId": 2,
    "name": "카페라테",
    "unitPrice": 5000,
    "quantity": 3,
    "lineAmount": 15000
  }
}
```

수량이 `0`이면 `200 OK`와 삭제 결과 메시지를 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "장바구니 항목이 삭제되었습니다."
}
```

### 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | 수량이 비어 있거나 음수임 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | Authorization 헤더가 없거나 Bearer 토큰 형식이 아님 |
| `401 Unauthorized` | `INVALID_TOKEN` | 토큰이 유효하지 않거나 토큰의 사용자가 존재하지 않거나 탈퇴 상태임 |
| `401 Unauthorized` | `BLACKLISTED_TOKEN` | 로그아웃되어 사용할 수 없는 토큰으로 요청함 |
| `404 Not Found` | `MENU_NOT_FOUND` | 메뉴가 존재하지 않음 |

## 장바구니 전체 비우기

로그인한 회원의 장바구니에 담긴 모든 항목을 삭제한다.

### 요청

```http
DELETE /api/v1/carts/me/items
Authorization: Bearer {accessToken}
```

요청 본문은 없다.

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "장바구니가 비워졌습니다."
}
```

장바구니가 이미 비어 있어도 성공으로 처리한다.

## 주문 완료와 장바구니

장바구니 주문 API로 주문과 결제가 완료되면 해당 회원의 장바구니 항목 전체를 삭제한다.
즉시 주문 API는 장바구니를 변경하지 않는다.
주문 생성 중 메뉴 상태, 포인트 부족, 인증 실패 등으로 주문이 완료되지 않으면 장바구니는 변경하지 않는다.
