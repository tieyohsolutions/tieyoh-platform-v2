# Tieyoh Agent Team — Install & Use

| Name | Role |
|---|---|
| **Nile Orchestrator** | The one you talk to. Runs the pipeline, never writes code. |
| **Crocodile Analyst** | Product manager / analyst — requirements & acceptance criteria |
| **Mississippi** | Developer — backend (Spring Boot), Android (Kotlin), iOS (Swift/Xcode) |
| **Missouri** | QA tester — tests, defects |
| **Eureka Security Engineer** | Security reviews, scans, final scan |

## 1. Install (once, on the MacBook)

1. Unzip this package and copy its contents into the **root** of your repo:
   ```bash
   cd ~/Projects/tieyoh-platform
   cp -R ~/Downloads/tieyoh-agent-team/. .
   ```
   You should now have `CLAUDE.md`, `.claude/settings.json`, `.claude/agents/`, `.claude/skills/feature/`, and `docs/work/` at the repo root.
   If the repo already has a `.claude/settings.json`, merge the `permissions` lists instead of overwriting.
   If the repo already has a `CLAUDE.md`, merge the two instead of overwriting.
2. Commit it on a branch so the setup is versioned:
   ```bash
   git checkout -b chore/agent-team
   git add CLAUDE.md .claude docs/work README-AGENT-TEAM.md
   git commit -m "Add Tieyoh agent team (Nile, Crocodile, Mississippi, Missouri, Eureka)"
   ```
3. Install the security scanners Eureka uses (optional but recommended):
   ```bash
   brew install gitleaks semgrep
   ```
   OWASP dependency-check runs through Maven and needs no install.
4. Make sure Claude Code is installed and you're signed in, then start it from the repo root:
   ```bash
   cd ~/Projects/tieyoh-platform
   claude
   ```
   Type `/agents` to confirm the four agents are listed.

## 2. Use

Start a feature:
```
/feature Recipients can see all gifts they've received for one merchant, with a running total
```
Nile will:
1. Have Crocodile write `REQ-###` → **stop for your approval**.
2. Have Mississippi build it on `feature/REQ-###-…`.
3. Run Missouri (QA) and Eureka (security) in parallel → **stop for your triage** (Fix now / Defer / Accept).
4. Loop fixes (max 3), then Eureka's final scan.
5. Give you a close-out summary. Lows are logged in `docs/work/LOW-RISK-LOG.md`. Nothing merges without your OK.

Other things you can say: "status", "show open defects", "ask Eureka to scan the whole repo", "send REQ-003 back to Crocodile", "skip to triage".

## 3. Before stepping away
Keep the Mac awake while a run is going: plug it in and set **System Settings → Battery/Energy → Prevent automatic sleeping when the display is off**, or run `caffeinate -i` in a spare terminal. Remember the pipeline pauses at the two stops until you answer.

## 4. Where things live
- `docs/work/requirements/` — REQ-### (→ Jira Story)
- `docs/work/defects/` — DEF-### (→ Jira Bug)
- `docs/work/security/` — SEC-### (→ Jira Security Bug)
- `docs/work/STATUS.md` — status board
- `docs/work/LOW-RISK-LOG.md` — every Low, with your decision
- `docs/work/templates/` — formats the agents follow
