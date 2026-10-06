# Pre-Exercise Analysis

## How the project context was presented to the AI

**Tools**
- Claude Code v2.1.289 (Opus 5.5) in the IntelliJ IDEA terminal, connected through the IntelliJ MCP server.
- Claude (chat) for a first review of the repository.

**Context**
- `/init` was run in plan mode. Claude Code read the README, `pom.xml` and the source code, and generated `CLAUDE.md` (commands, layered architecture, request flow, conventions, README constraints).
- Claude Code was then asked to read `README.md` and summarize the tasks and constraints.
- Most changes were approved manually; auto mode was enabled during part of the later work (the JWT review and the final documentation changes), and those changes were reviewed in the diff before each commit.

## Information provided before starting

- **Specification:** `README.md`.
- **Constraints for the work:**
  - do not change the layered architecture;
  - Java 17 and Spring Boot 3.5;
  - only up-to-date third-party libraries with no known vulnerabilities.
- **Language preference:** conversation in Italian; code, comments, Javadoc, commits and documentation in English.

## Limitations in the tool's initial understanding

**What it understood well:** the layered architecture (web / service / persistence), the request flow, and the code conventions.

**Limitations found:**
- **Ambiguous CLAUDE.md wording.** The generated `CLAUDE.md` described centralized exception handling in a way that could be read as an existing feature, but it is a README requirement (task 4). After review, the line was rewritten to say it does not exist yet.
- **Incomplete bug list at `/init`.** The `/init` summary reported only 4 bugs:
  - `lastName` is mapped from `firstName` in `AddUserAssembler`;
  - validation errors are swallowed into a generic 500 in `UserServiceImpl`;
  - the email is truncated to its domain in `UserAssembler`;
  - the wrong `BY_LASTNAME_DESC` label.

  Other issues came up only in a later message:
  - `phoneNumber` is not mapped in `UserAssembler`;
  - the static, mutable `GENERIC_ERROR`;
  - `total` has no getter/setter in `GenericPagedResult`;
  - `UserRepository.getAll()` exposes the internal list.
- **Missed bug.** The invalid seed phone numbers in `FakeDatabase` (`"+39" + i`) were never flagged by the tool. They had already come up in the initial review of the repository done with Claude (chat), before Claude Code was used.
