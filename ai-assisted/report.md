# Final Report

## Key prompts used

_To be completed_

## Difficulties encountered

- **Correct, tested code with a design flaw that the tests could not catch.** In the catch block for unexpected errors in `UserServiceImpl.addUser`, Claude Code logged the error first and created the generic `GenericException` afterwards. The trace id returned to the client was therefore never written to the log. If a user reported "I got an error with traceId abc-123", nobody could find it on the server. The code compiled and all tests passed, because no test checked the log. The flaw came up while reviewing the diff, where I used Claude (chat) as a second reviewer of Claude Code's output. I evaluated the point, agreed it was a real gap and asked Claude Code to reverse the order: create the exception first, then log it with its trace id. I also asked to update the tests where needed; Claude Code extended the existing test to check the log content. The test now captures the log output and checks that it contains the same trace id the client receives. Lesson: green tests only cover what they assert; reviewing the design is still a human job.
- **A concurrency test that caught the bug only sometimes.** To fix the non-thread-safe `ArrayList` in `FakeDatabase`, Claude Code switched to a `CopyOnWriteArrayList` and wrote a test with 8 threads × 250 concurrent `save` calls. It passed. Claude Code then applied, without being asked, the "reintroduce the bug" check I had introduced in the first block, showing that the practice had carried over to its later work. It temporarily restored the `ArrayList` and ran the test 3 times. The test failed only once (2 users lost), so the first version would have let the bug through most of the time. Claude Code made the test more aggressive: 1000 users per thread, plus a `CountDownLatch` that starts all threads at the same moment. With the `ArrayList` the test then failed 5 times out of 5. With the fix restored, the full suite passed 3 runs in a row. Detection is still probabilistic, as with any concurrency test, but the test is now reliable in practice. Lesson: a test written by the AI that passes is not evidence that it detects the bug; it has to be seen failing.

## Approaches adopted

- **Fact-checking the AI's own documentation.** I dictated a list of "limitations" for `pre-analysis.md` that were partly inaccurate. Instead of writing them as given, Claude Code checked them against `git status` and the conversation history. It flagged the mismatches: `CLAUDE.md` had not been edited by hand, and four of the "missed" bugs had already been reported in a later message. It then asked how to proceed. The result was a more accurate account.
- **Verifying that the AI-written tests actually catch the bug.** A passing test proves nothing on its own. After Claude Code fixed the `lastName` mapping bug in `AddUserAssembler` and wrote `AddUserAssemblerTest`, I temporarily put the bug back (`getFirstName()` instead of `getLastName()`) and ran the tests: they failed. I then restored the fix. This confirmed that the test guards against the regression and does not pass trivially. After the bug-fixing blocks, I made this check a project rule in `CLAUDE.md`, so that every bug fix is verified this way.

## Personal assessment

_To be completed_
