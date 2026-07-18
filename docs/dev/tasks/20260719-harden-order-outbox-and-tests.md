# 주문 Outbox 안정화와 테스트 보강

## 작업 목적

주문 도메인 코드 리뷰에서 확인한 Outbox 전송 실패 전파 문제와 인증 사용자 잠금 조회 오류 코드 불일치를 수정하고,
전송 실패와 동시 결제 시나리오를 테스트로 보강한다.

## 변경 범위와 계획

- 변경할 영역: 주문 서비스, 주문 Outbox 이벤트 리스너, 비동기 설정, 사용자 facade, 주문 API 테스트
- 변경하지 않을 영역: 실제 외부 데이터 수집 플랫폼 연동, Outbox 재시도 스케줄러, DB 구조
- 구현 방향: 주문 트랜잭션 커밋 후 이벤트 ID만 비동기로 전달하고, 리스너가 새 트랜잭션에서 Outbox를 재조회해 전송 성공 시에만 `SENT` 처리한다. 전송 실패는 주문 응답에 전파하지 않고 현재 구현 범위에 맞춰 `PENDING`으로 유지한다.
- 검증 방법: 주문 도메인 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 주문 커밋 후 Outbox 이벤트 ID만 비동기로 전달하고, 리스너가 별도 트랜잭션에서 Outbox를 재조회해 전송 성공 시에만 `SENT`로 변경하도록 수정했다. 전송 실패는 주문 응답으로 전파하지 않고 로그를 남기며 `PENDING` 상태를 유지한다. 주문 중 활성 회원 잠금 조회 실패는 주문 API 인증 계약과 맞게 `INVALID_TOKEN`으로 반환하도록 정렬했다. Outbox 전송 실패와 같은 회원 동시 결제 테스트를 추가했다.
- 변경 파일: `CafeApplication`, `OrderService`, `OrderPaidOutboxEvent`, `MockOrderEventPublisher`, `UserFacade`, `OrderControllerTest`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.*'` 성공, `./gradlew test` 성공, `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 실제 외부 데이터 수집 플랫폼 실패 재시도 스케줄러는 문서상 향후 범위라 이번 작업에서는 구현하지 않았다.
