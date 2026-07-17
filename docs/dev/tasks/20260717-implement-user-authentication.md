# 회원 인증과 탈퇴 기능 구현

## 작업 목적

사용자 역할을 구분하고 회원가입, 로그인, JWT 블랙리스트 기반 로그아웃과 회원탈퇴 기능을 제공한다.
로컬 개발 환경에서 동작을 확인할 수 있도록 관리자 3명과 일반 회원 7명을 초기 데이터로 추가한다.

## 변경 범위와 계획

- 변경할 영역: 사용자 엔티티와 저장소, 인증·보안 설정, 인증 API, 오류 코드, 개발용 초기 데이터,
  사용자 API 계약, ERD, 자동 테스트
- 변경하지 않을 영역: 운영 PostgreSQL 데이터, 기존 주문·포인트 API의 사용자 식별 방식,
  사용자별 관리자 권한 정책
- 구현 방향: `UserStatus`로 `ADMIN`과 `USER`를 구분하고 비밀번호는 BCrypt로 저장한다.
  로그인 시 JWT 액세스 토큰을 발급하고 로그아웃한 토큰의 `jti`를 JWT 남은 유효시간만큼
  Redis 블랙리스트에 저장한다.
  회원탈퇴는 이력을 보존하기 위해 `is_deleted`를 사용하는 소프트 삭제로 처리하고,
  생성·수정 시각은 공통 `BaseEntity`에서 관리한다.
- 검증 방법: 사용자 상태와 인증 서비스 단위 테스트, 인증 API 통합 테스트, 전체 Gradle 테스트,
  문서·공백 검사와 최종 diff 검토

### 보호 작업 승인과 복구 수단

- 승인 일시: 2026-07-17
- 승인 내용: 인증·인가 정책과 DB 구조 변경, 로컬 H2 초기 회원 10명 추가
- 대상 환경: 현재 저장소의 애플리케이션 코드와 로컬 개발용 임베디드 H2
- 예상 영향: 사용자 테이블 컬럼이 추가되고 인증 API와 Redis 기반 토큰 블랙리스트가 제공된다.
- 제외 대상: 운영 PostgreSQL 및 외부·공유 데이터
- 복구 수단: 이번 작업에서 추가·수정한 파일의 diff만 되돌린다. 기존 미커밋 변경은 보존한다.

### 변경된 보호 작업 승인과 주문 구현 정리

- 추가 승인 일시: 2026-07-17
- 승인 내용: 미추적 주문 소스·테스트·작업 기록 삭제, `is_deleted`와 `BaseEntity` 정책 적용
- 삭제 대상: `src/main/java/com/example/cafe/order/`, `src/test/java/com/example/cafe/order/`,
  `docs/dev/tasks/20260717-implement-order-payment-api.md`
- 복구 수단: 삭제 전 `/private/tmp/cafe-order-backup.C9NaZw`에 대상 파일을 복사하고 파일 목록을 확인했다.
- 유지 대상: 주문 API와 DB 설계 문서는 향후 구현 명세로 보존한다.

### Redis 블랙리스트 변경 승인

- 추가 승인 일시: 2026-07-17
- 승인 내용: JWT 블랙리스트 저장소를 관계형 DB에서 Redis로 변경하고 비밀번호를 영문, 숫자,
  일반 ASCII 특수문자 8자 이상 64자 이하로 제한한다. 오류 응답에서 `data`를 제거하고
  `ErrorCode`를 도메인별 주석으로 구분하며 `.env.example`을 추가한다.
- 대상 환경: 애플리케이션 코드와 로컬 설정 예시. 실제 외부 Redis에는 접속하거나 데이터를 쓰지 않는다.
- 예상 영향: 애플리케이션 실행 시 Redis 연결 정보가 필요하며, 로그아웃 토큰 키는 JWT 만료와 함께
  TTL로 자동 삭제된다.
- 복구 수단: Redis 관련 의존성·구현·설정을 되돌리고 이전 JPA 블랙리스트 구현을 복원한다.

## 변경 결과

- 핵심 변경:
  - `UserStatus`의 `ADMIN`, `USER`를 `users.user_status`에 문자열로 저장한다.
  - 회원가입 비밀번호를 BCrypt로 암호화하고 로그인 시 HS256 JWT를 발급한다.
  - 로그아웃·회원탈퇴 JWT의 `jti`를 Redis에 저장하고 JWT 남은 유효시간을 TTL로 설정한다.
  - Redis 저장소 계약과 구현 이름을 `TokenBlacklistStore`, `RedisTokenBlacklistStore`로 명확히 하고
    로그인 응답에서는 서버 내부 관리 정보인 JWT 만료 시각을 제거했다.
  - 회원탈퇴는 `users.is_deleted`를 `true`로 변경하고 탈퇴 회원의 모든 JWT 사용을 거부한다.
  - `User`의 공통 생성·수정 시각을 `BaseEntity`로 이동했다.
  - 로컬 H2 초기 데이터에 관리자 3명과 일반 회원 7명을 추가했다.
  - 승인된 미추적 주문 구현 소스·테스트·작업 기록을 삭제했다.
- 변경 파일:
  - 인증 구현: `src/main/java/com/example/cafe/auth/`, `src/main/java/com/example/cafe/user/`,
    `src/main/java/com/example/cafe/common/config/WebConfig.java`
  - 공통 엔티티: `src/main/java/com/example/cafe/common/entity/BaseEntity.java`
  - 설정·초기 데이터: `build.gradle`, `.env.example`, `src/main/resources/application.properties`,
    `src/main/resources/data.sql`
  - 문서: `docs/api/auth.md`, `docs/api/README.md`, `docs/db/ERD.md`,
    `docs/project-profile.md`
  - 테스트: `src/test/java/com/example/cafe/auth/`, 기존 포인트 테스트의 회원 데이터 입력문

## 검증과 남은 사항

- 실행한 검증과 결과:
  - 인증 API와 초기 회원 데이터 대상 테스트 성공
  - `./gradlew test` 성공: 총 26개 테스트, 실패와 건너뜀 없음
  - `git diff --check` 성공
  - 주문 소스·테스트 디렉터리와 주문 구현 작업 기록이 삭제됐음을 확인
- 실행하지 못한 검증: 운영 PostgreSQL과 실제 Redis는 대상 범위가 아니며 현재 로컬 환경에도
  Redis 서버가 없어 연결 검증하지 않았다.
- 남은 제한사항과 사용자 조치:
  - 운영 환경에서는 Base64 디코딩 기준 32바이트 이상의 `JWT_SECRET`을 반드시 제공해야 한다.
  - 로그아웃과 회원탈퇴를 사용하려면 실행 환경에 Redis 연결 정보를 제공해야 한다.
  - 관리자 전용 API가 아직 없으므로 `ADMIN`과 `USER`를 나누는 세부 인가 규칙은 적용 대상 API가
    추가될 때 구현해야 한다.
  - 주문 구현 복구용 사본은 `/private/tmp/cafe-order-backup.C9NaZw`에 있으며 임시 디렉터리 정리 전까지
    사용할 수 있다.
