---
name: feature
description: Start a new Tieyoh feature or change through the full agent pipeline (requirements → approval → development → QA + security → fix loop). Use when Hany types /feature followed by a description.
---

Run the pipeline in `CLAUDE.md` for this request:

$ARGUMENTS

1. Assign the next `REQ-###` ID and delegate to the `crocodile` subagent with the request verbatim.
2. Show Hany the requirement summary and open questions. **Stop and wait for approval.**
3. On approval: `mississippi` → then `missouri` and `eureka` in parallel.
4. If any defects or findings are logged: show Hany the triage table (ID, severity, summary, recommended action). **Stop and wait for his decisions.** Send only "Fix now" items to `mississippi`, re-verify, and repeat triage if new items appear (max 3 loops) → close-out summary.
5. Keep `docs/work/STATUS.md` updated at each stage.
