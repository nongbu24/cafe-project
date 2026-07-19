# 메뉴 API

메뉴 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

## 1. 메뉴 목록 조회

현재 등록된 카페 메뉴의 ID, 이름과 가격을 조회한다.

### 요청

```http
GET /api/v1/menus?page=0&size=2
```

요청 본문은 없다.

| 쿼리 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | int | X | `0` | 조회할 페이지 번호. 0부터 시작하며 0 이상 |
| `size` | int | X | `10` | 한 페이지의 메뉴 수. 1 이상 100 이하 |

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "menuId": 1,
        "name": "아메리카노",
        "price": 4500,
        "status": "AVAILABLE"
      },
      {
        "menuId": 2,
        "name": "아이스 아메리카노",
        "price": 4500,
        "status": "AVAILABLE"
      }
    ],
    "page": 0,
    "size": 2,
    "totalElements": 90,
    "totalPages": 45,
    "first": true,
    "last": false
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 성공 응답 코드 `SUCCESS` |
| `message` | string | O | 요청 처리 결과 설명 |
| `data.content` | array | O | 메뉴 목록. 조회할 메뉴가 없으면 빈 배열 |
| `data.content[].menuId` | long | O | 메뉴 식별값 |
| `data.content[].name` | string | O | 메뉴 이름 |
| `data.content[].price` | long | O | 메뉴 가격 |
| `data.content[].status` | string | O | 메뉴 상태. `AVAILABLE`은 판매중, `SOLD_OUT`은 품절 |
| `data.page` | int | O | 현재 페이지 번호 |
| `data.size` | int | O | 요청한 페이지 크기 |
| `data.totalElements` | long | O | 전체 메뉴 수 |
| `data.totalPages` | int | O | 전체 페이지 수 |
| `data.first` | boolean | O | 첫 페이지 여부 |
| `data.last` | boolean | O | 마지막 페이지 여부 |

판매중(`AVAILABLE`)과 품절(`SOLD_OUT`) 메뉴만 메뉴 ID 오름차순으로 반환한다.
단종(`DISCONTINUED`) 메뉴는 목록과 페이지 전체 개수에서 제외한다.

`page`가 0보다 작거나 `size`가 1 미만 또는 100을 초과하면
`INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.

## 2. 인기 메뉴 목록 조회

요청 시점을 기준으로 직전 7일 동안 결제된 주문 항목 수량이 많은 메뉴를 최대 3개 조회한다.
인기 메뉴 집계 데이터는 결제 완료 시 Redis에 기록된 주문 항목을 기준으로 조회한다.

### 요청

```http
GET /api/v1/menus/popular
```

요청 본문과 쿼리 파라미터는 없다.

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "from": "2026-07-09T15:00:00+09:00",
    "to": "2026-07-16T15:00:00+09:00",
    "menus": [
      {
        "rank": 1,
        "menuId": 2,
        "name": "카페라테",
        "price": 5000
      },
      {
        "rank": 2,
        "menuId": 1,
        "name": "아메리카노",
        "price": 4500
      }
    ]
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 성공 응답 코드 `SUCCESS` |
| `message` | string | O | 요청 처리 결과 설명 |
| `data.from` | string | O | 집계 시작 시각, 포함 |
| `data.to` | string | O | 집계 종료 시각, 미포함 |
| `data.menus` | array | O | 인기 메뉴 목록. 주문이 없으면 빈 배열 |
| `data.menus[].rank` | int | O | 응답 내 순위, 1부터 시작 |
| `data.menus[].menuId` | long | O | 메뉴 식별값 |
| `data.menus[].name` | string | O | 현재 메뉴 이름 |
| `data.menus[].price` | long | O | 현재 메뉴 가격 |

### 집계 규칙

- 서버가 요청 처리 중 `to`를 한 번 정하고 `from = to - 7일`로 계산한다.
- 결제 완료 시 Redis에 기록된 주문 항목 중 `paidAt >= from AND paidAt < to` 범위만 센다.
- 주문 항목의 `quantity` 합계를 메뉴별 주문 수로 센다.
- 주문 수 내림차순으로 정렬한다.
- 주문 수가 같으면 메뉴 ID 오름차순으로 정렬한다.
- 정렬 결과 중 최대 3개를 반환한다.
- 응답의 메뉴 이름과 가격은 현재 메뉴 정보를 사용한다.

`rank`는 동률 공동 순위가 아니라 최종 정렬 결과에서의 위치다. 따라서 항상 1, 2, 3 순서다.

## 3. 관리자 메뉴 생성

관리자가 새 메뉴를 등록한다. 생성된 메뉴의 기본 상태는 판매중(`AVAILABLE`)이다.

### 요청

```http
POST /api/v1/admin/menus
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "바닐라 콜드브루",
  "price": 5800
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | string | O | 메뉴 이름. 공백만 입력할 수 없으며 100자 이하 |
| `price` | long | O | 메뉴 가격. 1 이상 |

### 성공 응답

`201 Created`

```http
Location: /api/v1/admin/menus/101
```

```json
{
  "code": "SUCCESS",
  "message": "메뉴 생성이 완료되었습니다.",
  "data": {
    "menuId": 101,
    "name": "바닐라 콜드브루",
    "price": 5800,
    "status": "AVAILABLE"
  }
}
```

### 오류

- 인증하지 않으면 `AUTHENTICATION_REQUIRED` 오류와 함께 `401 Unauthorized`를 반환한다.
- 일반 회원이 요청하면 `FORBIDDEN` 오류와 함께 `403 Forbidden`을 반환한다.
- `name`이 비어 있거나 100자를 초과하면 `INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.
- `price`가 없거나 1보다 작으면 `INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.

## 4. 관리자 메뉴 상태 변경

관리자가 메뉴 상태를 판매중, 품절 또는 단종으로 변경한다.

### 요청

```http
PATCH /api/v1/admin/menus/1/status
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "status": "DISCONTINUED"
}
```

| 경로 변수 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `menuId` | long | O | 상태를 변경할 메뉴 식별값 |

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | O | 변경할 메뉴 상태. `AVAILABLE`, `SOLD_OUT`, `DISCONTINUED` 중 하나 |

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "메뉴 상태 변경이 완료되었습니다.",
  "data": {
    "menuId": 1,
    "name": "아메리카노",
    "price": 4500,
    "status": "DISCONTINUED"
  }
}
```

### 오류

- 인증하지 않으면 `AUTHENTICATION_REQUIRED` 오류와 함께 `401 Unauthorized`를 반환한다.
- 일반 회원이 요청하면 `FORBIDDEN` 오류와 함께 `403 Forbidden`을 반환한다.
- 존재하지 않는 메뉴이면 `MENU_NOT_FOUND` 오류와 함께 `404 Not Found`를 반환한다.
- `status`가 없거나 허용된 상태값이 아니면 `INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.

## 5. 관리자 메뉴 목록 조회

관리자가 단종 메뉴를 포함해 전체 메뉴를 조회한다.

### 요청

```http
GET /api/v1/admin/menus?page=0&size=100&status=DISCONTINUED
Authorization: Bearer {accessToken}
```

요청 본문은 없다.

| 쿼리 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | int | X | `0` | 조회할 페이지 번호. 0부터 시작하며 0 이상 |
| `size` | int | X | `10` | 한 페이지의 메뉴 수. 1 이상 100 이하 |
| `status` | string | X | 없음 | 조회할 메뉴 상태. `AVAILABLE`, `SOLD_OUT`, `DISCONTINUED` 중 하나 |

### 성공 응답

`200 OK`

```json
{
  "code": "SUCCESS",
  "message": "관리자 메뉴 목록 조회가 완료되었습니다.",
  "data": {
    "content": [
      {
        "menuId": 1,
        "name": "아메리카노",
        "price": 4500,
        "status": "AVAILABLE",
        "orderCount": 12
      },
      {
        "menuId": 91,
        "name": "단종 메뉴",
        "price": 4500,
        "status": "DISCONTINUED",
        "orderCount": 3
      }
    ],
    "page": 0,
    "size": 100,
    "totalElements": 100,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

`status`가 없으면 판매중(`AVAILABLE`), 품절(`SOLD_OUT`), 단종(`DISCONTINUED`) 메뉴 전체를 메뉴 ID 오름차순으로 반환한다.
`status`가 있으면 해당 상태의 메뉴만 메뉴 ID 오름차순으로 반환한다.
`orderCount`는 해당 메뉴가 주문 항목에 포함된 전체 수량이며, 주문이 없으면 `0`이다.

### 오류

- 인증하지 않으면 `AUTHENTICATION_REQUIRED` 오류와 함께 `401 Unauthorized`를 반환한다.
- 일반 회원이 요청하면 `FORBIDDEN` 오류와 함께 `403 Forbidden`을 반환한다.
- `page`가 0보다 작거나 `size`가 1 미만 또는 100을 초과하면 `INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.
- `status`가 허용된 상태값이 아니면 `INVALID_REQUEST` 오류와 함께 `400 Bad Request`를 반환한다.
