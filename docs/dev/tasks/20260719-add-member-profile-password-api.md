# 회원 내 정보와 비밀번호 변경 API 추가

## 작업 목적

회원이 인증 토큰으로 자신의 정보를 조회하고, 현재 비밀번호 확인 후 새 비밀번호로 변경할 수 있는 API를 추가한다.

## 변경 범위와 계획

- 변경할 영역: 회원 컨트롤러, 회원 서비스, 회원 DTO, 인증 인터셉터 경로, 회원 API 문서, 컨트롤러 테스트
- 변경하지 않을 영역: DB 구조, 인증 토큰 발급 방식, 회원가입/로그인 API 계약
- 구현 방향: 기존 `ApiResponse`와 `ApplicationException`/`ErrorCode` 구조를 유지하고, 비밀번호 정책은 회원가입과 같은 규칙을 적용한다.
- 검증 방법: 회원 컨트롤러 테스트와 관련 인증 컨트롤러 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 회원 내 정보 조회 API와 비밀번호 변경 API를 추가하고, 비밀번호 변경 시 현재 비밀번호 검증과 BCrypt 암호화를 적용했다. 현재 비밀번호가 일치하지 않으면 `INVALID_PASSWORD`를 반환한다.
- 변경 파일: `UserController`, `UserService`, `User`, 회원 DTO, `WebConfig`, `UserControllerTest`, `docs/api/user.md`, `docs/api/README.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test --tests 'com.example.cafe.user.controller.UserControllerTest' --tests 'com.example.cafe.auth.controller.AuthControllerTest'`: 성공
  - `./gradlew test`: 성공
  - `./gradlew test --tests 'com.example.cafe.user.controller.UserControllerTest'`: 성공
  - `git diff --check`: 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
