# 카페 REST API

이 디렉터리는 카페 서비스 REST API 계약의 SSOT(Single Source of Truth)다.
실제 Controller, DTO와 API 테스트를 추가할 때 관련 도메인 문서와 함께 갱신한다.

## 도메인별 API

| 도메인 | 문서 | 포함 API |
|---|---|---|
| 회원 인증 | [`auth.md`](auth.md) | 회원가입, 로그인, 로그아웃, 회원탈퇴 |
| 메뉴 | [`menu.md`](menu.md) | 카페 메뉴 목록, 최근 7일 인기 메뉴 목록 |
| 포인트 | [`point.md`](point.md) | 포인트 충전 |
| 주문 | [`order.md`](order.md) | 메뉴 주문·결제, 데이터 수집 플랫폼 전송 |

## 공통 규칙

- 기본 경로: `/api/v1`
- 요청과 응답 형식: `application/json`
- 금액 단위: 원
- 포인트 환산: `1원 = 1P`
- 금액과 포인트의 Java 타입: `long`
- 금액과 포인트의 DB 타입: `BIGINT`
- 날짜·시각 형식: 시간대 정보를 포함한 ISO 8601 문자열
- 회원가입과 인증은 회원 인증 API 계약을 따르며 메뉴 관리 API는 현재 범위에 포함하지 않는다.

금액과 포인트는 정수만 허용한다. 소수, 0과 음수 충전은 허용하지 않는다.

## 공통 성공 응답

모든 성공 응답은 `code`, `message`, `data`를 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {}
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 클라이언트가 결과를 구분할 수 있는 응답 코드 |
| `message` | string | O | 요청 처리 결과에 대한 설명 |
| `data` | object, array 또는 null | O | API별 응답 데이터 |

목록을 페이지 단위로 반환하는 API의 `data`는 다음 공통 필드를 사용한다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `content` | array | O | 현재 페이지의 데이터 |
| `page` | int | O | 현재 페이지 번호 |
| `size` | int | O | 요청한 페이지 크기 |
| `totalElements` | long | O | 전체 데이터 수 |
| `totalPages` | int | O | 전체 페이지 수 |
| `first` | boolean | O | 첫 페이지 여부 |
| `last` | boolean | O | 마지막 페이지 여부 |

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

오류 응답에는 `data` 필드를 포함하지 않는다.

요청 값 검증에 실패하면 `INVALID_REQUEST` 코드와 구체적인 실패 이유를 반환한다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다."
}
```
