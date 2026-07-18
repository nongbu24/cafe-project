# 문서, 스키마, 시간 기준 정합성 정리

## 작업 목적

문서와 현재 구현의 차이를 줄이고, DB 제약은 마이그레이션으로 관리하며, Outbox payload와 시각 저장 기준을 명확히 한다.

## 변경 범위와 계획

- 변경할 영역: 프로젝트/API/ERD 문서, Gradle 의존성, 애플리케이션 설정, DB 마이그레이션, UTC 시각 유틸, Outbox payload 생성과 전송
- 변경하지 않을 영역: 운영 배포 설정, 실제 외부 데이터 수집 플랫폼 연동, Outbox 재시도 구현
- 구현 방향:
  - 현재 구현 상태와 인증 오류 응답을 문서에 반영한다.
  - Outbox 재시도는 향후 구현으로 낮춰 문서화한다.
  - Flyway 마이그레이션으로 테이블, 기본값, CHECK, FK, 인덱스를 관리한다.
  - DB 저장 시각은 UTC `LocalDateTime`으로 생성하고 API 응답에서 KST offset으로 변환한다.
  - 저장된 Outbox payload가 외부 전송 원본 JSON이 되도록 `eventId`, `occurredAt`을 포함한다.
- 검증 방법: 관련 컨트롤러 테스트와 전체 테스트, 문서 공백 검사

## 변경 결과

- 핵심 변경:
  - 프로젝트 프로필과 API 문서를 현재 구현 상태에 맞게 갱신했다.
  - 주문 Outbox 재시도는 향후 구현 범위로 문서화하고, 현재 구현은 mock 즉시 전송과 `SENT` 처리로 명시했다.
  - Flyway 마이그레이션을 도입하고 Hibernate는 `validate`로 전환해 DB 테이블, 기본값, CHECK, FK와 인덱스를 마이그레이션으로 관리하게 했다.
  - `point_transaction.order_id`는 단순 참조값 의도에 맞춰 ERD에서 FK 표현을 제거했다.
  - DB 저장 시각은 UTC 기준 `LocalDateTime`으로 생성하고, API 응답은 한국 시간 offset으로 변환하도록 정리했다.
  - 모든 도메인의 `updated_at`은 실제 수정 전까지 `NULL`로 유지하도록 `BaseEntity`, 마이그레이션과 초기 데이터를 정리했다.
  - `created_at`, `updated_at`을 함께 사용하는 `Menu` 엔티티가 `BaseEntity`를 상속하도록 정리했다.
  - Outbox 저장 payload에 `eventId`, `occurredAt`을 포함하고, mock publisher가 저장 payload를 전송 원본으로 읽도록 변경했다.
- 변경 파일:
  - `build.gradle`
  - `src/main/resources/application.properties`
  - `src/main/resources/db/migration/V1__create_cafe_schema.sql`
  - `src/main/java/com/example/cafe/common/**`
  - `src/main/java/com/example/cafe/menu/**`
  - `src/main/java/com/example/cafe/order/**`
  - `src/main/java/com/example/cafe/point/service/PointService.java`
  - `src/test/java/com/example/cafe/order/controller/OrderControllerTest.java`
  - `docs/project-profile.md`, `docs/api/auth.md`, `docs/api/order.md`, `docs/db/ERD.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest'`: 통과
  - `./gradlew test --tests 'com.example.cafe.menu.controller.MenuControllerTest'`: 통과
  - `./gradlew test`: 통과
  - `./gradlew check`: 통과
  - `git diff --check`: 통과
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치:
  - Outbox 실패 처리, `retry_count` 증가, `next_retry_at` 계산과 재시도 작업은 문서대로 향후 구현 범위다.
