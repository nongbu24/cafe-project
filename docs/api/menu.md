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

요청 시점을 기준으로 직전 7일 동안 결제된 주문 수가 많은 메뉴를 최대 3개 조회한다.

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
- `paidAt >= from AND paidAt < to`이고 상태가 `PAID`인 주문만 센다.
- 주문 행 한 건을 메뉴 주문 1회로 센다.
- 주문 횟수 내림차순으로 정렬한다.
- 주문 횟수가 같으면 메뉴 ID 오름차순으로 정렬한다.
- 정렬 결과 중 최대 3개를 반환한다.

`rank`는 동률 공동 순위가 아니라 최종 정렬 결과에서의 위치다. 따라서 항상 1, 2, 3 순서다.
