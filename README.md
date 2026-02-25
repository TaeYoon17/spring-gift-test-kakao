# spring-gift-test

## 전제조건

- Java 21
- Gradle 8.4 (wrapper 포함)
- Docker (Docker Desktop 또는 Docker Engine)

## 빌드

```bash
./gradlew build
```

## 애플리케이션 실행

```bash
./gradlew bootRun
```

## 테스트 실행

### 전체 테스트

```bash
./gradlew test
```

### Cucumber BDD 테스트 (PostgreSQL)

Docker가 실행 중이어야 한다. Spring Boot Docker Compose 모듈이 PostgreSQL 컨테이너를 자동으로 시작/종료한다.

```bash
./gradlew cucumberTest
```

### Cucumber 시나리오 구조

테스트는 Cucumber BDD 기반으로 작성되어 있다. 한국어 Gherkin 문법(`조건`/`만일`/`그러면`)을 사용하며, 비개발자도 시나리오를 읽고 이해할 수 있다.

**Feature 파일 위치:** `src/test/resources/features/`

| Feature 파일 | 도메인 | 시나리오 수 |
|---|---|---|
| `category.feature` | 카테고리 생성/조회 | 2 |
| `product.feature` | 상품 생성/조회 | 2 |
| `gift.feature` | 선물하기 (성공/실패) | 3 |

**Step Definitions:** `src/test/java/gift/cucumber/steps/`

### Cucumber 리포트

테스트 실행 후 HTML 리포트가 생성된다:

```
build/reports/cucumber/cucumber-report.html
```

### 트러블슈팅

- **Docker 미실행 시 테스트 실패**: Docker Desktop이 실행 중인지 확인한다.
- **포트 충돌**: `docker-compose.yml`에서 PostgreSQL은 호스트 포트 `15432`를 사용한다. 해당 포트가 사용 중이면 변경한다.
- **컨테이너 수동 정리**: `docker compose down` 명령으로 남은 컨테이너를 정리할 수 있다.
