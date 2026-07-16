# 메뉴 수정 시각 NULL 정책 적용

## 작업 목적

메뉴가 생성된 뒤 실제로 수정되기 전까지 `updated_at`을 `NULL`로 유지하여 수정 여부를 명확히 구분한다.

## 변경 범위와 계획

- 변경할 영역: 메뉴 엔티티 생명주기, H2 초기 데이터, ERD 정책, 통합 테스트
- 변경하지 않을 영역: 메뉴 조회 API 응답과 데이터 내용, 생성 시각 기록
- 구현 방향: 생성 시 `createdAt`만 설정하고 `updatedAt`은 실제 수정 시점에만 설정한다.
- 검증 방법: 초기 데이터의 `updated_at` 확인 테스트, 전체 테스트, `git diff --check`

## 변경 결과

- 핵심 변경:
  - 메뉴 생성 시 `createdAt`만 설정하고 `updatedAt`은 설정하지 않도록 변경했다.
  - H2 초기 메뉴 100건의 `updated_at`을 모두 `NULL`로 변경했다.
  - ERD에 메뉴의 `updated_at`이 실제 수정 전까지 `NULL`이라는 정책을 반영했다.
  - 초기 메뉴 100건의 `updated_at`이 모두 `NULL`인지 확인하는 통합 테스트를 추가했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/menu/Menu.java`
  - `src/main/resources/data.sql`
  - `src/test/java/com/example/cafe/menu/MenuControllerTest.java`
  - `docs/db/ERD.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test`: 성공, 전체 테스트 3개 통과
  - 초기 데이터 확인: 100건 모두 `updated_at IS NULL`
  - `git diff --check`: 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 메뉴 수정 기능이 추가되면 `@PreUpdate`가 실제 수정 시각을 기록한다.
