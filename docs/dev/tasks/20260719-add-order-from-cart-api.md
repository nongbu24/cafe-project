# 장바구니 주문 API 추가와 즉시 주문 단건 제한

## 작업 목적

즉시 주문 API는 메뉴 하나만 주문하도록 되돌리고, 장바구니에 담긴 항목으로 주문하는 API를 별도로 추가한다.

## 변경 범위와 계획

- 변경할 영역: 주문 controller/service/dto, 장바구니 facade/repository, 주문·장바구니 API 문서, 주문 테스트
- 변경하지 않을 영역: 장바구니 조회 API, 주문 취소와 환불, DB 테이블 구조
- 구현 방향: `POST /api/v1/orders`는 `menuId`, `quantity` 단건 요청만 받는다. `POST /api/v1/orders/from-cart`는 로그인 회원의 장바구니 항목 전체로 주문을 생성하고 성공 시 장바구니를 비운다.
- 검증 방법: 주문 컨트롤러 테스트, 장바구니 컨트롤러 테스트, 전체 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `POST /api/v1/orders`는 `menuId`, `quantity` 단건 즉시 주문만 받도록 변경했다. `POST /api/v1/orders/from-cart`를 추가해 장바구니 항목 전체로 주문하고 성공 시 장바구니를 비우도록 했다. 즉시 주문은 장바구니를 변경하지 않는다.
- 변경 파일: 주문 controller/service/dto, 장바구니 facade, 인증 인터셉터 경로 설정, 주문·장바구니 API 문서, ERD, 주문 테스트

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew compileTestJava` 성공, `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest' --tests 'com.example.cafe.cart.controller.CartControllerTest'` 성공, `./gradlew test` 성공, `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 장바구니 조회 API는 이번 범위에 포함하지 않았다.
