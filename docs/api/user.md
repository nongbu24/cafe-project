# 회원 API

회원 정보 조회와 회원 본인 관리 계약을 정의한다.

## 1. 내 정보 조회

```http
GET /api/v1/users/me
Authorization: Bearer {accessToken}
```

인증된 회원이 자신의 정보를 조회할 수 있다.

성공하면 `200 OK`와 회원 정보를 반환한다. 비밀번호와 탈퇴 여부는 반환하지 않는다.

```json
{
  "code": "SUCCESS",
  "message": "내 정보 조회가 완료되었습니다.",
  "data": {
    "userId": 4,
    "username": "user1",
    "userStatus": "USER",
    "pointBalance": 0,
    "createdAt": "2026-07-19T12:00:00+09:00",
    "updatedAt": null
  }
}
```

## 2. 비밀번호 변경

```http
PATCH /api/v1/users/me/password
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "currentPassword": "Cafe1234!",
  "newPassword": "NewCafe1234!"
}
```

인증된 회원이 현재 비밀번호를 확인한 뒤 새 비밀번호로 변경할 수 있다.

- `currentPassword`와 `newPassword`는 8자 이상 64자 이하이며 공백 없이 영문 대·소문자, 숫자와 일반 ASCII 특수문자만 사용할 수 있다.
- 새 비밀번호는 BCrypt로 암호화하여 저장한다.

성공하면 `200 OK`와 비밀번호 변경 완료 메시지를 반환한다. 반환할 데이터가 없으므로 `data` 필드는 포함하지 않는다.

```json
{
  "code": "SUCCESS",
  "message": "비밀번호 변경이 완료되었습니다."
}
```

## 3. 관리자 회원 목록 조회

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

## 4. 관리자 회원 단건 조회

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
| `400 Bad Request` | `INVALID_REQUEST` | `page`, `size` 또는 비밀번호 요청 값이 허용 범위를 벗어남 |
| `401 Unauthorized` | `AUTHENTICATION_REQUIRED` | 인증 토큰 없이 요청함 |
| `401 Unauthorized` | `INVALID_TOKEN` | 토큰이 유효하지 않거나 탈퇴 회원의 토큰으로 요청함 |
| `401 Unauthorized` | `INVALID_PASSWORD` | 비밀번호 변경 시 현재 비밀번호가 일치하지 않음 |
| `403 Forbidden` | `FORBIDDEN` | 일반 회원이 관리자 API를 요청함 |
| `404 Not Found` | `USER_NOT_FOUND` | 단건 조회 대상 회원이 존재하지 않음 |
