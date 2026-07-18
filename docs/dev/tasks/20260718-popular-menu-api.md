# 인기 메뉴 목록 조회 API

## 작업 목적

최근 7일 동안 결제 완료된 주문을 기준으로 주문 횟수가 많은 메뉴 최대 3개를 조회하는 API를 구현한다.

## 변경 범위와 계획

- 변경할 영역: 메뉴 조회 API, 주문 집계 리포지토리, 메뉴 응답 DTO, API 테스트
- 변경하지 않을 영역: 주문 생성 정책, 포인트 결제 정책, 인증 정책
- 구현 방향: 요청 처리 시점의 종료 시각을 한 번 정하고, 직전 7일 동안 `PAID` 주문을 메뉴별로 집계해 주문 횟수 내림차순과 메뉴 ID 오름차순으로 최대 3개를 반환한다.
- 검증 방법: 인기 메뉴 API 테스트와 관련 메뉴 컨트롤러 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `GET /api/v1/menus/popular` 엔드포인트를 추가하고, 최근 7일 `PAID` 주문을 메뉴별로 집계해 최대 3개 메뉴를 반환한다.
- 변경 파일:
  - `src/main/java/com/example/cafe/menu/controller/MenuController.java`
  - `src/main/java/com/example/cafe/menu/service/MenuService.java`
  - `src/main/java/com/example/cafe/menu/dto/PopularMenuResponse.java`
  - `src/main/java/com/example/cafe/menu/dto/PopularMenuItemResponse.java`
  - `src/main/java/com/example/cafe/menu/dto/PopularMenuOrderCount.java`
  - `src/main/java/com/example/cafe/order/repository/OrderRepository.java`
  - `src/test/java/com/example/cafe/menu/controller/MenuControllerTest.java`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'` 성공
  - `./gradlew test` 성공
  - `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
