# Change Plan

## Intervention plan

After reading `README.md`, Claude Code proposed an order in which each step prepares the next one. I accepted it and decided to give instructions one step at a time.

| Step | README task | Reason for the position |
|---|---|---|
| 0 | — | Verify that the project builds and the existing test passes (`./mvnw test`), to have a clean baseline |
| 1 | 6. Bug fixing | Some bugs interfere with later tasks (e.g. validation errors were swallowed into a generic 500) |
| 2 | Refactoring | Switch from field injection to constructor injection (added by me, see "Plan iterations") |
| 3 | 4. Exception handling | Needed before validation, because it is how errors reach the client (HTTP 200 + `StatusDTO`) |
| 4 | 1. Validation | Email and Italian phone number |
| 5 | 2. SQL injection | Same fields and logic as validation; the project has no SQL layer, so the fix is input validation |
| 6 | 3. Search, pagination, sorting | Reuses validation and exception handling |
| 7 | 5. JWT | Last, because it protects every endpoint and makes manual testing harder |
| — | 7. Unit tests, 8. Javadoc | Inside every step, not at the end |

Before touching the code, Claude Code produced a list of 11 bugs with file, description, proposed fix and severity. It also listed:
- issues deliberately deferred to the step they belong to (invalid seed phone numbers → validation; empty `getUsers` → search; unused `AddUserResponse` → exception handling);
- things it suggested not to change (PUT for create and POST for search, because the README refers to "the `PUT` endpoint").

## Plan iterations

- **Bug fixing split into three blocks.** I split the 11 bugs into Block A (assembler mapping), Block B (error propagation, `GenericException`, blank strings) and Block C (repository, concurrency, paging and sort metadata). After each block Claude Code stops, runs `./mvnw test` and shows the result; I review the diff and commit.
- **Field injection vs. constructor injection.** Claude Code suggested keeping field `@Autowired` because it is the project convention and the README asks not to change the architecture. I disagreed: constructor injection does not change the layered architecture and improves testability and immutability. It became a separate refactoring step after bug fixing, using Lombok `@RequiredArgsConstructor` for consistency with the rest of the project.
- **Ambiguous bug.** Claude Code pointed out that truncating the email to its domain in `UserAssembler` could be intentional masking, explained why it considered it a bug, and asked me to decide. I approved treating it as a bug.
- **Testing convention.** Before Block B, I added a rule to `CLAUDE.md`: every change needs tests for (a) the positive case, (b) edge cases such as null, empty or blank values and boundaries, (c) the error or regression case, with no tests that verify nothing new. I had it applied retroactively to the Block A tests.
- **Design changes during review.**
  - I asked for `genericError(Throwable)` so that the original cause stays in the stack trace.
  - I asked to log unexpected errors with the same trace id returned to the client (see `report.md`).
  - I questioned Javadoc that only restated names. As a result, the generic error constants became private and redundant class-level Javadoc was removed from test classes.
  - I asked Claude Code to always review the diff for clean code before each commit. In the first such review it found that the `genericError()` overload without a cause was no longer used, and I had it removed.
- **Spring Boot upgrade before the JWT step.**
  - Claude Code proposed upgrading Spring Boot before adding Spring Security, at first to the latest 3.5.x patch.
  - While checking the available versions, it found that the 3.5 line had reached the end of its open-source support on 2026-06-30 and no longer receives free security fixes. The README requires up-to-date libraries with no known vulnerabilities, so it recommended 4.1.1 instead, a major upgrade.
  - I chose 4.1.1 and asked for it as a separate commit, so that the JWT step starts from supported libraries.
- **Code conventions added to `CLAUDE.md` along the way:**
  - no inline comments in production code;
  - Javadoc on classes, public methods and relevant fields;
  - every bug fix verified by temporarily reintroducing the bug.

## Delegated vs. manual tasks

**Delegated to Claude Code** (every change approved manually, no auto mode):
- generating `CLAUDE.md` with `/init`;
- analyzing the code and listing the bugs;
- writing the fixes, the unit tests and the Javadoc;
- running `./mvnw test` after every block;
- drafting commit messages and the factual entries in the `ai-assisted/` documents.

**Done or decided by me:**
- the order of work, the split into blocks, and every design decision listed above;
- reviewing every diff before committing;
- making every commit by hand from IntelliJ (Claude Code only lists the files and suggests the message);
- verifying the Block A test by temporarily reintroducing the `lastName` bug;
- correcting wording in the generated Javadoc;
- removing by hand an inline comment;
- limiting Claude Code's permanent permissions to reading project files and read-only git commands. Most changes were approved manually; auto mode was enabled during part of the later work (the JWT review and the final documentation changes), and those changes were reviewed in the diff before each commit;
- using Claude (chat) as a second reviewer of Claude Code's output; this is how the cause-preserving `genericError(Throwable)` and the trace id logging gap came up.
