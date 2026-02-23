# API.md

이 문서는 현재 코드 기준의 공개 REST API 계약을 정리한다.
대상 독자:
- 사람: 기능/요청/응답을 빠르게 확인
- AI Agent: 변경 영향, 테스트 포인트, 예외 동작을 정확히 확인

## 1) Quick Contract

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- 인증/인가: 없음 (단, 선물 API는 `Member-Id` 헤더 필수)
- 전역 예외 핸들러(`@ControllerAdvice`): 없음
- 현재 공개 API: `categories`, `products`, `gifts`만 노출

| Method | Path | Request | Success | 비고 |
|--------|------|---------|---------|------|
| `POST` | `/api/categories` | body: `name` | `200` + Category | |
| `GET` | `/api/categories` | - | `200` + Category[] | |
| `POST` | `/api/products` | body: `name, price, imageUrl, categoryId` | `200` + Product | `categoryId`가 유효해야 함 |
| `GET` | `/api/products` | - | `200` + Product[] | |
| `POST` | `/api/gifts` | header: `Member-Id`, body: `optionId, quantity, receiverId, message` | `200` + empty body | 재고 차감 후 발송 |

## 2) 데이터 계약

### Category

```json
{
  "id": 1,
  "name": "음료"
}
```

### Product

```json
{
  "id": 1,
  "name": "아이스 아메리카노",
  "price": 4500,
  "imageUrl": "https://example.com/americano.png",
  "category": {
    "id": 1,
    "name": "음료"
  }
}
```

### Gift Request

```json
{
  "optionId": 10,
  "quantity": 1,
  "receiverId": 2,
  "message": "생일 축하해!"
}
```

## 3) Endpoint Detail

### 3.1 POST `/api/categories`

카테고리 생성.

Request:
```http
POST /api/categories
Content-Type: application/json
```

```json
{
  "name": "음료"
}
```

Response: `200 OK`
```json
{
  "id": 1,
  "name": "음료"
}
```

### 3.2 GET `/api/categories`

카테고리 목록 조회.

Request:
```http
GET /api/categories
```

Response: `200 OK`
```json
[
  {
    "id": 1,
    "name": "음료"
  }
]
```

### 3.3 POST `/api/products`

상품 생성. `categoryId`가 반드시 존재해야 한다.

Request:
```http
POST /api/products
Content-Type: application/json
```

```json
{
  "name": "아이스 아메리카노",
  "price": 4500,
  "imageUrl": "https://example.com/americano.png",
  "categoryId": 1
}
```

Response: `200 OK`
```json
{
  "id": 1,
  "name": "아이스 아메리카노",
  "price": 4500,
  "imageUrl": "https://example.com/americano.png",
  "category": {
    "id": 1,
    "name": "음료"
  }
}
```

Failure:
- 존재하지 않는 `categoryId` -> 기본적으로 `500 Internal Server Error`

### 3.4 GET `/api/products`

상품 목록 조회.

Request:
```http
GET /api/products
```

Response: `200 OK` + `Product[]`

### 3.5 POST `/api/gifts`

선물 발송 요청.
처리 흐름: Option 조회 -> 재고 차감 -> Gift 생성 -> 발송 처리.

Request:
```http
POST /api/gifts
Content-Type: application/json
Member-Id: 1
```

```json
{
  "optionId": 10,
  "quantity": 1,
  "receiverId": 2,
  "message": "생일 축하해!"
}
```

Response: `200 OK` (empty body)

Failure:
- 존재하지 않는 `optionId` -> 기본적으로 `500 Internal Server Error`
- 재고 부족(`IllegalStateException`) -> 기본적으로 `500 Internal Server Error`

중요:
- `Member`/`Option`은 현재 공개 API로 생성할 수 없다.
- 인수테스트/수동 테스트에서 `gifts`를 검증하려면 DB seed 또는 Repository 저장이 필요하다.

## 4) 에러 응답 정책 (현재 구현)

글로벌 예외 핸들러가 없어, 서버 예외는 Spring Boot 기본 에러 응답으로 반환된다.
필드는 설정/버전에 따라 달라질 수 있으므로 `status`, `error`, `path` 중심으로 검증한다.

예시:
```json
{
  "timestamp": "2026-02-23T12:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/gifts"
}
```

## 5) AI Agent 작업 체크리스트

API 변경 시 아래를 함께 점검한다.

1. 컨트롤러 경로/헤더/DTO 필드 일치 여부
2. 서비스 계층 예외/트랜잭션 동작 변화
3. `README.md`, `API.md`, 테스트 문서 동시 업데이트
4. 최소 1회 실행 검증 (`./gradlew test` 또는 대상 테스트)
