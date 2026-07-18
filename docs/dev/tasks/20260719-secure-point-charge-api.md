# 포인트 충전 API 인증과 요청 검증 보강

## 목표

- 포인트 충전 API를 인증된 본인 기준 경로인 `/api/v1/users/me/point-charges`로 변경한다.
- 포인트 충전 요청 DTO를 Bean Validation 기반으로 검증한다.
- 인증 누락, 탈퇴 회원, 잘못된 요청 본문에 대한 테스트를 보강한다.

## 작업 계획

- 주문 도메인의 인증 주체 사용 방식과 테스트 패턴을 따른다.
- 포인트 충전 API 계약 문서를 새 경로와 인증 기준에 맞게 갱신한다.
- 관련 포인트 컨트롤러 테스트를 실행해 변경 결과를 확인한다.

## 작업 결과

- 포인트 충전 API 경로를 `/api/v1/users/me/point-charges`로 변경하고 인증 인터셉터 적용 대상에 추가했다.
- 컨트롤러가 path variable이 아니라 인증 인터셉터가 설정한 회원 id로 충전하도록 변경했다.
- 포인트 충전 서비스가 active user 잠금 조회를 사용하도록 변경해 탈퇴 회원 충전을 막았다.
- `PointChargeRequest.amount`를 Bean Validation으로 검증하고 검증 실패를 공통 `INVALID_REQUEST` 응답으로 처리했다.
- 포인트 API 문서와 컨트롤러 테스트를 새 계약에 맞게 갱신하고, 중복도가 높은 테스트는 정리했다.

## 검증

- `./gradlew test --tests 'com.example.cafe.point.controller.PointControllerTest'` 성공
- `./gradlew test` 성공
