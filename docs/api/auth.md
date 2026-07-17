# 회원 인증 API

회원가입, 로그인, 로그아웃과 회원탈퇴 계약을 정의한다.

## 1. 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "username": "cafe_user",
  "password": "Cafe1234!"
}
```

- `username`은 영문, 숫자와 밑줄로 구성된 4자 이상 50자 이하의 고유 값이다.
- `password`는 8자 이상 64자 이하이며 공백 없이 영문 대·소문자, 숫자와 일반 ASCII 특수문자만 사용할 수 있다. BCrypt로 암호화하여 저장한다.
- API로 가입한 회원의 `userStatus`는 항상 `USER`다.

성공하면 `201 Created`와 생성된 회원 정보를 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "userId": 11,
    "username": "cafe_user",
    "userStatus": "USER"
  }
}
```

## 2. 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

회원가입과 같은 `username`, `password` 형식으로 요청한다. 성공하면 유효기간이 1시간인 JWT를 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "JWT"
  }
}
```

JWT 만료 시각은 토큰 내부의 `exp` 클레임으로 관리하며 로그인 응답에는 별도로 반환하지 않는다.

탈퇴 회원 또는 일치하지 않는 인증 정보에는 `401 INVALID_CREDENTIALS`를 반환한다.

## 3. 로그아웃

```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

JWT의 고유 식별값인 `jti`를 Redis의 `auth:blacklist:{jti}` 키로 저장한다.
키의 TTL은 JWT의 남은 유효시간이며, 블랙리스트에 등록된 토큰은 만료 전이라도 다시 사용할 수 없다.

## 4. 회원탈퇴

```http
DELETE /api/v1/users/me
Authorization: Bearer {accessToken}
```

회원 행을 삭제하지 않고 `users.is_deleted`를 `true`로 변경한다.
현재 요청에 사용한 JWT도 블랙리스트에 등록하며, 탈퇴 회원이 발급받았던 다른 JWT도 사용자 상태 검사에서 거부한다.

## 5. 토큰 설정

- JWT 서명 알고리즘: `HS256`
- JWT 유효기간 기본값: 3,600초
- 운영 환경은 32바이트 이상의 키를 Base64로 인코딩하여 `JWT_SECRET` 환경 변수로 제공한다.
- `JWT_SECRET`이 없으면 실행할 때 임시 키를 생성하므로 애플리케이션을 재시작하면 기존 JWT가 무효화된다.
- Redis 연결 정보는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 환경 변수로 제공한다.
- 로컬 `.env` 파일은 Spring Boot가 자동으로 읽지 않으므로 IDE, 셸 또는 컨테이너 실행 설정에서
  `.env.example`의 값을 환경 변수로 전달해야 한다.
