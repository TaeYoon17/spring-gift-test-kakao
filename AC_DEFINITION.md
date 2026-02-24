# AC_DEFINITION.md

선물/상품 도메인 인수 조건(Acceptance Criteria) 정의서.
리스크 기반 테스트 선택: **Risk = Impact x Likelihood x Detection Cost**.

---

## 리스크 평가 기준

| 수준 | Impact | Likelihood | Detection Cost | 자동화 수준 |
|------|--------|------------|----------------|-------------|
| **High** (>= 12) | 서비스 중단, 데이터 손실 | 변경 빈도 높음 | 수동 발견 어려움 | Cucumber 자동화 (필수) |
| **Medium** (6-11) | 일부 기능 저하 | 변경 빈도 보통 | 수동 발견 가능 | 샘플 자동화 또는 수동 |
| **Low** (<= 5) | 미미한 영향 | 변경 빈도 낮음 | 쉽게 발견 | 수동 QA |

---

## User Story 1: 카테고리 관리

**As a** 운영자
**I want to** 카테고리를 생성하고 조회할 수 있다
**So that** 상품 분류 체계를 관리할 수 있다

### Acceptance Criteria

| AC ID | 조건 | 행동 | 결과 | Risk | 자동화 |
|-------|------|------|------|------|--------|
| AC-CAT-01 | - | POST /api/categories {"name":"식품"} | 200 + id, name 반환 | Medium (8) | RestAssured JUnit |
| AC-CAT-02 | 카테고리 1개 이상 존재 | GET /api/categories | 200 + 목록에 포함 | Medium (6) | RestAssured JUnit |

- Risk 근거: CRUD 기본 기능. 실패 시 상품 등록 불가하지만, 변경 빈도가 낮고 수동 발견이 쉬움.
- 자동화 이유: 기존 RestAssured JUnit 테스트로 충분. Cucumber 자동화 대상 아님.

---

## User Story 2: 상품 관리

**As a** 운영자
**I want to** 카테고리에 속하는 상품을 등록하고 조회할 수 있다
**So that** 판매 가능한 상품 카탈로그를 구성할 수 있다

### Acceptance Criteria

| AC ID | 조건 | 행동 | 결과 | Risk | 자동화 |
|-------|------|------|------|------|--------|
| AC-PRD-01 | 유효한 categoryId | POST /api/products | 200 + id, name, category 반환 | Medium (8) | RestAssured JUnit |
| AC-PRD-02 | 상품 1개 이상 존재 | GET /api/products | 200 + 목록에 포함 | Medium (6) | RestAssured JUnit |
| AC-PRD-03 | 존재하지 않는 categoryId | POST /api/products | 500 | Low (4) | 수동 |

- Risk 근거: 상품 등록은 선물하기의 전제 조건이나, 단순 CRUD로 변경 빈도가 낮음.
- 자동화 이유: 기존 RestAssured JUnit 테스트로 충분. AC-PRD-03은 Low Risk로 수동 QA.

---

## User Story 3: 선물하기

**As a** 카카오 선물하기 사용자
**I want to** 친구에게 선물을 보내고 싶다
**So that** 특별한 날을 축하할 수 있다

### Acceptance Criteria

| AC ID | 조건 | 행동 | 결과 | Risk | 자동화 |
|-------|------|------|------|------|--------|
| AC-GFT-01 | 유효한 옵션, 충분한 재고 | POST /api/gifts | 200 + 재고 차감 + 배달 호출 | **High (15)** | **Cucumber 자동화** |
| AC-GFT-02 | 재고 부족 | POST /api/gifts | 500 + 재고 미변경 + 배달 미호출 | **High (15)** | **Cucumber 자동화** |
| AC-GFT-03 | 존재하지 않는 옵션 | POST /api/gifts | 500 + 배달 미호출 | **High (12)** | **Cucumber 자동화** |
| AC-GFT-04 | Member-Id 헤더 누락 | POST /api/gifts | 400 | Low (3) | 수동 |
| AC-GFT-05 | 동시 선물 발송 시 정합성 | POST /api/gifts (동시) | 재고 정합성 유지 | Medium (9) | 수동 (별도 부하 테스트) |

### AC-GFT-01: 재고 충분 → 선물 발송 성공 ✅ Cucumber 자동화

- **Risk: High (15)** = Impact(5) x Likelihood(3) x Detection Cost(1)
- Impact (5): 핵심 비즈니스 흐름. 실패 시 매출 직접 영향
- Likelihood (3): 재고/옵션 로직 변경 빈도 보통
- Detection Cost (1): 수동으로도 발견 가능하지만 재고 DB 상태 확인 필요
- **자동화 이유**: 재고 관리는 핵심 비즈니스 로직. 데이터 정합성이 critical.

### AC-GFT-02: 재고 부족 → 선물 발송 실패 ✅ Cucumber 자동화

- **Risk: High (15)** = Impact(5) x Likelihood(3) x Detection Cost(1)
- Impact (5): 재고 정합성 훼손 시 과매도 → 데이터 손실
- Likelihood (3): Option.decrease() 변경 시 영향
- Detection Cost (1): 재고 상태 확인 필요
- **자동화 이유**: 실패 시나리오로 트랜잭션 롤백과 상태 변화 미발생을 증명.

### AC-GFT-03: 존재하지 않는 옵션 → 선물 실패 ✅ Cucumber 자동화

- **Risk: High (12)** = Impact(4) x Likelihood(3) x Detection Cost(1)
- Impact (4): 잘못된 입력에 대한 방어. 실패 시 시스템 불안정
- Likelihood (3): orElseThrow() 패턴 변경 가능성
- Detection Cost (1): 수동 발견 가능
- **자동화 이유**: 방어 로직 검증. 재고 부족과 함께 실패 경로 완성.

### AC-GFT-04: Member-Id 헤더 누락 ❌ 수동

- **Risk: Low (3)** — Spring 프레임워크 레벨에서 자동 처리. 비즈니스 로직과 무관.

### AC-GFT-05: 동시 선물 발송 시 정합성 ❌ 수동

- **Risk: Medium (9)** — 현재 낙관적/비관적 락 미구현. 별도 부하 테스트로 검증 필요.

---

## 테스트 더블 전략

| 유형 | 용도 | 적용 대상 |
|------|------|----------|
| **Mock** | 호출 검증 + 행위 격리 | `GiftDelivery` — `@MockBean`으로 `FakeGiftDelivery` 대체 |

### GiftDelivery Mock 적용 근거

- `FakeGiftDelivery`는 `MemberRepository.findById()`를 호출하여 비즈니스 로직 테스트와 무관한 의존성 발생
- Mock으로 격리하면 `verify()`로 배달 서비스 호출 여부를 명확히 검증 가능
- 프로덕션 코드(`FakeGiftDelivery`) 변경 없이 테스트 격리 달성
