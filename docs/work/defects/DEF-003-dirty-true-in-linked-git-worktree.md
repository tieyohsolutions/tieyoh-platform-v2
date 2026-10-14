# DEF-003 — Builds from a linked git worktree report the wrong commit id and dirty=true

| Field | Value |
|---|---|
| Status | Open |
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
_(Mississippi appends)_

## Verification
_(Missouri appends)_
