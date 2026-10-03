---
name: pre-push-check
description: Verifies code is ready before pushing to GitHub or opening a PR. Use when the user says they're about to push, ready to merge, done with a change, wants to open a PR, or asks to verify/check their work before pushing.
allowed-tools: Bash, Read, Grep, Glob
---

You are verifying that recent changes are safe to push to the Civitas repository. Do not skip steps or assume something passed without actually running it.

## Steps

1. **Run the full test suite.**
   Run `./mvnw test` and capture the real output. Do not proceed on the assumption tests pass — you must see the actual result.

2. **Read the diff.**
   Run `git diff main...HEAD` (or `git diff --staged` if nothing is committed yet) to see every changed file.

3. **Check for weakened tests.**
   Compare the diff against the previous version of any modified test file. Flag anything that:
    - Removed or loosened an assertion
    - Added `@Disabled` / `@Ignore` to a previously active test
    - Changed an exact assertion to a vague one (e.g. `assertNotNull` replacing a specific value check)

4. **Check union-scoping (critical for this project).**
   For every repository/service method touched in the diff:
    - Any new or modified repository query must be scoped by `Union` (e.g. `findAllByUnion(...)`) — flag any query that isn't.
    - Any new entity creation must call `.setUnion(...)` before saving — flag if missing.
    - Any update/delete on a tenant-scoped entity must call a `verifyXxxBelongsToUnion()` guard first — flag if missing.
    - Reference the pattern in CLAUDE.md's "Multi-Tenancy Pattern" section as the standard to check against.

5. **Check for leftover debug artifacts.**
   Grep the diff for `System.out.println`, `TODO`, `FIXME`, hardcoded credentials, or commented-out code blocks that look accidental rather than intentional.

6. **Compile check.**
   Run `./mvnw compile` to catch anything the diff might have broken that tests didn't cover.

## Output format

Report your findings in this structure:

1. **Test Results**: Pass/fail, with the actual test count and any failures shown verbatim.
2. **Union-Scoping Review**: List every touched repository/service method and whether it correctly follows the multi-tenancy pattern. Flag any violation explicitly — do not soften this.
3. **Test Integrity**: Any weakened/disabled tests found, or "none found."
4. **Debug Artifacts**: Any leftover debug code found, or "none found."
5. **Compile Status**: Pass/fail.
6. **Verdict**: A single clear statement — "Safe to push" or "Do not push — [specific reason]."

Do not say "looks good" without having actually run steps 1 and 6. A visual read of the diff is not a substitute for running the tests and compiler.