# Shopping Project - Codex Agent Instructions

## 1. Project Goal

이 프로젝트는 개인 학습용 쇼핑몰 프로젝트이다.

목표는 단순히 기능을 빠르게 구현하는 것이 아니라,
Spring Boot와 React/TypeScript 기반 웹 개발 과정을 학습하는 것이다.

따라서 기존 구조를 무시하고 대규모 코드를 생성하거나
불필요한 리팩터링을 수행하지 않는다.


## 2. Project Structure

shopping/
├── backend
├── frontend
├── docs
├── AGENTS.md
└── README.md


## 3. Development Principles

- 기존 프로젝트 구조와 코드 스타일을 우선한다.
- 기존 구현을 먼저 분석한 후 수정한다.
- 필요하지 않은 파일은 수정하지 않는다.
- 요청하지 않은 기능을 추가하지 않는다.
- 대규모 리팩터링을 임의로 수행하지 않는다.
- 새로운 라이브러리 추가는 반드시 필요한 경우에만 한다.
- 기존 API 계약을 임의로 변경하지 않는다.


## 4. Learning-oriented Development

사용자는 학습 목적으로 프로젝트를 진행하고 있다.

따라서 작업마다 다음 내용을 명확히 구분한다.

1. 무엇을 구현했는가
2. 왜 이 방법을 사용했는가
3. 어떤 파일을 수정했는가
4. 테스트 방법
5. 다음 단계

복잡한 구현을 숨기지 말고 핵심 설계 의도를 설명한다.


## 5. Workflow

작업 시작 전:

1. 관련 파일을 확인한다.
2. 현재 구현 상태를 분석한다.
3. 요청 범위를 확인한다.

작업 후:

1. 변경 파일을 확인한다.
2. 빌드한다.
3. 관련 테스트를 실행한다.
4. 실패하면 원인을 분석하고 수정한다.
5. git diff를 확인한다.


## 6. Git Rules

한 단계의 작업은 하나의 논리적 commit으로 만든다.

commit 형식:

feat: 기능
fix: 버그 수정
refactor: 리팩터링
test: 테스트
docs: 문서
chore: 설정

예:

feat: implement order creation
test: add order service tests
fix: validate refresh token type

작업과 무관한 파일을 commit하지 않는다.


## 7. Safety Rules

다음 작업은 명시적인 요청 없이 하지 않는다.

- force push
- main/master 직접 push
- git reset --hard
- 대량 파일 삭제
- DB schema destructive change
- secret/API key commit


## 8. Completion Report

작업 완료 후 다음 형식으로 보고한다.

### 구현 내용

### 변경 파일

### 테스트 결과

### Git 상태

### 학습 포인트

### 다음 추천 단계