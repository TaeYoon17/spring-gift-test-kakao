---
name: acceptance-test
description: >
  RestAssured 기반 인수테스트를 생성·수정·실행한다.
  TEST_STRATEGY.md의 시나리오(P0/P1)를 코드로 구현하며,
  실행 후 결과를 보고한다.
  트리거: "인수테스트", "acceptance test", "E2E 테스트",
  "/acceptance-test", 또는 특정 도메인 테스트 요청.
argument-hint: "[domain] (category | product | gift | all)"
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
---

# Acceptance Test Skill

대상: `$ARGUMENTS` (없으면 `all`)

## 0) 목적

- 코드 기준 사실만 사용해 인수테스트를 작성한다.
- 존재하지 않는 API/DTO/필드를 추측해 만들지 않는다.
- 테스트 작성 후 반드시 실행해서 결과를 보고한다.

## 1) 입력 해석 규칙

`$ARGUMENTS`를 아래 규칙으로 정규화한다.

- `/api/categories` 또는 `category` -> `category`
- `/api/products` 또는 `product` -> `product`
- `/api/gifts` 또는 `gift` -> `gift`
- `all` 또는 빈 값 -> `all`
- 그 외 값 -> 먼저 코드에서 일치 엔드포인트를 찾고, 못 찾으면 명확히 불가 사유를 보고한다.

## 2) 사전 조사 (필수)

아래를 순서대로 확인한다.

1. 컨트롤러: `src/main/java/gift/ui/`
2. 서비스/DTO: `src/main/java/gift/application/`
3. 엔티티/리포지토리: `src/main/java/gift/model/`
4. 기존 테스트: `src/test/java/gift/`
5. 프로젝트 규칙: `CLAUDE.md`, `README.md`

## 3) 현재 프로젝트 기준 사실(우선 참고)

### 공개 API

| Method | Path | Header | Body |
|--------|------|--------|------|
| POST | `/api/categories` | - | `{"name":"..."}` |
| GET | `/api/categories` | - | - |
| POST | `/api/products` | - | `{"name":"...","price":0,"imageUrl":"...","categoryId":0}` |
| GET | `/api/products` | - | - |
| POST | `/api/gifts` | `Member-Id` | `{"optionId":0,"quantity":0,"receiverId":0,"message":"..."}` |

### 도메인 제약

- `Option.decrease(int)`는 재고 부족 시 `IllegalStateException` 발생
- `GiftService.give(...)` 순서: Option 조회 -> 재고 감소 -> Gift 생성 -> GiftDelivery.deliver()
- 예외 핸들러(`@ControllerAdvice`)가 없어서 실패 케이스는 기본 에러 응답(주로 500)일 수 있음

### 생성자 (Repository 직접 저장 시)

- `Category(String name)`
- `Product(String name, int price, String imageUrl, Category category)`
- `Option(String name, int quantity, Product product)`
- `Member(String name, String email)`

## 4) 테스트 작성 규칙

### 기술/스타일

- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
- `TestRestTemplate` 사용
- JUnit5 + AssertJ 사용
- 테스트명은 목적이 드러나게 작성 (`@DisplayName`)
- `given/when/then` 구조 유지

### 파일 규칙

- 경로: `src/test/java/gift/{Domain}AcceptanceTest.java`
- 클래스명: `{Domain}AcceptanceTest`
- 패키지: `gift`

### 데이터 준비 규칙

- REST 노출 엔티티(Category, Product)는 가능한 API로 생성
- REST 미노출 엔티티(Option, Member)는 Repository로 직접 저장
- 테스트 간 의존 금지 (각 테스트가 독립적으로 준비)

### 필수 검증 항목

- HTTP 상태 코드
- 핵심 응답 필드 (예: id/name)
- 생성 후 조회 일관성
- 최소 1개 실패 케이스

## 5) 도메인별 최소 시나리오

### category

1. 카테고리 생성 성공
2. 카테고리 목록 조회 시 생성 데이터 포함

### product

1. 카테고리 선행 생성 후 상품 생성 성공
2. 상품 목록 조회 시 생성 데이터 포함

### gift

1. 성공 케이스: 선물 요청 성공(2xx) + Option 재고 감소 확인
2. 실패 케이스: 재고 초과 요청 시 실패(5xx) 확인

### all

- `category`, `product`, `gift` 시나리오를 모두 반영

## 6) 구현 템플릿

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DomainAcceptanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SomeRepository someRepository; // REST 미노출 엔티티용

    @Test
    @DisplayName("시나리오 설명")
    void scenario() {
        // given

        // when
        ResponseEntity<SomeType> response = restTemplate.postForEntity(
            "/api/path",
            request,
            SomeType.class
        );

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
```

## 7) 실행/검증 (필수)

작성 또는 수정 후 반드시 테스트를 실행한다.

```bash
./gradlew test --tests "gift.{Domain}AcceptanceTest"
./gradlew test
```

실패하면 원인 분석 후 테스트/코드를 재수정하고 재실행한다.

## 8) 최종 보고 포맷

결과 보고에는 아래 3가지를 포함한다.

1. 수정 파일 목록
2. 추가/수정한 시나리오 요약
3. 실행한 검증 명령과 결과(성공/실패, 핵심 로그)
