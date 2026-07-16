# 메뉴 API

메뉴 도메인의 REST API 계약이다.
공통 규칙과 오류 형식은 [`README.md`](README.md)를 따른다.

## 1. 커피 메뉴 목록 조회

현재 등록된 커피 메뉴의 ID, 이름과 가격을 조회한다.

### 요청

```http
GET /api/v1/menus
```

요청 본문과 쿼리 파라미터는 없다.

### 성공 응답

`200 OK`

```json
{
  "menus": [
    {
      "menuId": 1,
      "name": "아메리카노",
      "price": 4500
    },
    {
      "menuId": 2,
      "name": "카페라테",
      "price": 5000
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `menus` | array | O | 메뉴 목록. 등록된 메뉴가 없으면 빈 배열 |
| `menus[].menuId` | long | O | 메뉴 식별값 |
| `menus[].name` | string | O | 메뉴 이름 |
| `menus[].price` | long | O | 메뉴 가격 |

메뉴 ID 오름차순으로 반환한다.

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
  "from": "2026-07-09T15:00:00+09:00",
  "to": "2026-07-16T15:00:00+09:00",
  "menus": [
    {
      "rank": 1,
      "menuId": 2,
      "name": "카페라테",
      "price": 5000,
      "orderCount": 18
    },
    {
      "rank": 2,
      "menuId": 1,
      "name": "아메리카노",
      "price": 4500,
      "orderCount": 12
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | O | 집계 시작 시각, 포함 |
| `to` | string | O | 집계 종료 시각, 미포함 |
| `menus` | array | O | 인기 메뉴 목록. 주문이 없으면 빈 배열 |
| `menus[].rank` | int | O | 응답 내 순위, 1부터 시작 |
| `menus[].menuId` | long | O | 메뉴 식별값 |
| `menus[].name` | string | O | 현재 메뉴 이름 |
| `menus[].price` | long | O | 현재 메뉴 가격 |
| `menus[].orderCount` | long | O | 집계 구간 내 결제 완료 주문 횟수 |

### 집계 규칙

- 서버가 요청 처리 중 `to`를 한 번 정하고 `from = to - 7일`로 계산한다.
- `paidAt >= from AND paidAt < to`이고 상태가 `PAID`인 주문만 센다.
- 주문 행 한 건을 메뉴 주문 1회로 센다.
- 주문 횟수 내림차순으로 정렬한다.
- 주문 횟수가 같으면 메뉴 ID 오름차순으로 정렬한다.
- 정렬 결과 중 최대 3개를 반환한다.

`rank`는 동률 공동 순위가 아니라 최종 정렬 결과에서의 위치다. 따라서 항상 1, 2, 3 순서다.
