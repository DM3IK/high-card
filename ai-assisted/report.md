# Final Report

## Key prompts used

_To be completed_

## Difficulties encountered

- **Correct, tested code with a design flaw that the tests could not catch.** In the catch block for unexpected errors in `UserServiceImpl.addUser`, Claude Code logged the error first and created the generic `GenericException` afterwards. The trace id returned to the client was therefore never written to the log. If a user reported "I got an error with traceId abc-123", nobody could find it on the server. The code compiled and all tests passed, because no test checked the log. The flaw came up while reviewing the diff, where I used Claude (chat) as a second reviewer of Claude Code's output. I evaluated the point, agreed it was a real gap and asked Claude Code to reverse the order: create the exception first, then log it with its trace id. I also asked to update the tests where needed; Claude Code extended the existing test to check the log content. The test now captures the log output and checks that it contains the same trace id the client receives. Lesson: green tests only cover what they assert; reviewing the design is still a human job.
- **A concurrency test that caught the bug only sometimes.** To fix the non-thread-safe `ArrayList` in `FakeDatabase`, Claude Code switched to a `CopyOnWriteArrayList` and wrote a test with 8 threads × 250 concurrent `save` calls. It passed. Claude Code then applied, without being asked, the "reintroduce the bug" check I had introduced in the first block, showing that the practice had carried over to its later work. It temporarily restored the `ArrayList` and ran the test 3 times. The test failed only once (2 users lost), so the first version would have let the bug through most of the time. Claude Code made the test more aggressive: 1000 users per thread, plus a `CountDownLatch` that starts all threads at the same moment. With the `ArrayList` the test then failed 5 times out of 5. With the fix restored, the full suite passed 3 runs in a row. Detection is still probabilistic, as with any concurrency test, but the test is now reliable in practice. Lesson: a test written by the AI that passes is not evidence that it detects the bug; it has to be seen failing.
- **A security test that passed for the wrong reason.**
  - *The bug.* For the JWT step, Claude Code wrote a test helper (`TestTokens`) that always set the issue time (`iat`) to "now". For an already expired token, the expiration was therefore *before* the issue time, and Spring Security rejected the token as inconsistent ("expiresAt must be after issuedAt") instead of as expired.
  - *How it surfaced.* The test for a token expired 10 seconds ago, which should be accepted thanks to the 30-second clock skew, failed. Claude Code read the rejection reason in the log and found the cause. This also meant that the test for a token expired beyond the clock skew had been passing for the wrong reason: it checked only the 401 code, not why the token was rejected.
  - *The fix.* Claude Code fixed the helper (tokens are now issued one hour before they expire) and strengthened the tests: for every invalid token (expired, missing expiration, wrong issuer, wrong signature, unsigned, malformed) they now also check the rejection reason written in the log. It then checked that each logged reason was the expected one.
  - *Lesson.* For security tests, asserting the error code is not enough; the test must also prove which check rejected the request.

## Approaches adopted

- **Fact-checking the AI's own documentation.** I dictated a list of "limitations" for `pre-analysis.md` that were partly inaccurate. Instead of writing them as given, Claude Code checked them against `git status` and the conversation history. It flagged the mismatches: `CLAUDE.md` had not been edited by hand, and four of the "missed" bugs had already been reported in a later message. It then asked how to proceed. The result was a more accurate account.
- **Verifying that the AI-written tests actually catch the bug.** A passing test proves nothing on its own. After Claude Code fixed the `lastName` mapping bug in `AddUserAssembler` and wrote `AddUserAssemblerTest`, I temporarily put the bug back (`getFirstName()` instead of `getLastName()`) and ran the tests: they failed. I then restored the fix. This confirmed that the test guards against the regression and does not pass trivially. After the bug-fixing blocks, I made this check a project rule in `CLAUDE.md`, so that every bug fix is verified this way.
- **Keeping the dependency upgrade under control.**
  - *Upgrade.* Claude Code proposed upgrading Spring Boot before the JWT step, because the 3.5 line reached the end of its open-source support on 2026-06-30. After I chose 4.1.1, Claude Code changed the version in `pom.xml` to see what would break. I stopped it and asked why it had changed the version. It explained, described the current state (only `pom.xml` changed, two test imports broken) and offered to revert. I confirmed 4.1.1 as a separate commit.
  - *Vulnerabilities found after the upgrade.* IntelliJ (Mend.io) reported transitive vulnerabilities on `spring-boot-starter-webmvc`: first one in Logback, then, after that was fixed, five more (four in Jackson shown in detail, one not shown). I shared IntelliJ's warnings. Claude Code looked up each CVE on NVD, matched it with the actual dependency tree and forced fixed versions through Spring Boot's version properties: Logback 1.6.5, Jackson 3.1.7, Tomcat 11.0.26.
  - *A second Logback CVE.* While checking the Logback fix, Claude Code found a newer CVE (CVE-2026-104721) showing that the obvious choice, 1.6.3, was still affected.
  - *The CVE that was not shown.* The two Tomcat CVEs were found by searching NVD for the Tomcat version in use, because IntelliJ did not show the fifth CVE in detail.
  - *Confirmation.* I reloaded the Maven project in IntelliJ and confirmed that the warnings were gone.

## Personal assessment

_To be completed_
