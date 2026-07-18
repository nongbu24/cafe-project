# 커피 주문 결제 API 구현

## 작업 목적

사용자 식별값과 메뉴 ID로 커피를 주문하고 포인트로 결제하는 API를 구현한다.
주문 결제 내역은 데이터 수집 플랫폼으로 전송할 수 있도록 Outbox 이벤트와 Mock 전송 로직을 함께 추가한다.

## 변경 범위와 계획

- 변경할 영역: 주문 도메인, 포인트 결제 거래, 주문 이벤트 Outbox, 주문 API 테스트
- 변경하지 않을 영역: 메뉴 등록·수정, 주문 취소·환불, 실제 외부 데이터 수집 플랫폼 연동
- 구현 방향: 회원 포인트를 차감하고 주문·결제 거래·Outbox 이벤트를 하나의 트랜잭션에서 저장한 뒤, 커밋 이후 이벤트를 Mock 전송 컴포넌트로 전달한다.
- 검증 방법: 주문 컨트롤러 테스트와 관련 Gradle 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `POST /api/v1/orders` 주문 결제 API를 추가하고, 회원 포인트 차감·주문 저장·결제 포인트 거래 저장·주문 이벤트 Outbox 저장을 하나의 트랜잭션으로 처리했다. 트랜잭션 커밋 이후 `MockOrderDataCollector`로 주문 결제 이벤트를 전송하고 Outbox 상태를 `SENT`로 변경한다.
- 변경 파일: `src/main/java/com/example/cafe/order/**`, `src/test/java/com/example/cafe/order/**`, `src/main/java/com/example/cafe/user/entity/User.java`, `src/main/java/com/example/cafe/point/entity/PointTransaction.java`, `src/main/java/com/example/cafe/common/exception/ErrorCode.java`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest'` 성공, `./gradlew test` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 실제 외부 데이터 수집 플랫폼 연동은 현재 범위에 포함하지 않고 Mock 전송 컴포넌트로 대체했다.
