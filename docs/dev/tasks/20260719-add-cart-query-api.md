# 회원 장바구니 조회 API 추가

## 작업 목적

회원이 현재 장바구니에 담긴 메뉴, 수량과 총 금액을 조회할 수 있게 한다.

## 변경 범위와 계획

- 변경할 영역: 장바구니 controller/service/dto, 인증 인터셉터 경로 설정, 장바구니 API 문서, 장바구니 테스트
- 변경하지 않을 영역: 장바구니 DB 구조, 주문 취소와 환불
- 구현 방향: `GET /api/v1/carts/me`로 로그인 회원의 장바구니 항목 목록과 `totalAmount`를 반환한다. 장바구니가 없거나 비어 있으면 빈 배열과 0원을 반환한다.
- 검증 방법: 장바구니 컨트롤러 테스트와 전체 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `GET /api/v1/carts/me`를 추가해 로그인 회원의 장바구니 항목과 총 금액을 조회할 수 있게 했다. 장바구니가 비어 있으면 빈 배열과 `totalAmount` 0을 반환한다.
- 변경 파일: 장바구니 controller/service/dto, 인증 인터셉터 경로 설정, 장바구니 API 문서, 장바구니 테스트

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.cart.controller.CartControllerTest'` 성공, `./gradlew test` 성공, `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
