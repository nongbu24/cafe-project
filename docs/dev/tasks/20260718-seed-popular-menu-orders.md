# 인기 메뉴 확인용 초기 주문 데이터 추가

## 목표

- 로컬 H2 초기 데이터에서 인기 메뉴 API가 빈 배열만 반환하지 않도록 최근 결제 주문 데이터를 추가한다.
- 판매중 메뉴 2개와 품절 메뉴 1개가 인기 메뉴 응답에 보이게 한다.

## 범위

- `src/main/resources/data.sql`의 로컬 확인용 주문 데이터

## 검증 계획

- `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'`
- `git diff --check`

## 구현 결과

- `data.sql`에 최근 7일 안의 `PAID` 주문 데이터를 추가했다.
- 판매중 메뉴는 `아이스 아메리카노` 7건, `아메리카노` 5건으로 넣었다.
- 품절 메뉴는 `민트초콜릿 프라페` 3건으로 넣었다.
- 인기 메뉴 집계가 UTC `LocalDateTime` 기준으로 동작하므로, H2 `CURRENT_TIMESTAMP`로 만든 주문 시각을
  UTC 기준 최근 시각에 들어오도록 보정했다.

## 검증 결과

- `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'`: 성공
- `git diff --check`: 성공
- `curl -s http://127.0.0.1:8080/api/v1/menus/popular`: `아이스 아메리카노` 7건, `아메리카노` 5건,
  `민트초콜릿 프라페` 3건 반환 확인
