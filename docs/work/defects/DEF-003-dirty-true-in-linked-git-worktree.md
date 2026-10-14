# DEF-003 — Builds from a linked git worktree report the wrong commit id and dirty=true

| Field | Value |
|---|---|
| Status | Fixed — awaiting re-test |
| Jira type | Bug |
| Severity | Medium |
| Linked requirement | REQ-001 (AC-5 commit matches checked-out source; Decision 4 dirty flag) |
| Branch / commit tested | feature/REQ-001-backend-build-info @ c0878e3, and qa/REQ-001-defects @ 1c88afd |
| Found by | Missouri (QA) |
| Created | 2026-10-14 |

## Summary
When built from a linked git worktree (e.g. `.claude/worktrees/...`), git-commit-id-maven-plugin 9.2.0 reads the MAIN checkout's HEAD instead of the worktree's HEAD: `git.commit.id.abbrev` is wrong and `git.dirty` is true on a clean tree. Built from a normal clone the values are correct.

## Steps to reproduce
1. Linked worktree on commit A (here: qa branch at 1c88afd); main checkout has another commit B checked out (here: feature branch at c0878e3).
2. `git rev-parse --short HEAD` in the worktree gives `1c88afd`; `git status --short` is empty.
3. `cd Backend/tieyoh-api && ./mvnw -q clean package -DskipTests && cat target/classes/git.properties`
4. Repeat in a plain `git clone`: values are correct.

## Expected result
`git.commit.id.abbrev=1c88afd`, `git.dirty=false` (AC-5: matches `git rev-parse --short HEAD` of the checked-out source).

## Actual result
```
worktree @ 1c88afd: git.commit.id.abbrev=c0878e3   <- HEAD of the main checkout, not the worktree
                    git.dirty=true                  <- clean tree compared against the wrong HEAD
plain clone @ c0878e3: git.commit.id.abbrev=c0878e3, git.dirty=false (correct)
```
(Earlier, at c0878e3 in the same worktree, the id looked right only because the main checkout was on the same commit; dirty=true was already visible.)

## Impact
`/actuator/info` can state a commit the running code was not built from, which defeats the purpose of REQ-001 for anyone building from a worktree (this project's agent workflow uses them). Workaround: build/deploy from a normal clone or the main checkout. Not an issue for a CI/container build from a standard checkout. Possible fixes for Mississippi: newer plugin / `useNativeGit=true`, or document the limitation.

## Developer fix notes
_Mississippi, 2026-10-14, commit 1aba418 on feature/REQ-001-backend-build-info_

- **Root cause:** git-commit-id-plugin-core's `GitDirLocator.resolveWorktree` maps a linked worktree's `.git` file (`gitdir: <repo>/.git/worktrees/<name>`) to the main `<repo>/.git`, so HEAD and the dirty check come from the main checkout. I reproduced this in a scratch clone with plugin 9.2.0 and 10.0.1 (latest), in both JGit and `useNativeGit=true` modes. All four report the main checkout's HEAD, so neither a plugin upgrade nor the native flag fixes it.
- **Change:** removed git-commit-id-maven-plugin. A `maven-antrun-plugin` execution (version managed by the Spring Boot parent, 3.2.0) in `generate-resources` runs the git CLI in the project directory: `rev-parse --short=7 --verify HEAD`, `log -1 --format=%cd` with UTC ISO format, and `status --porcelain` for dirty. The git CLI resolves linked worktrees, normal clones and the main checkout itself. It writes exactly three keys: `git.commit.id.abbrev`, `git.commit.time`, `git.dirty`. Author, email, message, remote, branch and tags are never read, and git output is captured into properties, not logged.
- **AC-9/AC-10 preserved:** the step first deletes any old `target/classes/git.properties` so no stale values survive. If git isn't on PATH, or there's no .git, or any git command fails, no git.properties is written and the build still succeeds. `/actuator/info` then omits the git section. Verified with a copy that has no .git (full package passes, the jar has no git.properties, the git checks skip) and with git removed from PATH (build succeeds, no errors logged).
- **Test added:** `GitPropertiesMatchCheckoutTests.commitIdIsHeadOfThisCheckout` (and `containsOnlyApprovedKeys`). With the old plugin config in a linked worktree it fails (`expected "65d2a3e" but was "62bd9c1"`). With the fix it passes.
- **Verified:** in a scratch linked worktree on a different commit than its main checkout (which had uncommitted changes), the full `./mvnw clean test` passed 45/45, with `git.commit.id.abbrev` equal to the worktree HEAD and `git.dirty=false`. In a fresh clone the packaged jar's `BOOT-INF/classes/git.properties` holds only the three keys, and the build log has no author, email, remote, branch or message.

## Verification
_(Missouri appends)_
