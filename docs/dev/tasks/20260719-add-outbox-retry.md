# Outbox 재전송 로직 추가

## 작업 목적

주문 결제 후 생성되는 `ORDER_PAID` Outbox 이벤트가 최초 전송에 실패해도 주문과 결제는 유지하고, 이후 정해진 시간에 다시 전송되도록 한다.

## 변경 범위와 계획

- 변경할 영역: 주문 Outbox 엔티티, 저장소 조회, 전송 서비스, 재전송 스케줄러, 관련 테스트와 문서
- 변경하지 않을 영역: 주문/결제 API 요청·응답 형식, DB 마이그레이션 구조, 실제 외부 데이터 수집 플랫폼 연동
- 구현 방향: 최초 전송과 재전송이 공통 전송 로직을 사용하게 하고, 실패 시 `retry_count`, `next_retry_at`, `FAILED` 상태를 관리한다.
- 검증 방법: 주문 컨트롤러 테스트 중 Outbox 관련 테스트를 실행한다.

## 변경 결과

- 핵심 변경: Outbox 전송 공통 컴포넌트를 추가하고, 최초 전송 실패 시 재시도 횟수와 다음 재시도 시각을 저장하도록 했다. 재시도 시각이 지난 `PENDING` 이벤트를 스케줄러가 다시 전송하며, 3회 실패한 이벤트는 `FAILED`로 종료한다.
- 변경 파일: `CafeApplication`, `OrderEventOutbox`, `OrderEventOutboxRepository`, `MockOrderEventPublisher`, `OrderOutboxSender`, `OrderOutboxRetryService`, `OrderControllerTest`, `docs/api/order.md`, `docs/db/ERD.md`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest'` 성공
- 실행하지 못한 검증: 전체 테스트는 실행하지 않음
- 남은 제한사항과 사용자 조치: 현재 전송 대상은 실제 외부 플랫폼이 아니라 기존 mock 전송 지점이다.
