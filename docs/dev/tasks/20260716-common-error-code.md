# 공통 오류 코드 구조 적용

## 작업 목적

오류 종류가 늘어나도 `GlobalExceptionHandler`가 도메인 예외별로 계속 커지지 않도록 오류 코드와 애플리케이션 예외 처리를 공통화한다.

## 변경 범위와 계획

- 변경할 영역: 공통 오류 코드, 공통 애플리케이션 예외, 전역 예외 처리, 기존 메뉴·포인트 검증 예외
- 변경하지 않을 영역: 공개 API 경로, 성공·오류 응답 형식, 기존 HTTP 상태와 오류 코드
- 구현 방향: `ErrorCode`가 상태·코드·기본 메시지를 관리하고, `ApplicationException` 하나를 전역 핸들러에서 공통 처리한다.
- 검증 방법: 포인트와 메뉴 API 테스트, 전체 테스트, 문서·공백 검사

## 변경 결과

- 핵심 변경:
  - HTTP 상태, 응답 코드와 기본 메시지를 `ErrorCode`에서 한곳에 관리하도록 변경했다.
  - `ApplicationException`이 오류 코드와 필요한 경우 상세 메시지를 전달하도록 구현했다.
  - `GlobalExceptionHandler`가 모든 애플리케이션 예외를 하나의 메서드에서 처리하도록 변경했다.
  - 메뉴와 포인트 검증 오류 및 회원 없음 오류를 공통 예외 구조로 전환했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/common/exception/ErrorCode.java`
  - `src/main/java/com/example/cafe/common/exception/ApplicationException.java`
  - `src/main/java/com/example/cafe/common/exception/GlobalExceptionHandler.java`
  - `src/main/java/com/example/cafe/menu/controller/MenuController.java`
  - `src/main/java/com/example/cafe/point/service/PointService.java`
  - `src/main/java/com/example/cafe/user/entity/User.java`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - 메뉴·포인트 API 대상 테스트: 성공, 15개 통과
  - `./gradlew test`: 성공, 전체 테스트 16개 통과
  - `git diff --check`: 성공, 공백 오류 없음
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
