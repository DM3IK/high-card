# Final Report

## Key prompts used

_To be completed_

## Difficulties encountered

_To be completed_

## Approaches adopted

- **Fact-checking the AI's own documentation.** I dictated a list of "limitations" for `pre-analysis.md` that were partly inaccurate. Instead of writing them as given, Claude Code checked them against `git status` and the conversation history. It flagged the mismatches: `CLAUDE.md` had not been edited by hand, and four of the "missed" bugs had already been reported in a later message. It then asked how to proceed. The result was a more accurate account.
- **Verifying that the AI-written tests actually catch the bug.** A passing test proves nothing on its own. After Claude Code fixed the `lastName` mapping bug in `AddUserAssembler` and wrote `AddUserAssemblerTest`, I temporarily put the bug back (`getFirstName()` instead of `getLastName()`) and ran the tests: they failed. I then restored the fix. This confirmed that the test guards against the regression and does not pass trivially.

## Personal assessment

_To be completed_
