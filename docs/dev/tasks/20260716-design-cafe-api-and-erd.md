# 카페 API와 ERD 설계

## 작업 목적

커피 메뉴 조회, 포인트 충전, 주문·결제, 최근 7일 인기 메뉴 조회 기능을 구현하기 전에
데이터 구조와 REST API 계약을 명확히 정의한다.

## 변경 범위와 계획

- 변경할 영역: DB 구조 문서, REST API 명세, 프로젝트의 도메인별 SSOT 표
- 변경하지 않을 영역: Java 구현 코드, 애플리케이션 설정, 실제 DB 스키마와 데이터
- 구현 방향: 금액·포인트는 `BIGINT`를 사용하고, 결제의 원자성 및 주문 이벤트의 안정적인 전송을 고려한다.
- 검증 방법: 문서 간 명칭·자료형·경로를 대조하고 `git diff --check`로 문서 형식을 확인한다.

## 변경 결과

- 핵심 변경:
  - 회원, 메뉴, 포인트 거래, 주문, 주문 이벤트 Outbox로 구성된 ERD와 제약조건을 정의했다.
  - 메뉴, 포인트, 주문 도메인별 API 문서에 요청·응답과 오류 계약을 정의했다.
  - 금액·포인트는 Java `long`과 DB `BIGINT`를 사용하도록 정했다.
  - 동시 결제 시 잔액의 정확성을 위한 회원 행 잠금과 외부 이벤트 재전송 정책을 정의했다.
  - REST 계약과 DB 구조 문서를 도메인별 SSOT로 지정했다.
- 변경 파일:
  - `docs/api/README.md`
  - `docs/api/menu.md`
  - `docs/api/point.md`
  - `docs/api/order.md`
  - `docs/db/ERD.md`
  - `docs/project-profile.md`
  - `docs/dev/tasks/20260716-design-cafe-api-and-erd.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - API와 ERD에서 `BIGINT`, 주문 상태, Outbox, 최근 7일 집계 기준과 API 경로를 상호 대조했다.
  - Markdown 코드 블록의 시작·종료 개수가 일치하는지 확인했다.
  - `git diff --no-index --check`로 새 문서의 공백 오류가 없음을 확인했다.
- 실행하지 못한 검증: 코드와 DB 스키마를 변경하지 않았으므로 애플리케이션 테스트는 실행하지 않았다.
- 남은 제한사항과 사용자 조치: 구현 단계에서 Entity, Repository, Service, Controller와 동시성·집계·이벤트 전송 테스트를 추가해야 한다.
