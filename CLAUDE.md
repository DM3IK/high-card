# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1 / Java 17 / Maven demo app (`it.sara.demo`, main class `HighCardApplication`) used as a hiring exercise. README.md lists the required tasks: input validation (email + Italian phone numbers), SQL-injection hardening of the PUT endpoint, pagination/sorting/search for user listing, centralized exception handling, JWT auth (policy, issuer, expiration), bug fixing, unit tests, Javadoc, and an `/ai-assisted/` folder documenting AI usage (`pre-analysis.md`, `plan.md`, `report.md`).

Hard constraints from the README:
- Do **not** change the existing layered architecture.
- Every response, errors included, returns HTTP **200**. The real outcome goes in `StatusDTO.code` inside the response body.
- Third-party libraries must be current and have no known vulnerabilities.
  - `pom.xml` overrides three versions managed by Spring Boot 4.1.1, to fix known CVEs in transitive dependencies:
    - `logback.version` 1.6.5 (Boot ships 1.5.38): CVE-2026-19880, CVE-2026-104721;
    - `jackson-bom.version` 3.1.7 (Boot ships 3.1.5): CVE-2026-68497, CVE-2026-83557, CVE-2026-91776, CVE-2026-91777;
    - `tomcat.version` 11.0.26 (Boot ships 11.0.24): NVD lists 11 CVEs for 11.0.24 and none for 11.0.26, including CVE-2026-65182 and CVE-2026-66299 reported by IntelliJ.
  - Remove each override once Spring Boot manages that version or a later one.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` in bash):

- Build: `./mvnw clean package`
- Run: `JWT_SECRET=<at least 32 bytes> ./mvnw spring-boot:run` (PowerShell: `$env:JWT_SECRET='<at least 32 bytes>'; .\mvnw.cmd spring-boot:run`). The app refuses to start without a valid secret.
- Get a token for manual calls: run `JwtTokenGenerator` (in `src/test/.../web/security`) with the same `JWT_SECRET`, then send `Authorization: Bearer <token>`.
- `HOW_TO_RUN.md` is the step-by-step guide for the reviewer (Bash and PowerShell); keep it in sync with these commands.
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
- `service/user/validator/UserValidator` holds the whitelist rules for names, email and Italian phone numbers. This is also the defense against injection on the PUT endpoint (README task 2), since there is no SQL layer to parameterize. Every value is trimmed before validation, and values are stored normalized: phone numbers as `+39` followed by digits only. Phone numbers are checked against the Italian numbering plan with Google libphonenumber (`com.googlecode.libphonenumber`), accepting only landline and mobile types.

**Persistence (`service/database/`)**
- `UserRepository` is a `@Component` that wraps `FakeDatabase.TABLE_USER`, a static in-memory `List<User>` seeded with 10 users. There is no real database or JPA.

**Request flow**
Request → `UserController` → web assembler → `Criteria*` → `UserService` → `UserRepository` → `Result` → (`UserAssembler` → DTO) → `Response`.

**Errors**
- Services throw `GenericException`, a checked exception that carries a `StatusDTO` (`code`, `message`, `traceId`).
- `GenericResponse.successStatus(msg)` builds the success status of any response type; `GenericResponse.error(status)` builds an error response.
- `web/handler/GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place that turns exceptions into a `GenericResponse`, always with HTTP status 200. It handles `GenericException`, malformed body (400), unsupported method (405), unsupported media type (415), an `Accept` header that excludes JSON (406; both endpoints declare `produces` JSON, so the request is rejected before the controller runs), unknown path (404), and any other exception as a generic 500 that hides internal details.
- Request errors are logged **only** in the handler: 5xx at ERROR with trace id and stack trace, 4xx at WARN with trace id and message. Services create exceptions (keeping the cause) but do not log them.
- Spring Security errors (401/403) are raised in filters before the controllers, so the handler does not see them. `web/security/SecurityErrorHandler` writes them in the same format (HTTP 200 + `StatusDTO`) and logs the reason at WARN.

**Security (`web/security/`)**
- `SecurityConfig`: a stateless resource server that requires a Bearer JWT on every request. Tokens are validated on:
  - signature: HS256 with `security.jwt.secret`;
  - issuer: `security.jwt.issuer`;
  - expiration: `exp` is required, with `security.jwt.clock-skew` tolerance;
  - policy: `PUT /user/v1/user` needs scope `users:write`, `POST /user/v1/user` needs `users:read`.
- `JwtProperties` validates the settings at startup (non-blank issuer, secret of at least 256 bits) and masks the secret in `toString()`.
- The real secret comes from the `JWT_SECRET` environment variable and is never committed. The only committed secret is a test-only value in `src/test/resources/application.properties`, used with the `TestTokens` helper.
- `@WebMvcTest` tests that are not about security use `@AutoConfigureMockMvc(addFilters = false)`. `@SpringBootTest` tests send a token from `TestTokens`.

**Endpoints**
Both endpoints are mapped under `/user/v1/user`:
- `PUT` adds a user.
- `POST` searches users. Every body field is optional:
  - `query`: case-insensitive `contains` on first name, last name and email;
  - `offset`: default 0;
  - `limit`: default 10, max 100;
  - `order`: an `OrderType` name, default `BY_LASTNAME`.

  `GetUsersAssembler` maps the request to `CriteriaGetUsers` and leaves missing values null; `UserServiceImpl` applies the defaults and limits. The response contains the page of users and `total`, the number of matches before paging. Sorting uses an Italian `Collator` and is stable across pages (ties broken by the other name, then by guid).

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
