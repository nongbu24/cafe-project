# 포트원 포인트 결제 연동

## 작업 목적

포인트 충전을 포트원 결제 흐름과 연결하고, 포트원 웹훅을 통해 결제 완료를 확인한 뒤 포인트를 적립한다.

## 변경 범위와 계획

- 변경할 영역: 포인트 충전 API, 포트원 설정과 결제 조회 클라이언트, 웹훅 수신 API, 포인트 결제 대기 DB 구조, 관련 문서와 테스트
- 변경하지 않을 영역: 실제 포트원 콘솔 설정, 실제 결제 요청 실행, 운영 DB 직접 마이그레이션 적용, 주문 결제 흐름
- 구현 방향: 충전 요청 시 서버가 `paymentId`를 생성해 대기 상태로 저장하고, `Transaction.Paid` 웹훅 수신 시 포트원 결제 단건 조회 결과의 상태와 금액을 검증한 뒤 한 번만 포인트를 적립한다.
- 검증 방법: 포인트 컨트롤러 테스트와 웹훅 테스트를 실행하고, 가능하면 전체 테스트를 실행한다.

## 변경 결과

- 핵심 변경: 포인트 충전 API가 포트원 결제 준비 정보를 반환하도록 변경했고, 포트원 `Transaction.Paid` 웹훅 수신 시 결제 단건 조회 결과의 상태·상점·금액을 검증한 뒤 한 번만 포인트를 적립하도록 구현했다.
- 변경 파일: 포인트 도메인 controller/service/dto/entity/repository, 포트원 설정, Flyway 마이그레이션, API·ERD·프로젝트 문서, 관련 통합 테스트

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.point.controller.PointControllerTest'` 성공, `./gradlew test` 성공, `git diff --check` 성공
- 실행하지 못한 검증: 실제 포트원 콘솔 웹훅 호출과 실제 결제 승인 검증은 로컬 자동 테스트 범위가 아니므로 실행하지 않았다.
- 남은 제한사항과 사용자 조치: 실행 환경에 `PORTONE_STORE_ID`, `PORTONE_CHANNEL_KEY`, `PORTONE_API_SECRET` 값을 설정하고, 포트원 콘솔의 결제모듈 V2 웹훅 Endpoint URL을 `/api/v1/portone/webhooks/payments`로 연결해야 한다.
