# DEF-002 — git.dirty is false when the build has untracked (non-ignored) files

| Field | Value |
|---|---|
| Status | Open |
| Jira type | Bug |
| Severity | Low |
| Linked requirement | REQ-001 (Decision 4: dirty flag; related to AC-5 intent) |
| Branch / commit tested | feature/REQ-001-backend-build-info @ c0878e3 |
| Found by | Missouri (QA) |
| Created | 2026-10-14 |

## Summary
`/actuator/info` reports `"dirty": false` when the working tree has new untracked source files, so the flag can say the commit id fully describes the build when it does not.

## Steps to reproduce
1. Fresh clone of the branch, `cd Backend/tieyoh-api`.
2. `echo x > newfile.txt` (untracked, not ignored); `./mvnw -q clean package -DskipTests`.
3. `grep dirty target/classes/git.properties`.
4. Modify a tracked file (`echo "<!-- x -->" >> pom.xml`), rebuild, grep again.

## Expected result
Per Decision 4, dirty = "build had uncommitted changes" (arguably including new untracked source files).

## Actual result
Untracked file: `git.dirty=false`. Tracked modification: `git.dirty=true`. (Already flagged by the developer; this is the git-commit-id plugin default, no `<excludeProperties>`/untracked option set.)

## Evidence
```
untracked: git.dirty=false
tracked-mod: git.dirty=true
```
Low: the REQ does not define whether untracked files count, nothing sensitive leaks, and tracked changes are detected. Recommend Hany accepts or asks for a one-line decision.

## Developer fix notes
_(Mississippi appends)_

## Verification
_(Missouri appends)_
