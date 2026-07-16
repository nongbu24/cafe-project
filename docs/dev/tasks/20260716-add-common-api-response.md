# 공통 API 응답과 페이지 응답 구현

## 작업 목적

API마다 응답 구조를 새로 만들지 않도록 성공·오류 응답과 페이지네이션 정보를 공통 형식으로 제공한다.

## 변경 범위와 계획

- 변경할 영역: 공통 응답 DTO, 공통 예외 처리, 메뉴 목록 응답, API 계약과 통합 테스트
- 변경하지 않을 영역: 메뉴 조회 조건, 초기 데이터, 주문·포인트 기능
- 구현 방향: 모든 응답에 `code`, `message`, `data`를 제공하고 페이지 응답은 `content`와 페이지 메타데이터를 공통화한다.
- 검증 방법: 메뉴 API 통합 테스트, 전체 테스트, `git diff --check`

## 변경 결과

- 핵심 변경:
  - 성공과 오류 응답에 `code`, `message`, `data`를 제공하는 `ApiResponse<T>`를 추가했다.
  - Spring Data의 `Page<T>`를 API에 직접 노출하지 않고 공통 `PageResponse<T>`로 변환하도록 구현했다.
  - 잘못된 페이지 범위와 쿼리 파라미터 타입을 `GlobalExceptionHandler`에서 공통 오류 응답으로 처리했다.
  - 메뉴 목록 API를 `ApiResponse<PageResponse<MenuResponse>>` 구조로 변경했다.
  - 메뉴·포인트·주문 API 계약의 성공 응답을 공통 응답 구조로 통일했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/common/`
  - `src/main/java/com/example/cafe/menu/MenuController.java`
  - `src/main/java/com/example/cafe/menu/MenuService.java`
  - `src/test/java/com/example/cafe/menu/MenuControllerTest.java`
  - `docs/api/README.md`
  - `docs/api/menu.md`
  - `docs/api/point.md`
  - `docs/api/order.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test --tests 'com.example.cafe.menu.MenuControllerTest'`: 성공
  - `./gradlew test`: 성공
  - `git diff --check`: 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 현재 성공 응답은 기본 코드 `SUCCESS`와 기본 메시지를 사용하며, API별 코드와 메시지가 필요하면 `ApiResponse.success(code, message, data)`를 사용할 수 있다.
