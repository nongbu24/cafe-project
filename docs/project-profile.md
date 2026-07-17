# 프로젝트 프로필

이 문서는 cafe 저장소의 실제 값만 기록한다.
공통 강제 규칙은 `AGENTS.md`, 실행 방법과 권장 체크리스트는 `docs/agent-workflow.md`를 따른다.

아직 확인되지 않은 값은 `미정`, 사용하지 않는 영역은 `미사용`으로 기록한다.

## 프로젝트

- 이름: cafe
- 목적: 관리자와 회원이 사용하는 카페 서비스의 백엔드 기능 제공
- 주요 사용자: 관리자, 회원
- 현재 상태: Spring Boot 초기 개발 단계. 애플리케이션 진입점과 컨텍스트 로딩 테스트만 존재

## 기술과 구조

- 언어: Java 21
- 런타임·프레임워크: Spring Boot 4.1.0, Spring MVC, Spring Data JPA, WebSocket
- 패키지·빌드 도구: Gradle Wrapper 9.5.1, Groovy DSL
- 데이터 저장소: 개발 환경 H2, 운영 환경 PostgreSQL
- 테스트 도구: JUnit Jupiter, Spring Boot Test, Spring Data JPA Test, Spring MVC Test,
  WebSocket Test

| 주요 경로 | 역할 |
|---|---|
| `build.gradle` | 플러그인, Java 버전, 의존성과 테스트 설정 |
| `settings.gradle` | Gradle 프로젝트 이름 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 버전 |
| `src/main/java/com/example/cafe/` | 애플리케이션 Java 코드 |
| `src/main/resources/application.properties` | 애플리케이션 설정 |
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
| 비즈니스 정책 | `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | `docs/api/README.md`, `docs/db/ERD.md`, 향후 코드와 테스트 | 포인트·결제·인기 집계 정책 변경 시 |
| REST 계약 | `docs/api/README.md`, `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | 향후 Controller, DTO와 API 테스트 | REST API 계약 변경 시 |
| WebSocket 계약 | 미정 | 코드와 테스트 | 첫 WebSocket 계약을 구현할 때 지정 |
| DB 구조 | `docs/db/ERD.md` | 향후 Entity, DDL, 마이그레이션과 테스트 | DB 구조 변경 시 |
| 기능의 의도된 동작 | `docs/api/menu.md`, `docs/api/point.md`, `docs/api/order.md` | `docs/api/README.md`, `docs/db/ERD.md`, 향후 코드와 테스트 | 카페 기능 동작 변경 시 |
| 작업 이력과 검증 증거 | `docs/dev/tasks/`의 각 작업 기록 | 관련 커밋·PR | 일반·보호 작업 수행 시 |
| 빌드 생성물 | 소스 코드와 `build.gradle` | `build/` | `./gradlew build`로 재생성 |

작업 기록은 변경 당시의 목표, 범위와 검증 결과에 대한 이력이며 현재 기능·정책·계약의 SSOT로 사용하지 않는다.

## 실행과 검증

| 적용 대상 | 명령 또는 수동 확인 | 실행 전제 |
|---|---|---|
| 현재 기본 테스트 | `./gradlew test --tests 'com.example.cafe.CafeApplicationTests'` | JDK 21 |
| 전체 테스트 | `./gradlew test` | JDK 21 |
| Gradle 기본 검증 | `./gradlew check` | JDK 21 |
| 전체 빌드와 패키징 | `./gradlew build` | JDK 21 |
| 애플리케이션 실행 | `./gradlew bootRun` | 실행 환경에 맞는 데이터베이스 설정 |
| 린트·자동 포맷 | 미사용 | 관련 플러그인 미설정 |
| 문서·공백 검사 | `git diff --check` | 추적되거나 스테이징된 변경 기준 |
| 최종 범위 확인 | `git status --short`, `git diff` | 기존 변경과 현재 작업 구분 |

`./gradlew build`는 테스트와 기본 검증을 포함하므로 최종 검증에서는 같은 작업을 중복 실행하지 않아도 된다.
`./gradlew clean build`는 오래된 생성물이 문제라고 판단될 때만 사용한다.

## 프로젝트별 추가 보호 작업

- 운영 PostgreSQL에 직접 연결하여 데이터 조회·변경·초기화·마이그레이션을 수행하는 작업
- 개발 환경이 운영 PostgreSQL에 연결될 수 있도록 프로필이나 데이터소스 경계를 변경하는 작업
- H2 콘솔을 로컬 개발 환경 밖에서 활성화하거나 외부에 노출하는 작업

위 작업은 `AGENTS.md`의 보호 경로에 따라 대상 환경, 영향, 복구 방법과 검증 방법을 설명하고
실행 직전에 사용자의 명시적 승인을 받는다.

## 프로젝트 규칙

- 기본 패키지: `com.example.cafe`
- 도메인 패키지: 각 도메인 아래에 필요한 역할만 `controller`, `dto`, `entity`, `repository`,
  `service` 하위 패키지로 나눈다. 테스트 패키지는 대상 코드의 패키지 구조를 따른다.
- 공통 패키지: 여러 도메인이 함께 사용하는 응답과 예외 처리처럼 횡단 관심사만 `common`에 둔다.
- 개발 데이터베이스: H2
- 운영 데이터베이스: PostgreSQL
- 코드 스타일: 별도 포매터와 린터가 없으므로 기존 코드의 패키지·네이밍·포맷을 따른다.
- 생성 시각과 수정 시각을 공통 NOT NULL 정책으로 관리하는 엔티티는 `BaseEntity`를 상속하여 `createdAt`과 `updatedAt`을 관리한다.
  도메인 정책상 수정 시각이 nullable인 엔티티는 예외로 둔다.
- 회원탈퇴는 회원 행을 물리적으로 삭제하지 않고 `users.is_deleted`를 `true`로 변경하는 소프트 삭제 정책을 사용한다.
  탈퇴 회원은 로그인과 인증이 필요한 기능을 사용할 수 없다.
- 문서 스타일: 프로젝트 문서는 한국어로 작성하고 코드 식별자와 명령은 원문을 유지한다.
- 기본 브랜치: `main`
- 개발·관리 브랜치: `dev`
- 브랜치 보호와 병합 방식: 미정
- 커밋 메시지 형식: `type: 한국어 요약` (예: `feat: 회원 가입 기능 추가`)
- 문서 갱신 기준: 공개 API, WebSocket 계약, DB 구조, 비즈니스 정책 또는 실제 기능 동작이
  변경되면 해당 영역의 SSOT를 지정하거나 함께 갱신한다.

### REST API 공통 구현 규칙

- REST API의 성공과 오류 응답은 `ApiResponse<T>`의 `code`, `message`, `data` 구조를 사용한다.
- 성공 응답은 `ApiResponse.success(...)`로 생성한다. 오류 응답의 `data`는 `null`로 반환한다.
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

- 환경 변수 이름: JWT 서명 키 `JWT_SECRET`, Redis 연결용 `REDIS_HOST`, `REDIS_PORT`,
  `REDIS_PASSWORD`. 운영 PostgreSQL 연결 설정을 추가할 때 실제 이름 기록
- 로컬 실행 전제: JDK 21, H2와 Redis 사용
- 최초 빌드 전제: Gradle 배포 파일과 Maven 의존성을 내려받을 네트워크 연결
- 외부 서비스: JWT 블랙리스트용 Redis, 운영 환경 PostgreSQL 예정. 실제 운영 연결 위치와 제공 방식은 미정
- 배포·운영 환경: 미정
- 비용 발생 가능 작업: 미정

비밀 값 자체는 문서, 코드, 로그와 Git 관리 파일에 기록하지 않는다.

## 참고 링크

- `HELP.md`는 현재 Git에서 제외되어 있으므로 프로젝트 규칙이나 SSOT로 사용하지 않는다.
