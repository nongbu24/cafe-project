# 소비자용 인기 메뉴 주문 수 숨김

## 목표

- 소비자용 인기 메뉴 API 응답에서 내부 집계 수치인 `orderCount`를 제거한다.
- 코드 안에 `viewCount` 변수명이 있다면 `orderCount`로 정리한다.

## 확인 결과

- 현재 코드와 문서에는 `viewCount` 변수명이 없고 `orderCount`만 사용 중이다.

## 범위

- 인기 메뉴 응답 DTO
- 인기 메뉴 서비스 매핑
- 메뉴 API 문서
- 메뉴 컨트롤러 테스트

## 검증 계획

- `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'`
- `git diff --check`

## 구현 결과

- 소비자용 인기 메뉴 응답 DTO에서 `orderCount`를 제거했다.
- 내부 집계 DTO `PopularMenuOrderCount.orderCount`는 인기 메뉴 정렬과 순위 산정에 필요하므로 유지했다.
- 메뉴 API 문서의 인기 메뉴 응답 예시와 필드 표에서 `orderCount`를 제거했다.
- 컨트롤러 테스트에서 `orderCount`가 응답에 포함되지 않는지 검증하도록 변경했다.

## 검증 결과

- `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'`: 성공
- `git diff --check`: 성공
