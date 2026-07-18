# 로컬 Docker Compose 추가

## 목적

- 로컬 개발에서 Redis와 PostgreSQL을 Docker Compose로 실행할 수 있게 한다.
- 애플리케이션의 현재 Redis 설정(`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)과 맞게 구성한다.
- PostgreSQL과 Redis 비밀번호는 Git에 실제 값을 남기지 않고 환경변수에서 읽는다.

## 변경

- `docker-compose.yml`에 `postgres`, `redis` 서비스를 추가한다.
- 각 서비스에 로컬 포트 매핑, 데이터 볼륨, 기본 healthcheck를 설정한다.
- `.env.example`에 PostgreSQL 설정 변수 이름을 추가한다.
- Redis는 `REDIS_PASSWORD`를 `redis-server --requirepass`에 전달해 비밀번호 인증을 사용한다.

## 검증

- `POSTGRES_PASSWORD`, `REDIS_PASSWORD`에 임시 더미 값을 넣고 `docker compose -f docker-compose.yml config`: 성공
