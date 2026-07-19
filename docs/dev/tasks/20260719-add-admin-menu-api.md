# 관리자 메뉴 API 추가

## 작업 목적

관리자가 메뉴를 생성하고, 메뉴 상태를 품절·단종 등으로 변경하며, 단종 메뉴까지 포함해 전체 메뉴 목록을 조회할 수 있는 API를 추가한다.

## 변경 범위와 계획

- 변경할 영역: 메뉴 엔티티·DTO·서비스·컨트롤러, 인증 인터셉터 적용 경로, 메뉴 API 문서, 메뉴 컨트롤러 테스트
- 변경하지 않을 영역: DB 스키마, 주문 정책, 회원용 메뉴 목록 조회 정책
- 구현 방향: 기존 관리자 회원 API와 같은 인증·권한 확인 흐름을 사용하고, 관리자 메뉴 목록은 모든 상태를 ID 오름차순 페이지로 반환한다.
- 검증 방법: 관리자 메뉴 컨트롤러 테스트와 필요 시 전체 메뉴 컨트롤러 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 관리자 메뉴 생성, 상태 변경, 단종 포함 목록 조회 API를 추가하고 관리자 권한 검증과 요청값 검증을 적용했다. 관리자 메뉴 목록은 선택한 메뉴 상태로 필터링할 수 있고 각 메뉴의 전체 주문 수를 함께 반환한다. 회원용 메뉴 목록 조회는 기존처럼 단종 메뉴를 제외한다. 관리자 메뉴 테스트의 반복 요청 코드를 헬퍼로 정리하고, 단종 처리 후 회원 목록 제외와 빈 메뉴명 검증을 추가했다.
- 변경 파일: 메뉴 엔티티·DTO·서비스·컨트롤러, 인증 인터셉터 경로 설정, 관리자 메뉴 컨트롤러 테스트, 메뉴 API 문서

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.menu.controller.AdminMenuControllerTest' --tests 'com.example.cafe.menu.controller.MenuControllerTest'` 성공, `./gradlew test --tests 'com.example.cafe.menu.controller.AdminMenuControllerTest' --tests 'com.example.cafe.user.controller.AdminUserControllerTest'` 성공, `./gradlew test --tests 'com.example.cafe.menu.controller.AdminMenuControllerTest' --tests 'com.example.cafe.menu.controller.MenuControllerTest' --tests 'com.example.cafe.order.repository.OrderRepositoryImplTest'` 성공
- 실행하지 못한 검증: 전체 테스트는 실행하지 않음
- 남은 제한사항과 사용자 조치: 없음
