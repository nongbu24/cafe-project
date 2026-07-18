# 주문 API 인증 회원 기준 변경

## 작업 목적

주문 API가 요청 본문의 `userId`가 아니라 로그인한 회원의 JWT에서 확인한 회원 ID로만 주문하도록 변경한다.

## 변경 범위와 계획

- 변경할 영역: 주문 API 인증 설정, 주문 컨트롤러와 요청 DTO, 주문 API 테스트, 주문 API 문서
- 변경하지 않을 영역: 주문 서비스의 결제 트랜잭션 구조, JWT 발급 방식, 메뉴와 포인트 도메인 정책
- 구현 방향: `/api/v1/orders`에 인증 인터셉터를 적용하고, 컨트롤러에서 인증된 회원 ID를 읽어 주문 서비스에 전달한다. 요청 body는 `menuId`만 받도록 계약을 바꾼다.
- 검증 방법: 주문 도메인 테스트와 전체 테스트를 실행한다.

## 변경 결과

- 핵심 변경: `/api/v1/orders`에 인증 인터셉터를 적용하고, 주문 컨트롤러가 요청 본문의 `userId` 대신 JWT 인증 결과의 회원 ID를 사용하도록 변경했다. 주문 요청 DTO는 `menuId`만 받도록 줄였고, 요청에 다른 `userId`가 포함되어도 로그인한 회원으로 주문되는 테스트와 인증 실패 테스트를 추가했다. API 문서도 인증 헤더와 `menuId` 단일 요청 계약으로 갱신했다.
- 변경 파일: `WebConfig`, `OrderController`, `OrderCreateRequest`, `OrderControllerTest`, `docs/api/order.md`

## 검증과 남은 사항

- 실행한 검증과 결과: `./gradlew test --tests 'com.example.cafe.order.*'` 성공, `./gradlew test` 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
