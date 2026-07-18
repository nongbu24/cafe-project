# 회원가입 중복 username 저장 충돌 처리

## 목적

- 회원가입 요청이 동시에 들어와 사전 중복 검사를 통과하더라도, 저장 시점의 DB unique 제약 충돌을
  API 계약의 `DUPLICATE_USERNAME` 오류로 변환한다.

## 변경

- `AuthService.signup()`에서 `userFacade.save()` 중 발생한 `DataIntegrityViolationException`을
  `ApplicationException(ErrorCode.DUPLICATE_USERNAME)`으로 변환한다.
- 저장 시점 충돌 변환을 확인하는 서비스 단위 테스트를 추가한다.

## 검증

- `./gradlew test --tests 'com.example.cafe.auth.*'`: 성공
