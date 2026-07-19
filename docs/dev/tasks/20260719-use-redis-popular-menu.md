# 인기 메뉴 조회 Redis 전환

## 1. 작업 목적

인기 메뉴 조회를 주문 DB 집계 대신 Redis 기반 집계로 변경한다.

## 2. 변경 범위와 계획

- 승인: 사용자가 "좋아!"라고 승인했다.
- 결제 완료된 주문 항목을 Redis에 기록한다.
- 인기 메뉴 조회는 Redis에 저장된 최근 7일 주문 항목을 기준으로 메뉴별 수량을 합산한다.
- 응답 계약은 유지하고, 메뉴 이름과 가격은 현재 `menus` 테이블 값을 사용한다.

## 3. 변경 결과

- `PopularMenuStore` 계약과 `RedisPopularMenuStore` 구현을 추가했다.
- 결제 완료 주문 생성 후 메뉴별 주문 항목을 Redis ZSET에 기록하도록 연결했다.
- 인기 메뉴 조회는 Redis의 최근 7일 주문 항목을 메뉴별 수량으로 합산하고, 현재 메뉴 정보로 응답한다.
- 기존 인기 메뉴 DB 집계 Repository 메서드와 DTO를 제거했다.
- API 문서, ERD 문서와 README의 인기 메뉴 저장소 설명을 Redis 기준으로 갱신했다.

## 4. 검증과 남은 사항

- `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest' --tests 'com.example.cafe.menu.store.RedisPopularMenuStoreTest' --tests 'com.example.cafe.order.controller.OrderControllerTest'`: 성공
- `./gradlew test`: 성공
- 실제 Redis 서버 연결 검증은 자동 테스트 범위가 아니므로 실행하지 않았다.
