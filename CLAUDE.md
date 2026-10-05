# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 3.5 / Java 17 / Maven demo app (`it.sara.demo`, main class `HighCardApplication`) used as a hiring exercise. README.md lists the required tasks: input validation (email + Italian phone numbers), SQL-injection hardening of the PUT endpoint, pagination/sorting/search for user listing, centralized exception handling, JWT auth (policy, issuer, expiration), bug fixing, unit tests, Javadoc, and an `/ai-assisted/` folder documenting AI usage (`pre-analysis.md`, `plan.md`, `report.md`).

Hard constraints from the README:
- Do **not** change the existing layered architecture.
- Every response, errors included, returns HTTP **200**. The real outcome goes in `StatusDTO.code` inside the response body.
- Third-party libraries must be current and have no known vulnerabilities.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` in bash):

- Build: `./mvnw clean package`
- Run: `./mvnw spring-boot:run`
- All tests: `./mvnw test`
- Single test class: `./mvnw test -Dtest=HighCardApplicationTests`
- Single test method: `./mvnw test -Dtest=ClassName#methodName`

Lombok is set up as an annotation processor in `pom.xml`.

## Architecture

There are two layers, and each one has its own object types. Keep them separate.

**Web layer (`web/`)**
- Controllers (`web/user/UserController`) accept `*Request` objects (which extend `GenericRequest`) and return `ResponseEntity<*Response>`. Response types extend `GenericResponse`, or `GenericPagedResponse` for paged results.
- Web assemblers (`web/assembler/*Assembler`) turn a Request into a service-layer `Criteria*` object.

**Service layer (`service/`)**
- Services (`service/user/UserService` interface plus `impl/UserServiceImpl`) accept `Criteria*` objects (which extend `GenericCriteria`) and return `*Result` objects (which extend `GenericResult` or `GenericPagedResult`).
- Services must never see web Request or Response types.
- `service/assembler/UserAssembler` maps the `User` entity to `UserDTO` (in `dto/`).

**Persistence (`service/database/`)**
- `UserRepository` is a `@Component` that wraps `FakeDatabase.TABLE_USER`, a static in-memory `List<User>` seeded with 10 users. There is no real database or JPA.

**Request flow**
Request → `UserController` → web assembler → `Criteria*` → `UserService` → `UserRepository` → `Result` → (`UserAssembler` → DTO) → `Response`.

**Errors**
- Services throw `GenericException`, a checked exception that carries a `StatusDTO` (`code`, `message`, `traceId`).
- `GenericResponse.success(msg)` builds the success status.
- Centralized exception handling does **not** exist yet (README task 4). When added, it must map exceptions to a `GenericResponse` with the right `StatusDTO`, always with HTTP status 200.

**Endpoints**
Both endpoints are mapped under `/user/v1/user`:
- `PUT` adds a user.
- `POST` lists or searches users. `CriteriaGetUsers` has `query`, `offset`, `limit` and an `OrderType` enum.

## Conventions
- Dependencies are injected through the constructor: `private final` fields with Lombok `@RequiredArgsConstructor`.
- Lombok `@Getter`/`@Setter` is used on DTOs, requests, responses and criteria. `@Slf4j` is used for logging.
- Methods name their return variable `returnValue`.
- No inline comments in production code: code must be self-explanatory; Javadoc only.
- Javadoc on classes, public methods and relevant fields.
- Every feature or bug fix comes with unit tests that cover at least:
  - (a) the positive case;
  - (b) edge cases: null, empty or whitespace-only strings, boundary values;
  - (c) the error or regression case.

  The number of tests follows the logic under test: do not add tests that verify nothing new.
- Every bug fix is verified by temporarily reintroducing the bug and checking that the new tests fail, then restoring the fix.
