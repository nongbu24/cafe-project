# 카페 REST API

이 디렉터리는 카페 서비스 REST API 계약의 SSOT(Single Source of Truth)다.
실제 Controller, DTO와 API 테스트를 추가할 때 관련 도메인 문서와 함께 갱신한다.

## 도메인별 API

| 도메인 | 문서 | 포함 API |
|---|---|---|
| 메뉴 | [`menu.md`](menu.md) | 커피 메뉴 목록, 최근 7일 인기 메뉴 목록 |
| 포인트 | [`point.md`](point.md) | 포인트 충전 |
| 주문 | [`order.md`](order.md) | 커피 주문·결제, 데이터 수집 플랫폼 전송 |

## 공통 규칙

- 기본 경로: `/api/v1`
- 요청과 응답 형식: `application/json`
- 금액 단위: 원
- 포인트 환산: `1원 = 1P`
- 금액과 포인트의 Java 타입: `long`
- 금액과 포인트의 DB 타입: `BIGINT`
- 날짜·시각 형식: 시간대 정보를 포함한 ISO 8601 문자열
- 존재하는 회원과 메뉴만 사용하며, 회원 가입과 메뉴 관리 API는 현재 범위에 포함하지 않는다.

금액과 포인트는 정수만 허용한다. 소수, 0과 음수 충전은 허용하지 않는다.

## 공통 오류 응답

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 클라이언트가 분기 처리할 수 있는 오류 코드 |
| `message` | string | O | 사용자가 이해할 수 있는 오류 설명 |

요청 필드 검증에 실패하면 다음처럼 `fieldErrors`를 추가할 수 있다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": [
    {
      "field": "amount",
      "reason": "1 이상의 정수여야 합니다."
    }
  ]
}
```
