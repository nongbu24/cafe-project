# 메뉴 상태 추가와 조회 대상 제한

## 작업 목적

메뉴를 판매중·품절·단종 상태로 구분하고, 메뉴 목록에서는 판매중과 품절 메뉴만 제공한다.

## 변경 범위와 계획

- 변경할 영역: 메뉴 상태 Enum·엔티티·Repository·응답 DTO, 초기 데이터, ERD·API 문서와 통합 테스트
- 변경하지 않을 영역: API 경로, 페이지네이션 기본값과 최대 크기
- 구현 방향: `AVAILABLE`, `SOLD_OUT`, `DISCONTINUED`를 문자열로 저장하고 조회 시 앞의 두 상태만 포함한다.
- 검증 방법: 상태별 초기 데이터 수, 단종 제외 목록과 페이지 정보, 전체 테스트, `git diff --check`

## 변경 결과

- 핵심 변경:
  - `AVAILABLE`(판매중), `SOLD_OUT`(품절), `DISCONTINUED`(단종) 메뉴 상태 Enum을 추가했다.
  - 메뉴 상태를 문자열로 DB에 저장하고 API 응답에 상태 코드를 포함하도록 변경했다.
  - 판매중과 품절 메뉴만 페이지 조회하고 단종 메뉴는 목록과 전체 개수에서 제외했다.
  - 초기 데이터 100건을 판매중 80건, 품절 10건, 단종 10건으로 구성했다.
  - ERD·API 계약과 상태별 저장·조회 통합 테스트를 갱신했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/menu/`
  - `src/main/resources/data.sql`
  - `src/test/java/com/example/cafe/menu/MenuControllerTest.java`
  - `docs/api/menu.md`
  - `docs/db/ERD.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test`: 성공, 전체 테스트 9개 통과
  - 초기 데이터 상태 수: 판매중 80건, 품절 10건, 단종 10건
  - 조회 결과: 판매중·품절 90건, 마지막 조회 메뉴 ID 90
  - `git diff --check`: 성공
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 단종 메뉴는 DB에 유지되지만 메뉴 목록 API에서는 조회되지 않는다.
