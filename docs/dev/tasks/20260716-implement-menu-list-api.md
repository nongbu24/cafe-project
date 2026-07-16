# 커피 메뉴 목록 조회 API 구현

## 작업 목적

커피 메뉴의 ID, 이름과 가격을 조회하는 API를 구현하고, 개발 환경에서 조회 기능을 확인할 수 있도록 카페 음료 데이터 100건을 제공한다.

## 변경 범위와 계획

- 변경할 영역: 메뉴 엔티티·Repository·Service·Controller·응답 DTO, H2 초기 데이터와 설정, API 통합 테스트
- 변경하지 않을 영역: 메뉴 등록·수정·삭제, 인기 메뉴, 주문·포인트 기능, 운영 PostgreSQL 데이터
- 구현 방향: `GET /api/v1/menus` 요청에 메뉴를 ID 오름차순으로 조회하고, H2에서만 초기 데이터 100건을 적재한다.
- 검증 방법: 메뉴 API 통합 테스트, 전체 테스트, `git diff --check`

## 변경 결과

- 핵심 변경:
  - 메뉴 엔티티와 JPA Repository를 추가했다.
  - 메뉴 ID 오름차순으로 조회하는 Service와 `GET /api/v1/menus` Controller를 구현했다.
  - H2 데이터베이스에서만 실행되는 `data.sql`에 카페 음료 100건을 추가했다.
  - API 응답의 메뉴 개수와 첫 번째·마지막 메뉴 정보를 확인하는 통합 테스트를 추가했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/menu/`
  - `src/main/resources/application.properties`
  - `src/main/resources/data.sql`
  - `src/test/java/com/example/cafe/menu/MenuControllerTest.java`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test`: 성공, 전체 테스트 2개 통과
  - `git diff --check`: 성공
  - 초기 데이터 행 수 확인: 100건
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 초기 데이터는 내장형 H2에서만 자동 적재되며 운영 PostgreSQL에는 적재되지 않는다.
