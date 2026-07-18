# 회원탈퇴 HTTP 메서드 PATCH 변경

## 목적

- 회원탈퇴가 회원 행 삭제가 아니라 `users.is_deleted` 상태 변경이라는 점을 API 계약에 드러낸다.

## 변경

- 회원탈퇴 엔드포인트를 `DELETE /api/v1/users/me`에서 `PATCH /api/v1/users/me`로 변경한다.
- 회원탈퇴 컨트롤러 테스트와 인증 API 문서를 함께 갱신한다.

## 검증

- `./gradlew test --tests 'com.example.cafe.auth.*'`: 성공
