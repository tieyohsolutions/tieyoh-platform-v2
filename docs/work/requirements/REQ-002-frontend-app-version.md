# REQ-002 — Mobile app version number shown on the Profile screen

| Field | Value |
|---|---|
| Status | Approved (by Hany, 2026-10-15) |
| Jira type | Story |
| Priority | Low |
| Platforms | Android (iOS: see Scope) |
| Requested by | Hany |
| Created | 2026-10-15 |
| Branch | feature/REQ-002-frontend-app-version |
| Version bump | Sets the app version to 1.1 (starting value) |

## Background
Hany's request (verbatim):

> "create a version for the forntend app. it should start with 1.1. when we make a mintor change we will update the number to the right. for major changes we will update the number to the left. for now all updates will be to the number on the right. I will provide instrucutions on when we are making a minot vs major update. this should be displyed on the profile page under the log out button. it should be in bold font to the left side. skip Eureka in this run."

**What exists today** (observed in `Mobile/`):
- The Android app is in `Mobile/app`. Its package/namespace and applicationId are `com.tieyoh.mobile`. CLAUDE.md calls the module `tieyoh.app`, but the code uses `com.tieyoh.mobile`.
- `Mobile/app/build.gradle.kts` (`defaultConfig`) currently has `versionName = "1.0"` and `versionCode = 1`. These values were never changed from the project template, and the app does not display them anywhere.
- The Profile screen is the `ProfileScreen` composable in `Mobile/app/src/main/java/com/tieyoh/mobile/MainActivity.kt` (around line 4627). It opens from the `TieyohScreen.PROFILE` route (around line 1159). It is one vertically scrolling column with 20dp padding, showing in order: the "Profile" title, the identity card (photo, name, Tieyoh ID + QR, email), the "Edit profile" row, the "Privacy" row, the "Payment methods" row, and the full-width **"Log out"** button (`TieyohSecondaryButton`, around line 4875). Log out is the last item on the screen today.
- There is **no iOS project** in the repository yet (no `.swift` or `.xcodeproj` files found).

## User story
As an **admin** (Hany, operating Tieyoh), I want **the mobile app to have a version number that starts at 1.1, changes only when I say so, and appears on the Profile screen** so that **I can tell at a glance which app build is installed on a device**.

## Versioning rule (the business rule this REQ establishes)
- The app version has two numbers, written **MAJOR.MINOR** (left.right). The starting value is **1.1**.
- **Minor change**: the right number goes up by 1 (1.1 → 1.2 → 1.3 …). The left number stays the same.
- **Major change**: the left number goes up by 1 and the right number resets to 0 (1.7 → 2.0). See Decision D-2.
- **No bump unless Hany says so.** The version changes only when Hany explicitly says "minor" or "major". If he says nothing, the version stays where it is. Developers (Mississippi) and other agents never change the version on their own. See Decision D-3.
- For now Hany expects every bump to be **minor** (right number only).
- **iOS shares the version.** When the iOS app is built, it uses the same MAJOR.MINOR version as Android. See Decision D-4.
- **Single source of truth:** the app has exactly one place where the version is defined. Everything that shows or reports the version, including the Profile screen and the version Android shows under Settings > Apps, reads from that one place. The Profile screen must not contain a separately typed copy of the number.
- **Mapping to Android:** the human-readable version (Android "versionName") is the MAJOR.MINOR value, e.g. `1.1`. The internal build number (Android "versionCode") is not shown to users. It must go up by at least 1 every time the version changes, and it must never go down (Android requires this to install updates). See Assumption A-2.

## Acceptance criteria

### Version value
- **AC-1 — Starting version.** Given the app is built from this REQ's branch, when the installed app's version is checked in Android Settings > Apps > Tieyoh > App info, then it shows `1.1`.
- **AC-2 — Internal build number increased.** Given the previous build had internal build number (versionCode) 1, when this REQ is built, then the internal build number is greater than 1 (expected 2, per A-2).
- **AC-3 — One source of truth.** Given the version is defined in one place, when a reviewer searches the Android source for the literal version text (`1.1`), then it appears only in that one place. It does not appear as hard-coded text in the Profile screen code.
- **AC-4 — Changing the source changes the display.** Given a test build where the version source is temporarily changed to `1.2`, when the app is installed and the Profile screen is opened, then the screen shows `1.2` and Android App info also shows `1.2`, with no other code edits. *(Verification step only. The change is not committed.)*

### Display on the Profile screen
- **AC-5 — Shown below Log out.** Given a logged-in user opens the Profile screen, when they scroll to the bottom, then the version text appears directly below the "Log out" button and is the last item on the screen.
- **AC-6 — Format.** Given the app version is 1.1, when the Profile screen is shown, then the text reads exactly `Version 1.1`: the word "Version", one space, then the version number. It is not `v1.1` or `1.1` alone (Decision D-1).
- **AC-7 — Bold, left-aligned.** Given the Profile screen is shown, when the version text is inspected, then it is in bold and lines up with the left edge of the Log out button and the other Profile content (the screen's standard 20dp margin). It is not centered and not right-aligned.
- **AC-8 — Not tappable, Log out unchanged.** Given the version text is shown, when the user taps it, then nothing happens. When the user taps "Log out", it logs out exactly as before, and the version text does not overlap or shrink the Log out button's tap area.
- **AC-9 — Fully visible.** Given the Profile screen is scrolled to the bottom, when the user looks at the version text, then it is fully visible: not cut off by the screen edge or the bottom navigation bar, and with visible space between it and the Log out button.
- **AC-10 — Large text and small screens.** Given the device's font size is set to the largest setting, or the app runs on a small screen (e.g. 360dp wide), when the Profile screen is scrolled to the bottom, then `Version 1.1` is fully readable on one line and not truncated.
- **AC-11 — Screen reader.** Given TalkBack is on, when focus reaches the version text, then it reads out the version (e.g. "Version 1.1").
- **AC-12 — Works offline.** Given the device has no network connection, or the backend is down, when the Profile screen is opened, then the version is still shown correctly. The version never depends on a network call.
- **AC-13 — Same value across sessions.** Given the user logs out and logs in again (as the same or a different account), or force-closes and reopens the app, when they open the Profile screen, then the same version is shown.

### No regressions
- **AC-14 — Rest of Profile unchanged.** Given this change is installed, when the Profile screen is opened, then the title, identity card, Edit profile, Privacy, Payment methods, and Log out look and behave as they did before this change.
- **AC-15 — Upgrade install.** Given a device has the previous build (1.0, versionCode 1) installed, when the 1.1 build is installed over it, then the install succeeds as an update without uninstalling, the user's saved session and settings behave as they did before, and the Profile screen shows `Version 1.1`.
- **AC-16 — Build and tests.** Given this change, when the Android debug build and existing unit tests run, then both succeed.

### Process
- **AC-17 — No unrequested bumps.** Given any later REQ or change where Hany has not explicitly said "minor" or "major" (Version bump field is None or blank), when it is implemented, then the version remains unchanged (both the displayed version and the internal build number). Missouri should check this on later REQs (Decision D-3).
- **AC-18 — Minor bump.** Given the current version is 1.N and Hany explicitly says "minor" for a REQ, when that REQ is implemented, then the version becomes 1.(N+1) (e.g. 1.1 → 1.2) and the internal build number goes up by 1.
- **AC-19 — Major bump resets the right number.** Given the current version is X.Y and Hany explicitly says "major" for a REQ, when that REQ is implemented, then the version becomes (X+1).0 (e.g. 1.7 → 2.0, not 2.7) and the internal build number goes up by 1 (Decision D-2).

## Scope and process notes
- **In scope:** Android app only. This covers setting the version to 1.1, the versioning rule, and showing the version on the Profile screen.
- **iOS:** No iOS project exists yet, so there is nothing to change on iOS in this REQ. When the iOS app is built, it will share the same version number as Android (Decision D-4). That will be covered by a future iOS REQ.
- **Security review skipped for this run (Hany's instruction).** Eureka will not do the security review or the final scan for REQ-002. Verification is Missouri (QA) only. Nile should record in STATUS.md and in the close-out summary that Eureka was skipped at Hany's request.
- **Version bump field.** Starting with this REQ, each REQ header has a "Version bump" field (None / Minor / Major). Nile records Hany's explicit "minor" or "major" instruction there. If Hany says nothing, the field is None or blank and the version does not change (Decision D-3).

## Out of scope
- Any iOS work.
- Showing the version anywhere other than the Profile screen, such as the login screen, an About page, or a splash screen.
- Showing the backend version (REQ-001) in the app.
- Showing debug/release labels, build numbers, git commit IDs, or build dates in the app.
- Automatic version bumping, CI/CD, release tagging, or Play Store publishing.
- "Update available" prompts, or the backend checking or enforcing a minimum app version.
- Changing the backend's own version (`0.0.1-SNAPSHOT` in `pom.xml`).

## Money & data impact
- Touches balances / ledger / funding / redemption / ACH? **No.**
- Touches PII or card data? **No.** The version text is static app information. The Profile screen already shows the user's name, email, and Tieyoh ID; this change adds nothing to that.
- Merchant data? **No.**
- Compliance / security considerations: Minimal. The app version is shown only to a logged-in user on their own device. Android already makes it visible in App info. Eureka review was skipped at Hany's request.

## UI / API notes
- **Screen affected:** Profile (`ProfileScreen` in `Mobile/app/src/main/java/com/tieyoh/mobile/MainActivity.kt`), new last item below the "Log out" button.
- **Layout:** one line, bold, left-aligned with the existing content margin, a small gap above it (similar to the 16dp gaps already used between Profile items). Text size and color per Assumption A-4.
- **Version definition:** `Mobile/app/build.gradle.kts`, `defaultConfig` (`versionName` / `versionCode`). This is where the values live today. How the screen reads them is left to Mississippi.
- **No API or backend changes.**

## Assumptions
- **A-1:** "Frontend app" means the Tieyoh mobile app. Today that is only the Android app in `Mobile/app`. (Inferred; confirmed by Nile.)
- **A-2:** The internal Android build number (versionCode) goes from 1 to **2** for version 1.1, then up by 1 with every version change (1.2 → 3, 1.3 → 4, …). Hany never needs to manage it. (Inferred. Android requires it to increase.)
- **A-3:** Debug and release builds show the same version text, with no "-debug" suffix. (Inferred.)
- **A-4:** The version text uses the screen's normal body text size, in the same navy color as the other bold Profile labels ("Edit profile", "Privacy"). Hany specified bold and left-aligned only, so size and color are inferred.
- **A-5:** After right-number 9, the next minor version is **1.10**, then 1.11, and so on. Minor bumps never roll over into the left number automatically. A major bump happens only when Hany says so. (Inferred from "I will provide instructions…".)

## Decisions (resolved 2026-10-15)
Hany's answers (verbatim):

> "Question 1. display version 1.1, question 2. the right number reset to 0 after every new major version. question 3. it goes up only when I say minor or majoe. if I don't say anything it stays on the current version. question 4. IOS will be the same when I build it."

- **D-1 — Display text (was Q1).** The Profile screen shows `Version 1.1`, not `v1.1`. Applied in AC-6 and AC-11.
- **D-2 — Major bump (was Q2).** On every major bump the right number resets to 0 (1.7 → 2.0). Applied in the Versioning rule and AC-19.
- **D-3 — When a bump happens (was Q3).** The version goes up only when Hany explicitly says "minor" or "major". If he says nothing, it stays on the current version. Nothing bumps automatically. Applied in the Versioning rule, the Version bump field note, and AC-17/AC-18/AC-19.
- **D-4 — iOS (was Q4).** When the iOS app is built, it will use the same version number as Android. No iOS work in this REQ. Applied in the Versioning rule and Scope.

## Open questions for Hany
None. All four questions were resolved on 2026-10-15 (see Decisions).

## History
- 2026-10-15 — Created by Crocodile
- 2026-10-15 — Crocodile recorded Hany's answers to Q1–Q4 as Decisions D-1..D-4. Updated AC-6 and AC-17, added AC-18 (minor bump) and AC-19 (major bump resets to 0), and updated the Versioning rule and Scope notes. Assumptions unchanged. Status still Draft — awaiting Hany approval.
- 2026-10-15 — Hany approved (decisions D-1..D-4 and assumptions A-1..A-5 as written). Status → Approved.
