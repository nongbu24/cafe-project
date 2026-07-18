# 도메인 facade 도입

## 작업 목적

타 도메인의 repository를 직접 참조하는 서비스 의존을 줄이고, 도메인 간 협력 지점을 facade로 분리한다.

## 변경 범위와 계획

- 변경할 영역: `auth`, `menu`, `order`, `point`, `user` 도메인의 서비스 계층 의존 구조
- 변경하지 않을 영역: REST API 계약, DB 구조, 엔티티 매핑, 같은 도메인 내부 repository 사용
- 구현 방향: 각 도메인 `facade` 패키지에 facade를 추가하고, 타 도메인 repository 직접 주입을 facade 주입으로 교체한다.
- 검증 방법: 관련 자동 테스트 또는 전체 테스트 실행, `git diff --check`

## 변경 결과

- 핵심 변경: 타 도메인의 repository를 직접 주입받던 서비스가 각 도메인의 facade를 통해 필요한 기능을 호출하도록 변경했다.
- 변경 파일:
  - `docs/project-profile.md`
  - `src/main/java/com/example/cafe/user/facade/UserFacade.java`
  - `src/main/java/com/example/cafe/menu/facade/MenuFacade.java`
  - `src/main/java/com/example/cafe/point/facade/PointFacade.java`
  - `src/main/java/com/example/cafe/order/facade/OrderFacade.java`
  - `src/main/java/com/example/cafe/auth/service/AuthService.java`
  - `src/main/java/com/example/cafe/auth/service/AuthenticationInterceptor.java`
  - `src/main/java/com/example/cafe/menu/service/MenuService.java`
  - `src/main/java/com/example/cafe/order/service/OrderService.java`
  - `src/main/java/com/example/cafe/point/service/PointService.java`

## 검증과 남은 사항

- 실행한 검증과 결과:
  - `./gradlew test`: 성공
  - `git diff --check`: 성공
  - `rg "import com\\.example\\.cafe\\.(auth|menu|order|point|user)\\.repository" src/main/java/com/example/cafe -n`: 타 도메인 repository 직접 참조가 제거되고, facade 내부 또는 같은 도메인 서비스의 repository 참조만 남은 것을 확인
- 실행하지 못한 검증: 없음
- 남은 제한사항과 사용자 조치: 없음
