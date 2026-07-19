# 장바구니 수량 변경과 비우기 API 추가

## 작업 목적

회원이 장바구니 항목 수량을 변경하거나 장바구니 전체를 비울 수 있게 한다.

## 변경 범위와 계획

- 변경할 영역: 장바구니 controller/service/entity/repository, REST API 문서, 테스트
- 변경하지 않을 영역: 장바구니 조회 API, 주문 취소와 환불, 운영 데이터 변경
- 구현 방향: `PATCH /api/v1/carts/me/items/{menuId}`에서 `quantity >= 1`이면 담기 또는 수량 변경, `quantity = 0`이면 항목 삭제로 처리한다. `DELETE /api/v1/carts/me/items`는 전체 항목을 삭제한다.
- 검증 방법: 장바구니 컨트롤러 테스트와 주문 컨트롤러 테스트를 실행하고, 필요하면 전체 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 장바구니 항목 수량 변경 API와 전체 비우기 API를 추가했다. `quantity`가 1 이상이면 항목을 담거나 수량을 변경하고, 0이면 해당 항목을 삭제한다.
- 변경 파일: `cart` controller/service/dto/entity/repository/facade, 인증 인터셉터 경로 설정, 장바구니 API 문서, ERD, 프로젝트 프로필, 장바구니 테스트

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew compileTestJava` 성공, `./gradlew test --tests 'com.example.cafe.cart.controller.CartControllerTest' --tests 'com.example.cafe.order.controller.OrderControllerTest'` 성공, `./gradlew test` 성공, `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 장바구니 조회 API는 이번 범위에 포함하지 않았다.
