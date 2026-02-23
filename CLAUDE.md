# CLAUDE.md

이 문서는 이 저장소에서 작업하는 AI Agent용 실행 가이드다.
기준은 "빠른 수정"보다 "기존 동작 보존 + 검증 가능한 변경"이다.

## 1) TL;DR (먼저 읽기)

1. 코드 기준 사실만 사용한다. 추측 금지.
2. 공개 API 계약(경로, 헤더, JSON 필드)은 근거 없이 바꾸지 않는다.
3. 계층 경계를 지킨다. (`ui -> application -> model`, `infrastructure`는 구현체)
4. 수정 후 최소 1개 이상 검증 명령을 실행한다.
5. 보고 시 `수정 파일`, `동작 변화`, `검증 결과`를 반드시 남긴다.

## 2) 프로젝트 스냅샷

- 스택: Java 21, Spring Boot 3.5.8, Spring Data JPA, H2
- 빌드: Gradle 8.4
- 성격: 선물/상품 도메인 REST API
- 핵심 설정:
  - `spring.jpa.open-in-view=false`
  - `gift.Application`에 `@ConfigurationPropertiesScan`
- 테스트 현황: `src/test`에 테스트 소스 없음

## 3) 빠른 시작 명령

```bash
./gradlew build
./gradlew test
./gradlew bootRun
./gradlew test --tests "gift.SomeTestClass.someMethod"
```

## 4) 변경 전 확인 순서 (필수)

1. 컨트롤러: `src/main/java/gift/ui/`
2. 서비스/요청 DTO: `src/main/java/gift/application/`
3. 엔티티/리포지토리: `src/main/java/gift/model/`
4. 인프라 구현: `src/main/java/gift/infrastructure/`
5. 문서: `README.md`, `CLAUDE.md`

## 5) 패키지 경계와 책임

```text
src/main/java/gift/
├── ui/             # REST 컨트롤러만
├── application/    # 유스케이스, 트랜잭션 경계, 요청 DTO
├── model/          # 엔티티, 리포지토리, 도메인 규칙, 포트 인터페이스
└── infrastructure/ # 외부 연동 구현체
```

세부 규칙:
- `ui`에서 Repository 직접 접근 금지
- `application`에서 유스케이스 조합/흐름 제어
- `model`은 도메인 상태/규칙 보유
- `infrastructure`는 포트 구현체만 담당

## 6) 공개 API 계약 (현재 코드 기준)

| Method | Path | Body | Header | 서비스 |
|--------|------|------|--------|--------|
| `POST` | `/api/categories` | `{"name":"..."}` | - | `CategoryService.create()` |
| `GET` | `/api/categories` | - | - | `CategoryService.retrieve()` |
| `POST` | `/api/products` | `{"name":"...","price":0,"imageUrl":"...","categoryId":0}` | - | `ProductService.create()` |
| `GET` | `/api/products` | - | - | `ProductService.retrieve()` |
| `POST` | `/api/gifts` | `{"optionId":0,"quantity":0,"receiverId":0,"message":"..."}` | `Member-Id` | `GiftService.give()` |

참고:
- `OptionService`, `WishService`는 현재 REST 미노출
- 글로벌 예외 핸들러(`@ControllerAdvice`) 없음

## 7) 도메인 모델과 핵심 불변식

엔티티 관계:

```text
Category <- Product <- Option
Member   <- Wish -> Product
Gift (값 객체, JPA 엔티티 아님)
```

핵심 불변식:
- `Option.decrease(int)`는 재고 부족 시 `IllegalStateException`
- `GiftService.give(...)` 순서:
  1. Option 조회
  2. 재고 차감
  3. Gift 생성
  4. `GiftDelivery.deliver(...)` 호출
- `ProductService.create(...)`는 `categoryId` 미존재 시 실패 (`orElseThrow()`)

## 8) 작업 규칙

- 생성자 주입 유지
- 기존 스타일 유지 (`final` 파라미터, `create/retrieve/give` 네이밍)
- 요구사항과 무관한 리팩터링/대규모 포맷 변경 금지
- `application.properties`의 토큰/URL 임의 변경 금지

## 9) 변경 영향 점검표

다음 변경은 반드시 영향 확인 후 진행:

- API 경로/헤더/요청 필드 변경
- `orElseThrow()` 예외 타입 또는 예외 처리 방식 변경
- 트랜잭션 경계 변경 (`@Transactional` 위치/범위)
- `GiftService` 선물 흐름 순서 변경

## 10) 검증 규칙

- 가능하면 작은 범위부터 실행:
  1. 단일 테스트
  2. 관련 테스트
  3. 전체 테스트 (`./gradlew test`)
- 테스트가 없으면 최소 `./gradlew test` 또는 `./gradlew build` 실행
- 실행 불가 시 미실행 항목과 리스크를 명시

## 11) 보고 템플릿

작업 완료 보고에는 아래 3개를 포함한다.

1. 수정 파일 목록
2. 동작 변화 요약 (호환성 영향 포함)
3. 실행한 검증 명령과 결과

예시:

```text
[수정 파일]
- src/main/java/gift/application/GiftService.java
- README.md

[동작 변화]
- 선물 실패 케이스 응답을 명시적으로 매핑함 (기존 500 -> 400)

[검증]
- ./gradlew test --tests "gift.GiftAcceptanceTest" : SUCCESS
- ./gradlew test : SUCCESS
```
