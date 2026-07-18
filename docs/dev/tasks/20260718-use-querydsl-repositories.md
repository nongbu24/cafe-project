# QueryDSL Repository 전환

## 작업 목적

Spring Data JPA의 `@Query`로 직접 작성된 JPQL을 제거하고, 직접 쿼리 표현이 필요한 Repository 조회를 QueryDSL 기반으로 전환한다.

## 변경 범위와 계획

- 변경할 영역: QueryDSL 의존성·설정, 주문 인기 메뉴 집계 Repository, 회원 비관적 락 조회 Repository, 프로젝트 규칙 문서
- 변경하지 않을 영역: API 계약, 주문·포인트·회원 비즈니스 정책, 데이터베이스 구조
- 구현 방향: `JPAQueryFactory`를 공통 설정으로 등록하고, 도메인별 커스텀 Repository 구현에서 QueryDSL로 기존 조회 동작을 유지한다.
- 검증 방법: 전체 테스트와 `@Query` 잔존 여부 검색을 실행한다.

## 변경 결과

- 핵심 변경: `OrderRepository`와 `UserRepository`의 `@Query` 기반 JPQL을 제거하고 QueryDSL 커스텀 Repository 구현으로 대체했다.
- 변경 파일:
  - `build.gradle`
  - `docs/project-profile.md`
  - `src/main/java/com/example/cafe/common/config/QuerydslConfig.java`
  - `src/main/java/com/example/cafe/order/repository/OrderRepository.java`
  - `src/main/java/com/example/cafe/order/repository/OrderRepositoryCustom.java`
  - `src/main/java/com/example/cafe/order/repository/OrderRepositoryImpl.java`
  - `src/main/java/com/example/cafe/user/repository/UserRepository.java`
  - `src/main/java/com/example/cafe/user/repository/UserRepositoryCustom.java`
  - `src/main/java/com/example/cafe/user/repository/UserRepositoryImpl.java`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `rg "@Query|org\.springframework\.data\.jpa\.repository\.Query" src/main/java` 실행 결과, Java 코드에는 `@Query` 사용이 남아 있지 않음
  - `./gradlew test` 성공
  - `git diff --check` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
