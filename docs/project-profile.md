# 프로젝트 프로필

이 문서는 cafe 저장소의 실제 값만 기록한다.
공통 강제 규칙은 `AGENTS.md`, 실행 방법과 권장 체크리스트는 `docs/agent-workflow.md`를 따른다.

아직 확인되지 않은 값은 `미정`, 사용하지 않는 영역은 `미사용`으로 기록한다.

## 프로젝트

- 이름: cafe
- 목적: 관리자와 회원이 사용하는 카페 서비스의 백엔드 기능 제공
- 주요 사용자: 관리자, 회원
- 현재 상태: 인증, 메뉴, 포인트, 주문 주요 도메인 REST API, 장바구니 도메인 기반 구조와 관련 통합 테스트가 존재하는 초기 개발 단계

## 기술과 구조

- 언어: Java 21
- 런타임·프레임워크: Spring Boot 4.1.0, Spring MVC, Bean Validation, Spring Data JPA, QueryDSL, Flyway, WebSocket
- 패키지·빌드 도구: Gradle Wrapper 9.5.1, Groovy DSL
- 데이터 저장소: 애플리케이션 기본 개발 데이터베이스는 인메모리 H2, 운영 목표 데이터베이스는 PostgreSQL
- 로컬 인프라: Docker Compose, PostgreSQL 17-alpine, Redis 7.4-alpine
- 테스트 도구: JUnit Jupiter, Spring Boot Test, Spring Data JPA Test, Spring MVC Test,
  WebSocket Test
- 현재 테스트 방식: `@SpringBootTest`, MockMvc와 인메모리 H2 중심의 통합 테스트. 실제 Redis,
  PostgreSQL과 WebSocket 연결은 자동 테스트 범위에 포함되지 않음

| 주요 경로 | 역할 |
|---|---|
| `build.gradle` | 플러그인, Java 버전, 의존성과 테스트 설정 |
| `settings.gradle` | Gradle 프로젝트 이름 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 버전 |
| `.env.example` | 로컬 환경 변수 이름과 예시. 비밀 값은 비우며 Compose용 실제 값은 Git에서 제외되는 `.env`에 저장 가능 |
| `docker-compose.yml` | 로컬 PostgreSQL·Redis 컨테이너, 볼륨과 상태 확인 설정 |
| `src/main/java/com/example/cafe/` | 애플리케이션 Java 코드 |
| `src/main/resources/application.properties` | 애플리케이션 설정 |
| `src/main/resources/data.sql` | 인메모리 H2의 로컬 초기 데이터와 통합 테스트 fixture |
| `src/main/resources/db/migration/` | Flyway DB 마이그레이션 |
| `src/test/java/com/example/cafe/` | 자동 테스트 |
| `AGENTS.md` | 공통 작업 규칙과 작업 경로 |
| `docs/project-profile.md` | 실제 실행 명령, 프로젝트 규칙과 추가 위험 |
| `docs/dev/tasks/` | 일반·보호 작업 기록과 검증 증거 |

## 도메인별 SSOT

| 영역 | SSOT | 파생·설명 자료 | 갱신 방법·기준 |
|---|---|---|---|
| 공통 작업 규칙 | `AGENTS.md` | `docs/agent-workflow.md` | 공통 규칙 변경 시 |
| 빌드·의존성 설정 | `build.gradle` | `docs/project-profile.md` | 플러그인·의존성·Java 버전 변경 시 |
| Gradle 버전 | `gradle/wrapper/gradle-wrapper.properties` | `docs/project-profile.md` | Wrapper 버전 변경 시 |
| 실행·검증 명령 | `docs/project-profile.md` | 각 작업 기록의 실행 결과 | 명령이나 실행 환경 변경 시 |
| 애플리케이션 설정 | `src/main/resources/application.properties` | `docs/project-profile.md` | 설정 구조나 환경 구분 변경 시 |
| 로컬 컨테이너 구성 | `docker-compose.yml` | `.env.example`, `docs/project-profile.md` | 서비스, 이미지, 포트, 환경 변수, 볼륨이나 상태 확인 방식 변경 시 |
| 로컬 초기 데이터 | `src/main/resources/data.sql` | `SeedUserDataTest`, `MenuControllerTest` | 초기 회원·메뉴 데이터 변경 시 |
| 비즈니스 정책 | `docs/api/auth.md`, `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | `docs/api/README.md`, `docs/db/ERD.md`, 서비스·엔티티와 통합 테스트 | 인증·포인트·결제·인기 집계 정책 변경 시 |
| REST 계약 | `docs/api/README.md`, `docs/api/auth.md`, `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | Controller, DTO와 API 테스트 | REST API 계약 변경 시 |
| WebSocket 계약 | 미정 | 코드와 테스트 | 첫 WebSocket 계약을 구현할 때 지정 |
| DB 구조 | `docs/db/ERD.md` | Entity, Flyway 마이그레이션과 통합 테스트 | 테이블·컬럼·제약조건·관계·인덱스 변경 시 |
| 기능의 의도된 동작 | `docs/api/auth.md`, `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | `docs/api/README.md`, `docs/db/ERD.md`, 애플리케이션 코드와 통합 테스트 | 카페 기능 동작 변경 시 |
| 작업 이력과 검증 증거 | `docs/dev/tasks/`의 각 작업 기록 | 관련 커밋·PR | 일반·보호 작업 수행 시 |
| 빌드 생성물 | 소스 코드와 `build.gradle` | `build/` | `./gradlew build`로 재생성 |

작업 기록은 변경 당시의 목표, 범위와 검증 결과에 대한 이력이며 현재 기능·정책·계약의 SSOT로 사용하지 않는다.

## 실행과 검증

| 적용 대상 | 명령 또는 수동 확인 | 실행 전제 |
|---|---|---|
| 컨텍스트 스모크 테스트 | `./gradlew test --tests 'com.example.cafe.CafeApplicationTests'` | JDK 21 |
| 전체 테스트 | `./gradlew test` | JDK 21 |
| Gradle 기본 검증 | `./gradlew check` | JDK 21 |
| 전체 빌드와 패키징 | `./gradlew build` | JDK 21 |
| 애플리케이션 실행 | `./gradlew bootRun` | JDK 21. 기본 DB는 인메모리 H2이며 로그아웃·회원탈퇴 확인에는 Redis 필요 |
| PostgreSQL 프로필 실행 | `SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun` | PostgreSQL 접속 환경 변수와 Redis 접속 정보 |
| Compose 설정 검사 | `docker compose config --quiet` | Docker Compose와 필수 비밀번호 환경 변수 |
| 로컬 Redis 실행 | `docker compose up -d redis` | Docker Engine과 필수 비밀번호 환경 변수 |
| 전체 로컬 인프라 실행 | `docker compose up -d` | Docker Engine과 필수 비밀번호 환경 변수 |
| 로컬 인프라 상태 확인 | `docker compose ps` | Docker Engine과 실행할 때 사용한 필수 비밀번호 환경 변수 |
| 로컬 인프라 종료 | `docker compose down` | 같은 환경 변수 필요. 이름 있는 볼륨은 유지 |
| 린트·자동 포맷 | 미사용 | 관련 플러그인 미설정 |
| 작업 트리 공백 검사 | `git diff --check` | 스테이징하지 않은 추적 파일 변경 기준 |
| 스테이징 공백 검사 | `git diff --cached --check` | 스테이징된 변경 기준 |
| 최종 범위 확인 | `git status --short`, `git diff`, `git diff --cached` | 미추적 파일은 상태 확인 후 내용을 별도로 검토 |

`./gradlew build`는 테스트와 기본 검증을 포함하므로 최종 검증에서는 같은 작업을 중복 실행하지 않아도 된다.
`./gradlew clean build`는 오래된 생성물이 문제라고 판단될 때만 사용한다.
Gradle toolchain은 JDK 21을 사용한다. JDK 21이 로컬에 없을 때 자동 프로비저닝할 수 있도록
`settings.gradle`에 Foojay toolchain resolver convention 플러그인을 설정한다.

현재 자동 테스트는 인메모리 H2와 Redis 대체 구현·mock을 사용하므로 Docker Compose를 실행하지 않아도 된다.
반대로 실제 Redis 연결과 Compose PostgreSQL 연결은 자동 테스트가 보장하지 않는다.

Docker Compose는 저장소 루트의 `.env`를 자동으로 읽는다. `bootRun`은 `build.gradle` 설정을 통해
저장소 루트의 `.env`를 읽고, 같은 이름의 시스템 환경 변수가 이미 있으면 시스템 환경 변수를 우선한다.
PostgreSQL datasource는 `postgres` 프로필에서만 활성화하며 `POSTGRES_HOST`, `POSTGRES_PORT`,
`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`로 연결한다.

`docker compose down -v`는 이름 있는 볼륨과 로컬 PostgreSQL·Redis 데이터를 삭제한다. 단순 종료에는
볼륨을 유지하는 `docker compose down`을 사용하고, `-v`는 데이터 초기화 범위가 명확할 때만 사용한다.

## 프로젝트별 추가 보호 작업

- 운영 PostgreSQL에 직접 연결하여 데이터 조회·변경·초기화·마이그레이션을 수행하는 작업
- 개발 환경이 운영 PostgreSQL에 연결될 수 있도록 프로필이나 데이터소스 경계를 변경하는 작업
- H2 콘솔을 로컬 개발 환경 밖에서 활성화하거나 외부에 노출하는 작업

위 작업은 `AGENTS.md`의 보호 경로에 따라 대상 환경, 영향, 복구 방법과 검증 방법을 설명하고 실행 직전에 사용자의 명시적 승인을 받는다.

## 프로젝트 규칙

- 기본 패키지: `com.example.cafe`
- 도메인 패키지: 각 도메인 아래에 필요한 역할만 `controller`, `dto`, `entity`, `facade`, `repository`,
  `service`, `store` 하위 패키지로 나눈다. `store`는 Redis처럼 JPA 밖의 저장소 경계와 구현에 사용한다.
  테스트 패키지는 대상 코드의 패키지 구조를 따른다.
- 공통 패키지: 여러 도메인이 함께 사용하는 응답과 예외 처리처럼 횡단 관심사만 `common`에 둔다.
- 서비스가 다른 도메인의 repository 기능을 사용해야 할 때는 대상 도메인의 `facade`를 통해 접근한다.
  다른 도메인의 `repository`를 직접 참조하지 않으며, 같은 도메인 내부에서는 서비스와 facade가
  해당 도메인의 repository를 사용할 수 있다.
- 개발 애플리케이션 데이터베이스: 인메모리 H2. 시작할 때 Flyway 마이그레이션과 `data.sql`을
  적용하며 애플리케이션을 재시작하면 데이터가 초기화된다.
- 로컬 PostgreSQL: Docker Compose로 제공하며 `postgres` 프로필에서 애플리케이션 datasource로 연결 가능
- 운영 목표 데이터베이스: PostgreSQL. 실제 운영 환경의 호스트와 비밀 값은 배포 환경 변수로 제공한다.
- DB 구조를 변경할 때는 `docs/db/ERD.md`와 관련 Entity를 함께 갱신하고
  `src/main/resources/db/migration/`의 Flyway 마이그레이션으로 적용한다. Hibernate의 `ddl-auto`는
  `validate`를 유지한다.
- JPA에서 직접 쿼리를 작성해야 하는 경우 Spring Data JPA의 `@Query` 대신 QueryDSL을 사용한다.
  단순 조건 조회는 메서드 이름 기반 쿼리를 우선 사용하고, 복잡한 조회·집계·락처럼 직접 쿼리 표현이 필요한 경우
  도메인 `repository` 패키지의 커스텀 Repository 구현에서 QueryDSL로 작성한다.
- QueryDSL Q 타입은 `build/generated/sources/annotationProcessor/java/main/`의 생성물이므로 직접
  만들거나 수정하지 않는다. Entity 또는 annotation processor 설정을 변경한 뒤 `./gradlew compileJava`,
  `./gradlew test` 또는 `./gradlew build`로 다시 생성한다.
- 코드 스타일: 별도 포매터와 린터는 사용하지 않는다. 현재 프로젝트 안에서 패키지·네이밍·포맷이
  일관되도록 작성한다.
- 생성 시각과 수정 시각을 함께 관리하는 엔티티는 `BaseEntity`를 상속하여 `createdAt`과 `updatedAt`을 관리한다.
  `updatedAt`은 모든 도메인에서 실제 수정 전까지 `NULL`로 유지하고, 수정 시점에만 값을 기록한다.
- 애플리케이션 코드에서 관계형 DB 저장 시각을 생성할 때는 `docs/db/ERD.md`의 시간 기준에 따라
  UTC `LocalDateTime`으로 만들고, API 응답에서는 한국 시간 offset으로 변환한다. 공통 생성과
  변환에는 `DateTimeUtils`를 사용한다.
- 회원탈퇴는 회원 행을 물리적으로 삭제하지 않고 `users.is_deleted`를 `true`로 변경하는 소프트 삭제 정책을 사용한다.
  탈퇴 회원은 로그인과 인증이 필요한 기능을 사용할 수 없다.
- 문서 스타일: 프로젝트 문서는 한국어로 작성하고 코드 식별자와 명령은 원문을 유지한다.
- 원격 기본 브랜치: `dev`
- 현재 원격 브랜치: `dev`만 존재
- 브랜치 보호·병합 방식: 미정
- 커밋 메시지 형식: `type: 한국어 요약` (예: `feat: 회원 가입 기능 추가`)
- 문서 갱신 기준: 공개 API, WebSocket 계약, DB 구조, 비즈니스 정책 또는 실제 기능 동작이 변경되면 해당 영역의 SSOT를 지정하거나 함께 갱신한다.

### REST API 공통 구현 규칙

- REST API 성공 응답은 `ApiResponse<T>`의 `code`, `message`, `data` 구조를 사용하고 `ApiResponse.success(...)`로 생성한다.
- 성공 응답 메시지는 기본적으로 `요청이 성공적으로 처리되었습니다.`를 사용하되, 주문 완료처럼 API 상황에 맞는 문구가 있으면
  해당 API 계약 문서와 테스트에 그 응답 메시지를 명시한다.
- 오류 응답은 `ErrorResponse`의 `code`, `message` 구조를 사용하며 `data` 필드를 포함하지 않는다.
- 애플리케이션 오류의 HTTP 상태, 응답 코드와 기본 메시지는 `ErrorCode`에서 한곳에 관리한다.
  서비스와 컨트롤러에 같은 오류 코드나 기본 메시지를 문자열로 중복 작성하지 않는다.
- 비즈니스 규칙이나 요청 값 검증에 실패하면 `ApplicationException`에 알맞은 `ErrorCode`를 전달한다.
  기본 메시지보다 구체적인 설명이 필요할 때만 상세 메시지를 함께 전달한다.
- `GlobalExceptionHandler`는 `ApplicationException`을 공통 처리한다.
  오류 종류가 추가될 때마다 같은 형태의 예외 처리 메서드를 만들지 않고 먼저 `ErrorCode` 추가하여 해결한다.
- Spring MVC가 직접 발생시키는 요청 변환·본문 파싱 예외처럼
  애플리케이션에서 `ApplicationException`으로 바꿀 수 없는 예외만 전역 핸들러에서 별도로 처리한다.
- 공개 오류 코드나 응답 형식이 달라지면 관련 `docs/api/` 계약과 API 테스트를 함께 갱신한다.

## 외부 환경

- 애플리케이션 환경 변수: JWT 서명 키 `JWT_SECRET`, Redis 연결용 `REDIS_HOST`, `REDIS_PORT`,
  `REDIS_PASSWORD`, PostgreSQL 프로필 연결용 `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`,
  `POSTGRES_USER`, `POSTGRES_PASSWORD`
- 로컬 Compose 환경 변수: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`,
  `REDIS_PORT`, `REDIS_PASSWORD`. `POSTGRES_PASSWORD`와 `REDIS_PASSWORD`는 필수
- `.env`는 Git에서 제외되며 Docker Compose와 `bootRun`이 읽는다. 실제 값을 `.env.example`, 문서,
  코드, 로그나 Git 관리 파일에 기록하지 않는다.
- 로컬 실행 전제: JDK 21. Docker 기반 Redis·PostgreSQL을 사용할 때는 Docker Engine과 Docker Compose 필요
- 자동 테스트 전제: JDK 21. 외부 Redis·PostgreSQL은 사용하지 않음
- 최초 빌드 전제: Gradle 배포 파일과 Maven 의존성을 내려받을 네트워크 연결
- 외부 서비스: JWT 블랙리스트용 Redis, PostgreSQL. 로컬 Redis와 PostgreSQL은 Docker Compose로 제공하며,
  실제 운영 연결 위치와 제공 방식은 배포 환경에서 제공한다.
- 저장소 내 CI 설정: 미사용. 현재 검증은 로컬 Gradle 명령으로 수행
- 배포·운영 환경: 미정
- 비용 발생 가능 작업: 미정

비밀 값 자체는 문서, 코드, 로그와 Git 관리 파일에 기록하지 않는다.

## 참고 링크

- `HELP.md`는 현재 Git에서 제외되어 있으므로 프로젝트 규칙이나 SSOT로 사용하지 않는다.
