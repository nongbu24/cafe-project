# Gradle toolchain resolver 설정

## 목표

Gradle 10 업그레이드 전에 JDK 21 toolchain 자동 다운로드 경로를 명시한다.

## 작업 내용

- `settings.gradle`에 Foojay toolchain resolver convention 플러그인을 추가한다.
- 현재 터미널 Java 21 전환 방법을 확인한다.

## 검증

- `./gradlew -q javaToolchains`: 성공
