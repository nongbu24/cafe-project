# 관리자 회원 조회 API 추가

## 목표

관리자가 회원 도메인에서 회원 목록과 단건 정보를 조회할 수 있는 REST API를 추가한다.

## 범위

- 관리자 전용 회원 목록 조회 API 추가
- 관리자 전용 회원 단건 조회 API 추가
- 일반 회원 접근 시 권한 오류 응답 추가
- API 계약 문서와 통합 테스트 추가

## 검증

- `./gradlew test --tests 'com.example.cafe.user.controller.AdminUserControllerTest'` 성공
- `./gradlew test` 성공

## 구현 기록

- 관리자 회원 목록 조회 API는 `GET /api/v1/admin/users`로 추가했다.
- 관리자 회원 단건 조회 API는 `GET /api/v1/admin/users/{userId}`로 추가했다.
- 목록 응답은 `userId`, `username`, `userStatus`만 포함한다.
- 단건 응답은 포인트 잔액, 탈퇴 여부, 생성·수정 시각을 함께 포함한다.
- 일반 회원이 관리자 API를 호출하면 `403 FORBIDDEN`을 반환한다.
- 목록 조회 성공 메시지는 `회원 목록 조회가 완료되었습니다.`로 명시했다.
- 단건 조회 성공 메시지는 `회원 조회가 완료되었습니다.`로 명시했다.
