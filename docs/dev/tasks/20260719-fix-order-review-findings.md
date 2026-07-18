# 주문 도메인 리뷰 지적사항 수정

## 작업 목적

주문 도메인 코드 리뷰에서 확인한 탈퇴 회원 주문 가능성, Outbox 상태 값 불일치, 주문 테스트의 ID 고정 실패를 수정한다.

## 변경 범위와 계획

- 변경할 영역: 주문 서비스, 사용자 facade, 주문 Outbox enum, 주문 API 테스트, 주문 API 문서
- 변경하지 않을 영역: 인증 구조, DB 마이그레이션 구조, 실제 외부 데이터 수집 플랫폼 연동
- 구현 방향: 주문 시 활성 회원만 잠금 조회하고, Java enum을 DB/문서 상태 값과 맞추며, 테스트는 실제 생성 ID 기준으로 검증한다.
- 검증 방법: 주문 도메인 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 주문 시 탈퇴 회원을 제외한 활성 회원만 비관적 잠금으로 조회하도록 수정했다. Outbox 상태 enum에 DB/문서와 동일한 `SENDING` 값을 추가했다. 주문 테스트의 고정 ID 기대값을 실제 생성 ID 기준 검증으로 바꾸고, 탈퇴 회원 주문 거부 테스트를 추가했다. 전체 테스트 실행 중 초기 주문 데이터 때문에 `users` 삭제가 FK 제약에 걸리던 테스트 setup도 정리 순서에 맞게 보강했다.
- 변경 파일: `UserFacade`, `OrderService`, `OrderEventStatus`, `OrderControllerTest`, `AuthControllerTest`, `PointControllerTest`, `docs/api/order.md`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.*'` 성공, `./gradlew test` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
