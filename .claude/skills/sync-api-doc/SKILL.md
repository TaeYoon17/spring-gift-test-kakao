---
name: sync-api-doc
description: >
  코드 기준으로 API.md를 동기화한다.
  컨트롤러·DTO·엔티티를 읽어 문서와 비교하고,
  불일치 구간을 갱신한다.
  트리거: "API 문서 동기화", "API.md 업데이트",
  "/sync-api-doc", 또는 API 변경 후 문서 반영 요청.
argument-hint: "[domain] (category | product | gift | all)"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Sync API Doc Skill

대상: `$ARGUMENTS` (없으면 `all`)

## 0) 목적

- `API.md`를 코드와 일치시키는 것이 유일한 목표다.
- 추측 금지: 코드에 없는 엔드포인트/필드/상태코드/헤더를 문서에 넣지 않는다.
- 코드에서 삭제된 API는 문서에서도 제거한다.
- 사람과 AI가 모두 읽기 쉽도록 섹션 구조를 유지한다.

## 1) 입력 정규화 (필수)

`$ARGUMENTS`를 아래 규칙으로 정규화한다.

- `category` 또는 `/api/categories` -> `category`
- `product` 또는 `/api/products` -> `product`
- `gift` 또는 `/api/gifts` -> `gift`
- `all` 또는 빈 값 -> `all`
- 그 외 값:
  1. `src/main/java/gift/ui/`에서 일치 경로를 검색
  2. 찾으면 해당 endpoint 기준 부분 갱신
  3. 못 찾으면 "대상 엔드포인트 없음"으로 보고하고 종료

## 2) 소스 수집 순서 (필수)

아래 순서를 지킨다. 읽지 않고 문서를 쓰지 않는다.

1. **컨트롤러** (`src/main/java/gift/ui/`)
   - `@RequestMapping`, `@GetMapping`, `@PostMapping` 등 경로/메서드
   - `@RequestHeader`, `@RequestBody`, `@PathVariable`, `@RequestParam`
   - 메서드 반환 타입 (`void`, 엔티티, DTO, 컬렉션)
2. **요청 DTO** (`src/main/java/gift/application/`)
   - 필드명/타입 (getter 기준 JSON 필드명)
3. **응답 엔티티/DTO** (`src/main/java/gift/model/` 또는 `application/`)
   - 응답 필드(직렬화 대상 getter)
   - 연관 엔티티 중첩 형태 (`@ManyToOne` 등)
4. **서비스 계층** (`src/main/java/gift/application/`)
   - 주요 흐름/실패 조건(예: `orElseThrow`, 도메인 예외)
5. **예외 핸들러** (`@ControllerAdvice` 검색)
   - 있으면 예외 -> 상태코드 매핑 정리
   - 없으면 기본 에러 정책 명시
6. **기존 문서** (`API.md`, 필요 시 `README.md`, `CLAUDE.md`)
   - 구조/용어 일관성 유지

## 3) 문서 구조 (고정)

`API.md`의 섹션 번호와 제목을 유지한다.

```markdown
# API.md

(서문: 코드 기준 계약 문서임을 명시)

## 1) Quick Contract
(전체 공개 API 요약)

## 2) 데이터 계약
(요청/응답 핵심 JSON 예시)

## 3) Endpoint Detail
(엔드포인트별 Request/Response/Failure)

## 4) 에러 응답 정책
(현재 구현의 에러 처리 방식)

## 5) AI Agent 작업 체크리스트
(문서-코드 동기화 시 점검 항목)
```

## 4) 작성 규칙

### 4.1 Quick Contract

- 모든 공개 엔드포인트를 한 테이블에 정리
- 컬럼: `Method`, `Path`, `Request`, `Success`, `비고`
- 부분 갱신(`category/product/gift`)이어도 테이블 전체 정합성 확인

### 4.2 데이터 계약

- DTO/엔티티 getter 기준으로 JSON 예시 작성
- 중첩 객체는 실제 응답 구조 기준으로만 기술
- 코드에 없는 validation 규칙(`@NotNull` 등)은 임의로 추가하지 않는다

### 4.3 Endpoint Detail

- 각 엔드포인트마다:
  - Request: 메서드/경로/헤더/바디
  - Response: 상태코드/바디
  - Failure: 실패 조건/현재 구현 기준 상태코드
- 상태코드는 "현재 코드 동작"으로 작성한다. 개선안을 섞지 않는다.

### 4.4 에러 응답 정책

- `@ControllerAdvice` 있으면: 매핑 테이블 작성
- 없으면: 기본 에러 응답 형식과 검증 기준(`status/error/path`) 명시

### 4.5 비공개/테스트 제약

- REST 미노출 서비스(`OptionService`, `WishService` 등)가 있으면 별도로 명시
- 특정 API 검증을 위해 사전 데이터가 필요한 경우 명시 (예: `gifts`의 `Option`, `Member`)

## 5) 실행 절차

1. 입력 정규화 (`all/category/product/gift`).
2. 수집 순서대로 코드 사실 추출.
3. `API.md`의 해당 섹션 갱신.
4. 아래 완료 체크를 통과할 때까지 보정.

## 6) 완료 체크 (필수)

아래 조건을 모두 만족해야 완료다.

1. 컨트롤러의 모든 공개 경로가 `Quick Contract`에 반영됨
2. 각 엔드포인트의 헤더/바디 필드명이 DTO와 일치함
3. 실패 상태코드가 현재 예외 처리 방식과 모순 없음
4. 삭제된 API가 문서에 남아있지 않음
5. 부분 갱신 시에도 문서 전체 문맥이 깨지지 않음

## 7) 최종 보고 포맷

갱신 후 아래 형식으로 보고한다.

1. 변경 요약 (추가/수정/삭제된 엔드포인트)
2. 코드-문서 불일치 해소 내역
3. 남은 리스크/주의사항 (없으면 "없음")
