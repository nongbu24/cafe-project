# 도메인 패키지 구조 정리

## 작업 목적

도메인 바로 아래에 모여 있는 파일을 역할별 하위 패키지로 나누어 파일의 책임과 위치를 쉽게 찾을 수 있도록 정리한다.

## 변경 범위와 계획

- 변경할 영역: `menu`, `point`, `user`의 Java 소스와 관련 테스트 패키지, 프로젝트 구조 규칙
- 변경하지 않을 영역: API 경로와 응답, 비즈니스 로직, DB 테이블과 데이터, 공통 패키지 구조
- 구현 방향: 각 도메인 아래를 `controller`, `dto`, `entity`, `repository`, `service` 역할로 나누고 패키지와 import를 함께 변경한다.
- 검증 방법: 전체 테스트, 이전 패키지 참조 검색, 문서·공백 검사

## 변경 결과

- 핵심 변경:
  - `menu` 도메인을 `controller`, `dto`, `entity`, `repository`, `service` 패키지로 분리했다.
  - `point` 도메인을 `controller`, `dto`, `entity`, `repository`, `service` 패키지로 분리했다.
  - `user` 도메인을 `entity`, `repository` 패키지로 분리했다.
  - 메뉴와 포인트 컨트롤러 테스트를 실제 컨트롤러 패키지 구조에 맞춰 이동했다.
  - 프로젝트 프로필에 도메인과 공통 패키지 구성 규칙을 추가했다.
- 변경 파일:
  - `src/main/java/com/example/cafe/menu/`의 도메인 소스
  - `src/main/java/com/example/cafe/point/`의 도메인 소스
  - `src/main/java/com/example/cafe/user/`의 도메인 소스
  - `src/test/java/com/example/cafe/menu/controller/MenuControllerTest.java`
  - `src/test/java/com/example/cafe/point/controller/PointControllerTest.java`
  - `docs/project-profile.md`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - 이전 도메인 루트 패키지 선언과 import 검색: 남은 참조 없음
  - `./gradlew test`: 성공, 전체 테스트 16개 통과
  - `git diff --check`: 성공, 공백 오류 없음
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
