# 포인트 충전 API 구현

## 작업 목적

사용자 식별값과 충전금액을 입력받아 1원당 1포인트를 충전하는 REST API를 구현한다.

## 변경 범위와 계획

- 변경할 영역: 회원·포인트 엔티티와 저장소, 충전 서비스와 API, 공통 오류 처리, API 테스트
- 변경하지 않을 영역: 회원 가입, 메뉴 주문과 포인트 결제, 운영 데이터베이스 설정
- 구현 방향: 회원 행을 비관적 쓰기 잠금으로 조회하고 잔액 갱신과 `CHARGE` 거래 저장을 하나의 트랜잭션으로 처리한다.
- 검증 방법: 포인트 충전 API 대상 테스트, 전체 테스트, 문서·공백 검사

## 변경 결과

- 핵심 변경:
  - `POST /api/v1/users/{userId}/point-charges` API를 추가했다.
  - 회원 행을 비관적 쓰기 잠금으로 조회하고 포인트 잔액을 증가시켰다.
  - 잔액 갱신과 `CHARGE` 포인트 거래 저장을 하나의 트랜잭션으로 처리했다.
  - 잘못된 요청은 `INVALID_REQUEST`, 존재하지 않는 회원은 `USER_NOT_FOUND`로 응답하도록 처리했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/user/`: 회원 엔티티와 저장소
  - `src/main/java/com/example/cafe/point/`: 충전 API, 서비스, 요청·응답, 포인트 거래 엔티티와 저장소
  - `src/main/java/com/example/cafe/common/exception/GlobalExceptionHandler.java`: 요청 본문과 회원 없음 오류 처리
  - `src/test/java/com/example/cafe/point/controller/PointControllerTest.java`: 포인트 충전 API 통합 테스트

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test --tests 'com.example.cafe.point.controller.PointControllerTest'`: 성공, 포인트 API 테스트 6개 통과
  - `./gradlew test`: 성공, 전체 테스트 16개 통과
  - `git diff --check`: 성공, 공백 오류 없음
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치:
  - 회원 가입 API는 현재 범위가 아니므로 포인트 충전 전에 회원 데이터가 존재해야 한다.
