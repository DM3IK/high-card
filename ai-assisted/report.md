# Final Report

## Key prompts used

The prompts were written in Italian; the quotes below are translated. They are grouped by what they were meant to control.

**Context and ground rules**
- *"Create the ai-assisted/ folder… do NOT invent content: leave a 'To be completed' placeholder under each heading. The text must describe only things that really happened."* This set the rule for all the AI-assisted documents.
- *"I do all commits from the IntelliJ interface (Ctrl+K)… Don't give me instructions with git add or git commit: just tell me which files to tick and the commit message."* Claude Code never committed; I reviewed every diff and committed by hand.

**Splitting the work and keeping control**
- *"I approve the list. Let's proceed in blocks, stopping after each one so I can review and commit… After each block run ./mvnw test and show me the result."* The 11 bugs were fixed in three reviewable blocks.
- *"I disagree on the injection: switching from field injection to constructor injection doesn't change the layered architecture, it improves testability and immutability. We'll do it as a separate refactoring after the bugs."* This overrode Claude Code's suggestion to keep field `@Autowired`.
- *"Wait, why did you change the version in the pom?"* Claude Code had started the upgrade right after I chose the version in a multiple-choice question. I stopped it; Claude Code explained and offered to revert, and I then confirmed the Spring Boot 4.1.1 upgrade as a separate commit.

**Quality rules**
- *"Every feature or bug fix must have tests that cover at least (a) the positive case, (b) edge cases such as null, empty or whitespace-only strings and boundary values, (c) the error or regression case. The number of tests depends on the logic: don't add tests that verify nothing new."* This was added to `CLAUDE.md` and applied retroactively.
- *"No useless comments; always follow the README guidance."* Later: *"Always check the files in the commit and verify that the code is clean; remove useless comments."* This led to the no-inline-comments convention and a review of the diff before every commit.
- Before each commit I asked for a dedicated review pass of the whole diff, not only of the latest change. These passes found real issues: the JWT secret printed by the record's `toString()`, a misleading validator, an unused test dependency and a redundant test.

**Design decisions given as precise instructions**
- *"Also add an overload genericError(Throwable cause)… so that when an unexpected error becomes a generic 500, the original cause stays in the stack trace. The client must still receive only 'Generic error'."*
- *"In the generic catch, create the exception first and then log it with the trace id, so the error received by the client can be found in the log."*
- *"Option 1: log only in the handler. 5xx: log.error with trace id and stack trace; 4xx: log.warn with trace id and message, no stack trace. For HttpMessageNotReadableException return a generic message, without Jackson details."*
- *"The search POST must also work without a body… Choose whether in the controller or in the assembler, but justify the choice."*

**Fact-checking the AI**
- *"Check that every number corresponds to executions that really happened in this session. If something has no match in the output, remove or rephrase it. Tell me which commands you ran and with what result."*
- *"Check every point against the conversation history."* I made this a standing rule for the AI-assisted documents, because some of my drafts were written from memory: when a statement had no match in the history, Claude Code flagged it and we corrected it before committing.

## Difficulties encountered

- **Correct, tested code with a design flaw that the tests could not catch.** In the catch block for unexpected errors in `UserServiceImpl.addUser`, Claude Code logged the error first and created the generic `GenericException` afterwards. The trace id returned to the client was therefore never written to the log. If a user reported "I got an error with traceId abc-123", nobody could find it on the server. The code compiled and all tests passed, because no test checked the log. The flaw came up while reviewing the diff, where I used Claude (chat) as a second reviewer of Claude Code's output. I evaluated the point, agreed it was a real gap and asked Claude Code to reverse the order: create the exception first, then log it with its trace id. I also asked to update the tests where needed; Claude Code extended the existing test to check the log content. The test now captures the log output and checks that it contains the same trace id the client receives. Lesson: green tests only cover what they assert; reviewing the design is still a human job.
- **A concurrency test that caught the bug only sometimes.** To fix the non-thread-safe `ArrayList` in `FakeDatabase`, Claude Code switched to a `CopyOnWriteArrayList` and wrote a test with 8 threads × 250 concurrent `save` calls. It passed. Claude Code then applied, without being asked, the "reintroduce the bug" check I had introduced in the first block, showing that the practice had carried over to its later work. It temporarily restored the `ArrayList` and ran the test 3 times. The test failed only once (2 users lost), so the first version would have let the bug through most of the time. Claude Code made the test more aggressive: 1000 users per thread, plus a `CountDownLatch` that starts all threads at the same moment. With the `ArrayList` the test then failed 5 times out of 5. With the fix restored, the full suite passed 3 runs in a row. Detection is still probabilistic, as with any concurrency test, but the test is now reliable in practice. Lesson: a test written by the AI that passes is not evidence that it detects the bug; it has to be seen failing.
- **A security test that passed for the wrong reason.**
  - *The bug.* For the JWT step, Claude Code wrote a test helper (`TestTokens`) that always set the issue time (`iat`) to "now". For an already expired token, the expiration was therefore *before* the issue time, and Spring Security rejected the token as inconsistent ("expiresAt must be after issuedAt") instead of as expired.
  - *How it surfaced.* The test for a token expired 10 seconds ago, which should be accepted thanks to the 30-second clock skew, failed. Claude Code read the rejection reason in the log and found the cause. This also meant that the test for a token expired beyond the clock skew had been passing for the wrong reason: it checked only the 401 code, not why the token was rejected.
  - *The fix.* Claude Code fixed the helper (tokens are now issued one hour before they expire) and strengthened the tests: for every invalid token (expired, missing expiration, wrong issuer, wrong signature, unsigned, malformed) they now also check the rejection reason written in the log. It then checked that each logged reason was the expected one.
  - *Lesson.* For security tests, asserting the error code is not enough; the test must also prove which check rejected the request.
- **An inconsistency I found myself after the push.**
  - *The issue.* While reading the code and reasoning about the tests after the push, I noticed that `UserValidator` trimmed names and email before validating them, but not the phone number. So " Mario " was accepted, while " 3331234567" and "3331234567 " were rejected with code 400 "Invalid phone number". All the AI-written tests passed, because none of them covered spaces around the phone number.
  - *The fix.* I reported it to Claude Code with these two examples. It first wrote a regression test with four cases (leading, trailing and surrounding spaces, plus tab and newline), which failed against the existing code. Then it added the trim to `normalizePhoneNumber`, and the full suite passed: 217 tests.
  - *Lesson.* The rules applied to similar inputs must be checked side by side. A green suite only shows that the cases someone thought of work.
- **Three more issues I found and fixed myself, reviewed by Claude Code.** I kept reviewing the code after the push and fixed three issues that the AI-written tests did not cover, each with its own regression test:
  - *Foreign numbers dialed with 00.* A number such as "0044 2071 234" passed as an Italian landline and was stored as "+3900442071234". Italian area codes never start with `00`, which is the prefix for calling abroad, so a landline can no longer have `0` as its second digit.
  - *An `Accept` header without JSON.* With `Accept: application/xml`, the PUT stored the user and only then failed to write the response; the client got HTTP 406 with an empty body, which breaks the README rule "always HTTP 200". Both endpoints now declare `produces` JSON, so the request is rejected before the controller runs, and the exception handler always writes JSON with HTTP 200 and code 406.
  - *Accented letters in the search query.* Names are stored in composed Unicode form (NFC), but the search query was not normalized, so "Niccolò" written with a separate combining accent was not found. The query is now normalized too.
  - *Review by Claude Code.* I asked Claude Code to check the logic and the tests. It reintroduced each bug and confirmed that every new test fails without its fix (5 of 5 phone cases, the search case, and both halves of the 406 fix: without `produces` the user is stored, without the forced JSON content type the client still gets HTTP 406). By reproducing the original code it also confirmed that the server logged a 500 in that case. It found one gap: the 406 test covered only the PUT, so removing `produces` from the search POST would have gone unnoticed (it would answer with a generic 500). We added a POST case, which fails without `produces`.
- **Non-existent phone prefixes: a bug the AI missed, found with my own tests and an online search.**
  - *The issue.* The phone regex written by Claude Code checked only the first digit and the length (`3` plus 8-9 digits, `0` plus 5-10 digits). Claude Code missed this in every review, including the bug hunt in which it fixed the `00` case of the same regex. I wrote new tests with prefixes that do not exist in the Italian numbering plan: mobile prefixes `300` and `305`, a 9-digit number in the `31x` range, and the area codes `013`, `0162` and `0177`. All 7 failed, because these numbers were accepted.
  - *The solution.* Searching online, I found a Stack Overflow answer recommending Google libphonenumber, which ships the official numbering plan of every country and is updated with each release. I asked Claude Code to integrate it. It checked that the latest version (9.0.41) has no known vulnerabilities on OSV and no transitive dependencies, then replaced the regex check with libphonenumber, keeping the format whitelist and the Italian-only prefix check. The number is parsed as `+39` plus the national number, so a second prefix such as "+39 0039 333..." cannot be read as an Italian number.
  - *A side effect caught by an existing test.* One existing test failed: libphonenumber accepts "33312345678", a voicemail access number. Toll-free (`800`) and premium-rate (`899`) numbers are valid too. Only landline and mobile types are now accepted, with two more test cases. Removing the type filter makes 3 tests fail. Full suite: 235 tests.
  - *Lesson.* A rule such as "first digit and length" looks right and passes every test written from the same assumption. Data that changes over time, like a numbering plan, is better taken from a maintained library than rewritten by hand.

## Approaches adopted

- **Fact-checking the AI's own documentation.** I dictated a list of "limitations" for `pre-analysis.md` that were partly inaccurate. Instead of writing them as given, Claude Code checked them against `git status` and the conversation history. It flagged the mismatches: `CLAUDE.md` had not been edited by hand, and four of the "missed" bugs had already been reported in a later message. It then asked how to proceed. The result was a more accurate account.
- **Verifying that the AI-written tests actually catch the bug.** A passing test proves nothing on its own. After Claude Code fixed the `lastName` mapping bug in `AddUserAssembler` and wrote `AddUserAssemblerTest`, I temporarily put the bug back (`getFirstName()` instead of `getLastName()`) and ran the tests: they failed. I then restored the fix. This confirmed that the test guards against the regression and does not pass trivially. After the bug-fixing blocks, I made this check a project rule in `CLAUDE.md`, so that every bug fix is verified this way.
- **Keeping the dependency upgrade under control.**
  - *Upgrade.* Claude Code proposed upgrading Spring Boot before the JWT step, because the 3.5 line reached the end of its open-source support on 2026-06-30. After I chose 4.1.1, Claude Code changed the version in `pom.xml` to see what would break. I stopped it and asked why it had changed the version. It explained, described the current state (only `pom.xml` changed, two test imports broken) and offered to revert. I confirmed 4.1.1 as a separate commit.
  - *Vulnerabilities found after the upgrade.* IntelliJ (Mend.io) reported transitive vulnerabilities on `spring-boot-starter-webmvc`: first one in Logback, then, after that was fixed, five more (four in Jackson shown in detail, one not shown). I shared IntelliJ's warnings. Claude Code looked up each CVE on NVD, matched it with the actual dependency tree and forced fixed versions through Spring Boot's version properties: Logback 1.6.5, Jackson 3.1.7, Tomcat 11.0.26.
  - *A second Logback CVE.* While checking the Logback fix, Claude Code found a newer CVE (CVE-2026-104721) showing that the obvious choice, 1.6.3, was still affected.
  - *The CVE that was not shown.* The two Tomcat CVEs were found by searching NVD for the Tomcat version in use, because IntelliJ did not show the fifth CVE in detail.
  - *Confirmation.* I reloaded the Maven project in IntelliJ and confirmed that the warnings were gone.
- **Testing the running application, not only the tests.**
  - *While writing the guide.* Claude Code ran every command in `HOW_TO_RUN.md` as written, in both PowerShell and Bash: start with `JWT_SECRET`, token from `JwtTokenGenerator`, search, and user creation with phone normalization. The first version of the guide suggested `curl.exe` for PowerShell, but Windows PowerShell 5.1 broke JSON bodies containing spaces: creating a user with "+39 333 765 4321" returned "Malformed request body". The PowerShell examples now use `Invoke-RestMethod`, and the corrected commands were run again successfully.
  - *My manual test.* I then followed the guide myself, in a separate PowerShell terminal: I started the application from IntelliJ with `JWT_SECRET`, generated a token with `JwtTokenGenerator` and called the API with `Invoke-RestMethod`. Results:
    - search for "ro": code 200, total 3;
    - creation of Giorgio Neri with "+39 333 765 4321": code 200, and a search for "neri" returned him with "+393337654321";
    - request without a token: code 401;
    - phone number with the +44 prefix: code 400, "Invalid phone number";
    - first name "Robert'); DROP TABLE users;--": code 400, "Invalid first name".

    The results matched the behaviour covered by the automated tests.

## Personal assessment

**Time saved.** Without AI it would have taken me at least twice as long. For a task like this I would normally have done much more research on Stack Overflow, Reddit and the official documentation. I have used several AI services in the past and tested them in different ways; today the one I use most is Claude Code. I had already implemented authentication in other projects, and the difference is clear: without AI it takes much longer.

**Where it helped most.** Reviewing code on the fly, writing documentation and notes, and repetitive work such as writing JUnit tests. It was especially useful for the Javadoc, the most tedious part of the job: it drafted it quickly and I only had to review and correct it. It also helped with checks I would have done less consistently on my own: checking dependency versions and known vulnerabilities, and making sure tests really fail when the bug is put back.

**Where it fell short.** AI always needs to be reviewed, because it often makes mistakes: it does not know certain conditions of the project and does not always understand the context. That is why it is not always effective and needs supervision. Some concrete examples from this work:
- it wrote correct, tested code with a design flaw (the trace id returned to the client was never written to the log), which only came up during review;
- some tests passed for the wrong reason: for example, the expired-token test was rejecting the token because of inconsistent dates, not because of the expiration;
- the vulnerabilities in transitive dependencies were reported by IntelliJ, not by the AI.

**What I would do differently.** I would set up from the start the rules I introduced along the way (testing conventions, no useless comments, verifying fixes by putting the bug back), and I would keep manual approval for the whole job: I used auto mode in some phases, and that is exactly where I had less control over individual changes.

**In short:** AI speeds up the work a lot, but the result depends on how well you guide and check it. The decisions, the review and the responsibility for the code remain mine.
