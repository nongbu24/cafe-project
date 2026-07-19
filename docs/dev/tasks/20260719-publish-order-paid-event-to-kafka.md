# 주문 완료 이벤트 Kafka 발행

## 작업 목적

주문 완료 Outbox 이벤트를 기존 mock 데이터 수집 지점 대신 Kafka topic으로 발행한다.

## 변경 범위와 계획

- 변경할 영역: 주문 Outbox 전송 서비스, Kafka Producer 구성, Docker Compose Kafka broker 설정, 주문 통합 테스트, 관련 문서
- 변경하지 않을 영역: 주문 생성 API 계약, Outbox DB 구조, 재시도 정책, 실제 Kafka 컨테이너 구성
- 구현 방향: `OrderOutboxSender`가 `OrderPaidKafkaProducer`를 호출하고, Producer는 `cafe.order-paid` topic에 주문 완료 payload를 발행한다. 로컬 Kafka broker는 `docker compose up -d kafka`로 실행할 수 있게 한다.
- 검증 방법: 주문 컨트롤러 테스트와 컴파일 또는 관련 Gradle 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `MockOrderDataCollector`를 제거하고 `OrderPaidKafkaProducer`를 추가해 주문 완료 payload를 Kafka topic `cafe.order-paid`로 발행하도록 변경했다. `OrderOutboxSender`는 Kafka Producer 호출 성공 시 Outbox를 `SENT`로 표시하고, 실패 시 기존 재시도 정책을 유지한다. `docker-compose.yml`에 로컬 개발용 단일 Kafka broker를 추가했다.
- 변경 파일: `build.gradle`, `application.properties`, `.env.example`, `docker-compose.yml`, `OrderPaidKafkaProducer`, `OrderOutboxSender`, `OrderOutboxEventListener`, `OrderControllerTest`, `README.md`, `docs/api/order.md`, `docs/db/ERD.md`, `docs/project-profile.md`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.controller.OrderControllerTest'` 통과, `./gradlew test` 통과, `POSTGRES_PASSWORD=local REDIS_PASSWORD=local docker compose config --quiet` 통과, `git diff --check` 통과
- 실행하지 못한 검증: 실제 Kafka broker 컨테이너 실행과 메시지 발행 수동 검증은 실행하지 않았다.
- 남은 제한사항과 사용자 조치: 로컬에서는 `docker compose up -d kafka`로 Kafka broker를 실행해야 실제 발행이 성공한다. 배포 환경에서는 별도 Kafka broker와 `KAFKA_BOOTSTRAP_SERVERS` 값을 준비해야 한다.
