# TEST_STRATEGY.md

spring-gift-test-kakao 프로젝트의 **인수 테스트 전략** 문서.
RestAssured 기반으로 작성되었으며, 다음 7가지 축으로 구성한다:

1. 핵심 기능 분석
2. 시나리오 정의 (우선순위 포함)
3. 테스트 데이터 전략
4. 검증 전략
5. 커버리지 매트릭스
6. 주요 의사결정
7. 향후 개선 방향 + 변경 트리거 규칙

---

## 0. 핵심 기능 분석

사용자가 수행할 수 있는 행동 5가지를 REST 엔드포인트 기준으로 정리한다.

| 행동 | 엔드포인트 | 핵심 흐름 |
|------|-----------|----------|
| 카테고리 생성 | POST /api/categories | name → 저장 → Category 반환 |
| 카테고리 목록 조회 | GET /api/categories | findAll → List 반환 |
| 상품 생성 | POST /api/products | categoryId 검증 → 저장 → Product 반환 |
| 상품 목록 조회 | GET /api/products | findAll → List 반환 |
| **선물하기** | POST /api/gifts | Option 조회 → 재고 차감 → Gift 생성 → 배달 |

### 재고 변경 흐름 (선물하기의 핵심 부도메인)

```
GiftRestController.give(request, memberId)
  └→ GiftService.give(request, memberId)        // @Transactional
       ├→ optionRepository.findById(optionId)    // 실패: NoSuchElementException → 500
       ├→ option.decrease(quantity)               // 실패: IllegalStateException → 500
       │    └→ if (this.quantity < quantity) throw
       │    └→ this.quantity -= quantity           // JPA 더티체킹으로 자동 UPDATE
       ├→ new Gift(from, to, option, qty, msg)   // 값 객체 (DB 저장 안 됨)
       └→ giftDelivery.deliver(gift)             // FakeGiftDelivery: sender Member 조회
            └→ memberRepository.findById(from)    // 실패: NoSuchElementException → 500
```

REST 미노출 엔티티: Option, Member, Wish — 인수 테스트에서 `@Autowired` Repository로 직접 셋업.

---

## 1. 시나리오 정의

4개의 시나리오를 P0/P1/P2 우선순위로 분류한다.
P0은 회귀 시 즉시 차단, P1은 다음 스프린트 내 수정, P2는 백로그.

| 우선순위 | 시나리오 | 근거 |
|---------|---------|------|
| **P0** | 시나리오 1: 상품 등록 및 조회 | 기본 CRUD — 전체 기능의 전제 조건 |
| **P0** | 시나리오 2: 선물하기 성공 | 핵심 비즈니스 흐름 + 재고 상태 변경 |
| **P0** | 시나리오 3: 재고 부족 선물 실패 | 도메인 불변식 보호 — 깨지면 데이터 정합성 훼손 |
| **P1** | 시나리오 4: 존재하지 않는 옵션 선물 실패 | 방어 케이스 — 잘못된 입력에 대한 안전망 |

### 시나리오 1: 상품 등록 및 조회 (운영자) `P0`

운영자가 판매할 상품을 시스템에 등록하는 가장 기본적인 흐름.

| 단계 | 행동 | 검증 |
|------|------|------|
| 1 | `POST /api/categories` — 카테고리 생성 | 200 + `id` 반환 |
| 2 | `POST /api/products` — 위 카테고리 ID로 상품 생성 | 200 + `id`, `name`, `category` 반환 |
| 3 | `GET /api/products` — 상품 목록 조회 | 200 + 생성한 상품 포함 확인 |

커버 범위:
- Category 생성 → Product 생성 → Product 조회의 전체 흐름
- 엔티티 간 참조(Category → Product) 정합성

### 시나리오 2: 선물하기 성공 (사용자) `P0`

이 서비스의 가장 핵심적인 "선물하기" 기능의 성공 경로.

| 단계 | 행동 | 검증 |
|------|------|------|
| 사전 조건 | Repository로 Member(sender/receiver), Category, Product, Option(재고 10) 준비 | — |
| 1 | `POST /api/gifts` — `Member-Id` 헤더 + `{ optionId, quantity: 3, receiverId, message }` | 200 (empty body) |
| 2 | DB 조회 — `optionRepository.findById(...)` | 재고가 10 → 7로 차감 확인 |

커버 범위:
- `GiftService.give(...)` 전체 흐름 (Option 조회 → 재고 차감 → Gift 생성 → 배달)
- JPA 더티체킹에 의한 재고 UPDATE가 실제 DB에 반영되는지 E2E 검증
- `Gift`는 값 객체(DB 저장 안 됨)이므로 DB 검증 대상이 아님

### 시나리오 3: 재고 부족 선물하기 실패 (사용자) `P0`

"재고가 부족하면 선물할 수 없다"는 핵심 비즈니스 규칙 검증.

| 단계 | 행동 | 검증 |
|------|------|------|
| 사전 조건 | Repository로 Member(sender/receiver), Category, Product, Option(**재고 1**) 준비 | — |
| 1 | `POST /api/gifts` — quantity: **2** (재고 초과) | 500 (`IllegalStateException`, `@ControllerAdvice` 없음) |
| 2 | DB 조회 — `optionRepository.findById(...)` | 재고가 1 그대로 (트랜잭션 롤백 확인) |

커버 범위:
- `Option.decrease()` 도메인 불변식이 API 레벨까지 전파되는지
- `@Transactional` 롤백으로 재고 미차감 보장

### 시나리오 4: 존재하지 않는 옵션으로 선물 실패 (사용자) `P1`

잘못된 optionId에 대한 방어 케이스. `orElseThrow()` 실패 경로 검증.

| 단계 | 행동 | 검증 |
|------|------|------|
| 사전 조건 | Repository로 Member(sender/receiver) 준비. Option은 **생성하지 않음** | — |
| 1 | `POST /api/gifts` — 존재하지 않는 optionId: **999** | 500 (`NoSuchElementException`, `@ControllerAdvice` 없음) |

커버 범위:
- `optionRepository.findById(optionId).orElseThrow()` 실패 경로가 API까지 전파되는지
- 시나리오 3(재고 부족)과 함께 Gift 실패 경로 완성

### 제외 범위

아래 항목은 인수 테스트에서 **의도적으로 제외**한다.

| 제외 항목 | 이유 |
|----------|------|
| Option CRUD API | REST 미노출. 엔드포인트 없음 |
| Member CRUD API | REST 미노출. 엔드포인트 없음 |
| Wish CRUD API | REST 미노출. 엔드포인트 없음 |
| 카카오 외부 연동 | `FakeGiftDelivery`로 대체됨. 외부 의존 제거 |
| 동시성 (재고 차감 경합) | 현재 낙관적/비관적 락 미구현. 도입 후 별도 테스트 |
| 인증/인가 | 현재 `Member-Id` 헤더만 사용. 토큰 검증 로직 없음 |
| `Option.decrease()` 단위 테스트 | 도메인 단위 테스트 범위. 이 문서는 인수 테스트만 다룸 |
| `GiftService` 단위 테스트 | 서비스 단위 테스트 범위. 별도 계획 |

---

## 2. 테스트 데이터 전략

### 준비 (Setup)

REST 엔드포인트 있는 엔티티는 API로 생성한다 (인수 테스트의 원칙에 부합):

```java
// RestAssured로 카테고리 생성 후 id 추출
Long categoryId =
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "식품"))
    .when()
        .post("/api/categories")
    .then()
        .statusCode(200)
        .extract().jsonPath().getLong("id");
```

REST 미노출 엔티티(Option, Member)는 `@Autowired` Repository로 직접 저장:

```java
@Autowired MemberRepository memberRepository;
@Autowired OptionRepository optionRepository;

Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
Option option = optionRepository.save(new Option("Tall", 10, product));
```

### 정리 (Teardown)

`@DirtiesContext(AFTER_EACH_TEST_METHOD)`:
- 매 테스트 후 ApplicationContext 재생성 → H2 인메모리 DB 완전 초기화
- 테스트 간 데이터 격리 보장
- 테스트 4개 규모에서는 성능 문제 없음

---

## 3. 검증 전략

### 검증 경계 원칙

| 구분 | 원칙 | 예시 |
|------|------|------|
| **셋업** | REST API 있으면 API로, 없으면 Repository 허용 | Category → API / Option → Repository |
| **검증 (기본)** | API 응답으로 검증 (상태 코드 + 본문) | `statusCode(200)` + `body("name", equalTo(...))` |
| **검증 (예외)** | API 응답에 상태 변화가 드러나지 않을 때만 DB 검증 | 재고 차감: `void` 반환이므로 `optionRepository`로 확인 |

> 원칙: **셋업은 Repository 허용, 검증은 API 우선, 상태 변화만 예외적으로 DB 검증.**

### 상태 코드 검증 — 모든 테스트의 기본

```java
.then()
    .statusCode(200)  // 또는 500
```

### 응답 본문 검증 — 생성/조회 API

```java
.then()
    .statusCode(200)
    .body("id", notNullValue())
    .body("name", equalTo("식품"))
```

### 리스트 검증 — 목록 조회 API

```java
.then()
    .statusCode(200)
    .body("size()", greaterThanOrEqualTo(1))
```

### DB 상태 검증 — 재고 차감처럼 응답 본문이 없는 경우

```java
// 선물 API 호출 후
Option updated = optionRepository.findById(option.getId()).orElseThrow();
assertThat(updated.getQuantity()).isEqualTo(7); // 10 - 3
```

### void 반환 API 검증 — Gift API는 응답 본문 없음

```java
.then()
    .statusCode(200)
    .body(emptyOrNullString())
```

---

## 4. 커버리지 매트릭스

엔드포인트별로 Happy / Failure / 상태 변화 검증 커버 여부를 정리한다.
빈 칸은 현재 커버하지 않는 구간이다.

| 엔드포인트 | Happy path | Failure path | 상태 변화 DB 검증 | 시나리오 |
|-----------|:----------:|:------------:|:-----------------:|---------|
| POST /api/categories | O | — | — | #1 |
| GET /api/categories | O | — | — | #1 |
| POST /api/products | O | — | — | #1 |
| GET /api/products | O | — | — | #1 |
| POST /api/gifts (성공) | O | — | O (재고 차감) | #2 |
| POST /api/gifts (재고 부족) | — | O (500) | O (롤백 확인) | #3 |
| POST /api/gifts (옵션 미존재) | — | O (500) | — | #4 |

**현재 미커버 구간** (P2 — 필요 시 추가):

| 구간 | 이유 |
|------|------|
| POST /api/products — categoryId 미존재 실패 | 시나리오 1에서 간접적으로 전제하지만 명시적 실패 테스트 없음 |
| POST /api/gifts — Member-Id 헤더 누락 (400) | Spring 프레임워크 레벨 검증. 비즈니스 로직과 무관 |
| GET /api/categories — 빈 리스트 경계값 | 데이터 없는 상태의 경계값. 현재 시나리오에서 미포함 |

---

## 5. 주요 의사결정

| 결정 | 선택 | 이유 |
|------|------|------|
| HTTP 클라이언트 | RestAssured | given/when/then 구조로 가독성 높음. 인수 테스트 표준 |
| 격리 전략 | `@DirtiesContext` | 4개 테스트 규모에서 가장 단순하고 확실한 격리. 추후 `@Sql` truncate로 전환 가능 |
| 베이스 클래스 | `AcceptanceTestBase` | `@SpringBootTest(RANDOM_PORT)` + `RestAssured.port` 설정을 공유 |
| 미노출 엔티티 셋업 | Repository 직접 | Option/Member에 REST API 없음. Repository `@Autowired`가 유일한 방법 |
| 요청 본문 | `Map<String, Object>` | DTO에 setter 없음. Map은 Jackson이 바로 직렬화 |
| 에러 상태 코드 | 500 기대 | `@ControllerAdvice` 없으므로 모든 예외는 500. 추후 핸들러 추가 시 변경 |

### AcceptanceTestBase 구성

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public abstract class AcceptanceTestBase {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
    }
}
```

### build.gradle 의존성

```gradle
testImplementation 'io.rest-assured:rest-assured'
```

`spring-boot-starter-test`의 BOM이 버전을 관리하므로 버전 명시 불필요.

---

## 6. Cucumber (Gherkin) 도입 — High Risk 시나리오

### 도입 배경

기존 RestAssured 코드는 개발자만 읽을 수 있다. **Risk = Impact x Likelihood x Detection Cost** 기준으로
High Risk(>= 12) 시나리오만 Cucumber 자동화하여 비즈니스 담당자도 읽을 수 있는 테스트로 전환한다.

### 적용 범위

| 시나리오 | Risk | Cucumber | 근거 |
|---------|------|----------|------|
| 선물하기 성공 (재고 차감) | **High (15)** | ✅ | 핵심 비즈니스 + 데이터 정합성 |
| 재고 부족 선물 실패 | **High (15)** | ✅ | 과매도 방지 + 트랜잭션 롤백 |
| 옵션 미존재 선물 실패 | **High (12)** | ✅ | 방어 로직 |
| 카테고리/상품 CRUD | Medium (6-8) | ❌ | RestAssured JUnit 유지 |

### 구조

```
src/test/resources/features/gift.feature       # 한국어 Gherkin (# language: ko)
src/test/java/gift/cucumber/
├── CucumberSpringConfiguration.java           # @CucumberContextConfiguration + @MockBean
├── CucumberSuiteTest.java                     # @Suite + @IncludeEngines("cucumber")
├── ScenarioContext.java                       # @ScenarioScope — Step 간 상태 공유
├── DatabaseCleanerHook.java                   # Cucumber @Before — DB truncate
├── RestAssuredHook.java                       # Cucumber @Before — RestAssured 포트 설정
└── steps/
    ├── CommonSteps.java                       # 공통 응답 검증
    └── GiftSteps.java                         # 선물하기 Steps + Mock 검증
```

### 실행

```bash
./gradlew test                                    # JUnit + Cucumber 동시 실행
```

리포트: `build/reports/cucumber/cucumber-report.html`

---

## 7. 테스트 더블 전략 — GiftDelivery 격리

### 문제

`FakeGiftDelivery`는 `MemberRepository.findById()`를 호출하여 비즈니스 로직 테스트와 무관한 외부 의존이 발생한다.

### 해결

`@MockBean GiftDelivery`로 `FakeGiftDelivery`를 대체한다.

| 적용 위치 | 방식 |
|----------|------|
| `CucumberSpringConfiguration` | `@MockBean GiftDelivery` |
| `GiftAcceptanceTest` | `@MockBean GiftDelivery` |

### 검증 항목

- 성공 시: `verify(giftDelivery, times(1)).deliver(any())` — 배달 호출됨
- 실패 시: `verify(giftDelivery, never()).deliver(any())` — 배달 미호출

프로덕션 코드(`FakeGiftDelivery`) 변경 없음.

---

## 8. 테스트 격리 — @DirtiesContext → JdbcTemplate truncate

### 변경 사항

- `@DirtiesContext(AFTER_EACH_TEST_METHOD)` 제거
- `@BeforeEach`에서 `JdbcTemplate`으로 전 테이블 TRUNCATE
- `@ActiveProfiles("test")`로 `application-test.properties` 활성화

### 이유

- `@DirtiesContext`는 매 테스트마다 ApplicationContext를 재생성하여 느림
- TRUNCATE 방식은 컨텍스트를 공유하면서 데이터만 초기화

---

## 9. 향후 개선 방향

- `@ControllerAdvice` 추가 → 에러 상태 코드 세분화 (500 → 404/400/409)
- `@Valid` 추가 → 유효성 검증 테스트
- 낙관적/비관적 락 도입 → 동시성 테스트 (AC-GFT-05 자동화 승격)

### 변경 트리거 규칙

아래 변경이 발생하면 해당 테스트 기대값을 반드시 업데이트한다.

| 변경 트리거 | 영향받는 테스트 | 업데이트 내용 |
|------------|---------------|-------------|
| `@ControllerAdvice` 도입 | 시나리오 3, 4 | `statusCode(500)` → 세분화된 코드 (`404`, `400`, `409` 등) |
| `@Valid` (Bean Validation) 추가 | 시나리오 1 (상품 생성) | 빈 이름, 음수 가격 등 유효성 실패 테스트 추가 (400) |
| Gift API 응답 본문 추가 | 시나리오 2 | `body(emptyOrNullString())` → 응답 필드 검증으로 변경 |
| API 경로 변경 | 전체 시나리오 | RestAssured 요청 URL 일괄 수정 |
| `Member-Id` 헤더 → 토큰 인증 전환 | 시나리오 2, 3, 4 | 헤더 셋업 방식 변경 + 인증 실패 시나리오 추가 |
| 낙관적/비관적 락 도입 | 시나리오 2, 3 | 동시성 테스트 시나리오 추가 (P2 → P1 승격) |
| Option/Member REST 노출 | 시나리오 2, 3, 4 | Repository 셋업 → API 셋업으로 전환 + 제외 범위 표 업데이트 |
