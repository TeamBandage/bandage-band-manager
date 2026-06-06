# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew bootRun          # Run the application
./gradlew build            # Build (also runs tests)
./gradlew test             # Run tests
./gradlew spotlessApply    # Format code (Ktlint — must pass before commit)
```

A pre-commit hook at `scripts/pre-commit` runs `spotlessApply` automatically on commit.

## Architecture

This is a **modular monolith** (Spring Modulith) built for future MSA migration. Kotlin 2.x + Spring Boot 4.x + JDK 21.

### Package Layout

```
com.bandage.v1/
├── domain/          # Core business domains (auth, member, band, practice, performance)
├── facade/          # Orchestration for multi-domain transactions (e.g. MemberJoinFacade)
├── structure/       # Supporting structures (band application, management)
└── global/          # Cross-cutting concerns
    ├── config/      # Spring configs (security, swagger, querydsl, redis, web)
    ├── security/    # JWT authentication, handlers
    ├── error/       # Global exception handling
    ├── jpa/         # Audit infrastructure (MemberAuditorAware)
    ├── async/event/ # Domain event classes and handlers
    └── common/      # BaseEntity, shared models
```

### Domain Module Structure

Every domain follows this exact layout:
```
domain/{name}/
├── model/       # JPA entities
├── controller/  # REST endpoints
├── service/     # Business logic
├── repository/  # Spring Data JPA interfaces
└── dto/
    ├── req/     # Request DTOs
    └── res/     # Response DTOs
```

### Key Patterns

**Entity construction** — always use factory methods, never direct constructors:
```kotlin
companion object {
    fun create(...): MyEntity = MyEntity(...)
}
```

**Protected setters** — all mutable entity properties use `protected set` to enforce encapsulation.

**Soft delete** — entities annotated with `@SQLRestriction("deleted_at IS NULL")` expose `markAsDeleted(deleter: Long)` and `restore()` methods. Never hard-delete these.

**Primary keys** — Band-level and Practice entities use UUIDv7 (`@UuidGenerator(style = VERSION_7)`). Other entities use auto-increment Long.

**Service transactions** — services are `@Transactional(readOnly = true)` by default; write methods override with `@Transactional`.

**Facade layer** — use for operations that cross domain boundaries (e.g. creating a member and their auth record together). Facades maintain module isolation while enabling transactional orchestration.

**Event-driven hooks** — `global/async/event/` contains `CommonEvent` base class and domain events (`MemberJoinEvent`, `MemberLoginEvent`, etc.) consumed by `AuthEventHandler` and `RefreshTokenEventHandler`.

### Active Profiles

Default active profile is `local` (set via `PROFILE_ACTIVE` env var). Profile groups:
- `local` → includes: `default-datasource`, `swagger`, `redis`, `security`
- `dev` → includes: `dev-datasource`, `swagger`, `redis`, `security`

### Tech Stack

- **DB**: PostgreSQL + Spring Data JPA + QueryDSL 5.1.0
- **Cache/Session**: Redis
- **Auth**: JWT (JJWT 0.12.6) — access token 1h, refresh token 7h
- **Docs**: SpringDoc OpenAPI (Swagger UI available in local/dev)
- **Formatting**: Spotless + Ktlint

## OpenAPI 스펙 관리

API 스펙은 SpringDoc이 런타임에 생성하며, **diff 가능한 형태로 `docs/openapi.json`에 커밋**되어 관리한다. 이 파일은 BE/FE 영향평가(스펙 diff) Tool의 비교 기준점이므로, **API를 변경하는 PR은 반드시 `docs/openapi.json` 갱신을 함께 포함**해야 한다(코드-스펙 동일 PR 규약).

### 스펙 갱신 방법 (로컬)

```bash
# 1. 로컬 인프라 실행 (이미 실행 중이면 생략)
docker-compose up -d postgres redis

# 2. 애플리케이션 실행 (기본 local 프로파일, swagger 포함)
./gradlew bootRun

# 3. 스펙 다운로드 + 정렬 포맷(diff 안정성)으로 저장
curl -s http://localhost:8080/api-docs | python3 -m json.tool --sort-keys > docs/openapi.json

# 4. 변경사항 확인 후 코드와 동일 PR에 포함
git diff docs/openapi.json
```

위 3번 단계는 Gradle 태스크로도 실행할 수 있다(앱이 8080에서 실행 중이어야 함):

```bash
./gradlew dumpOpenApiSpec
```

### Breaking Change 확인

`docs/openapi.json`은 OAS **3.1.0** 출력이다. diff 도구는 3.1.0을 지원하는 것을 사용해야 한다(상세: `docs/openapi-diff-tooling.md`).

## Pull Request Convention

When writing a PR description, always follow `.github/PULL_REQUEST_TEMPLATE.md` and write all content in valid Markdown syntax.

Commit Convention
Always use this format for every commit message:

Plaintext
[{issue-key}] {type}: {summary}
- {detail 1}
- {detail 2}
- {detail n}

{smart-commit-commands}
Types: chore, feat, ai, test, refactor, fix

[{issue-key}] — REQUIRED when working on a branch tied to a Jira issue (e.g., branch feat/BAND-12-practice-crud → [BAND-12]). Always place at the very beginning of the first line. Omit only if there is genuinely no related issue.

{summary} — concise description of the change

bullet list — one line per meaningful change (omit if only one trivial change)

{smart-commit-commands} — OPTIONAL. Jira Smart Commit commands to transition states, log time, or add comments (e.g., #done, #time 1h 30m, #comment API 구현 완료). Always place on its own line at the very end, after a blank line.

### Examples
```plaintext
[BAND-12] feat: 합주 생성 API 구현
- PracticeCreateRequest, PracticeResponse DTO 추가
- PracticeService.createPractice 구현
- POST /practices 엔드포인트 추가

#done #time 2h
```
