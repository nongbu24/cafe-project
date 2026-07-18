# 주문 불가 메뉴 결제 차단

## 목표

- 메뉴 상태가 주문 가능 여부를 뜻하므로 `AVAILABLE` 메뉴만 주문할 수 있게 한다.
- 품절(`SOLD_OUT`)과 단종(`DISCONTINUED`) 메뉴 주문을 오류로 막는다.
- 인기 메뉴 집계의 시간 경계 조건 테스트를 추가한다.

## 범위

- 메뉴 주문용 facade 조회 정책
- 주문 API 오류 코드와 문서 계약
- 주문 API 테스트와 인기 메뉴 집계 Repository 테스트

## 검증 계획

- `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest'`
- `./gradlew test --tests 'com.example.cafe.order.repository.OrderRepositoryImplTest'`
- 필요하면 관련 메뉴 컨트롤러 테스트까지 실행한다.

## 구현 결과

- 주문 서비스가 메뉴 조회 시 `AVAILABLE` 상태만 주문 가능한 메뉴로 사용하도록 변경했다.
- 품절 또는 단종 메뉴는 `MENU_NOT_AVAILABLE` 오류와 `409 Conflict`로 응답하게 했다.
- 주문 API 계약에 주문 불가 메뉴 오류를 추가했다.
- 품절/단종 메뉴 주문 차단 테스트와 인기 메뉴 집계 시간 경계 테스트를 추가했다.

## 검증 결과

- `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest' --tests 'com.example.cafe.order.repository.OrderRepositoryImplTest' --tests 'com.example.cafe.menu.controller.MenuControllerTest'`: 성공
- `git diff --check`: 성공
