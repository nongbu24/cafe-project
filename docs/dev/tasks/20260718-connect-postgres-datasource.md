# PostgreSQL datasource 연결

## 목표

배포 또는 로컬 Compose PostgreSQL을 사용할 때 애플리케이션이 PostgreSQL datasource에 연결되도록 설정한다.
기본 개발 실행은 기존처럼 인메모리 H2를 유지한다.

## 승인

- 사용자 승인: `SPRING_PROFILES_ACTIVE=postgres` 프로필 기반으로 PostgreSQL datasource 설정을 추가한다.
- 예상 영향: `postgres` 프로필을 켠 실행에서만 datasource가 PostgreSQL로 바뀐다.
- 보호 작업 범위: 운영 PostgreSQL 실제 접속 정보 설정, 데이터 조회·변경, 마이그레이션 실행은 하지 않는다.

## 작업 내용

- `application-postgres.properties`를 추가해 PostgreSQL datasource URL, 계정, 드라이버를 환경 변수로 설정한다.
- `.env.example`에 애플리케이션용 `POSTGRES_HOST`와 `SPRING_PROFILES_ACTIVE=postgres` 사용 예시를 추가한다.
- 프로젝트 프로필 문서에 PostgreSQL 프로필 실행 방식을 반영한다.

## 검증

- `./gradlew test --tests 'com.example.cafe.CafeApplicationTests'`: 성공
- `git diff --check`: 성공
