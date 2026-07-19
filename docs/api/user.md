# 회원 API

회원 정보 조회 계약을 정의한다.

## 1. 관리자 회원 목록 조회

```http
GET /api/v1/admin/users?page=0&size=10
Authorization: Bearer {accessToken}
```

관리자만 회원 목록을 페이지 단위로 조회할 수 있다.

- `page`는 0 이상이어야 한다.
- `size`는 1 이상 100 이하여야 한다.
- 목록 응답은 빠른 식별에 필요한 `userId`, `username`, `userStatus`만 포함한다.

성공하면 `200 OK`와 회원 목록을 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "회원 목록 조회가 완료되었습니다.",
  "data": {
    "content": [
      {
        "userId": 1,
        "username": "admin1",
        "userStatus": "ADMIN"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 10,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

## 2. 관리자 회원 단건 조회

```http
GET /api/v1/admin/users/{userId}
Authorization: Bearer {accessToken}
```

관리자만 특정 회원의 상세 정보를 조회할 수 있다.

성공하면 `200 OK`와 회원 정보를 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "회원 조회가 완료되었습니다.",
  "data": {
    "userId": 4,
    "username": "user1",
    "userStatus": "USER",
    "pointBalance": 0,
    "deleted": false,
    "createdAt": "2026-07-19T12:00:00+09:00",
    "updatedAt": null
  }
}
```

## 오류 응답

| HTTP 상태 | 오류 코드 | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `INVALID_REQUEST` | `page` 또는 `size`가 허용 범위를 벗어남 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | 인증 토큰 없이 요청함 |
| `401 Unauthorized` | `INVALID_TOKEN` | 토큰이 유효하지 않거나 탈퇴 회원의 토큰으로 요청함 |
| `403 Forbidden` | `FORBIDDEN` | 일반 회원이 관리자 API를 요청함 |
| `404 Not Found` | `USER_NOT_FOUND` | 단건 조회 대상 회원이 존재하지 않음 |
