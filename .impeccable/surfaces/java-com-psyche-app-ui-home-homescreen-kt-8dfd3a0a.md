---
version: 1
slug: "java-com-psyche-app-ui-home-homescreen-kt-8dfd3a0a"
primary_target: "app/src/main/java/com/psyche/app/ui/screens/HomeScreen.kt"
related_targets: ["app/src/main/java/com/psyche/app/MainActivity.kt","app/src/main/java/com/psyche/app/CaptureActivity.kt","app/src/main/java/com/psyche/app/ui/PsycheApp.kt","app/src/main/java/com/psyche/app/ui/components/BudgetPassCard.kt","app/src/main/java/com/psyche/app/ui/components/ActivityBoard.kt","app/src/main/java/com/psyche/app/ui/sheets/FinanceSheets.kt"]
---

# Finance Tracker — home and capture

**Scope:** the finance tracker spine: Home (budget pass + live activity board), notification and in-app capture, Activity, Budgets, Accounts, savings goals, and local persistence.
**Mode:** Operate.
**Audience and job:** one owner, one Android phone, glancing for "how much is left?" and logging an expense from the shade in under five seconds.
**Constraints:** PHP / en-PH / 12-hour time; monthly spending cap with optional category allocations; credit spending counts against the cap; transfers and savings contributions are not expenses; income never silently expands the cap; local-only; no fabricated financial data.
**States owned:** first run, no cap, empty month, lit new row, tight, over, category over, month rollover, edited/deleted row with undo, filter-empty, permission denied, save failure, 320dp width at 1.3 font scale, dark and light.

## Direction contract

THESIS: The budget is a boarding pass; activity is a live gate board. It refuses the generic hero-metric card by making remaining budget a segmented document whose barcode is a real, copyable period reference, with the board kept subordinate as the record.

OWN-WORLD: Ink #0B0D10 ground, slate #1C2127 rows, steel #6B7178 quiet labels, white #FFFFFF pass, panel #E6E8EB daylight ground, alert #FFD400 used only for the pass's active row, FAB, and selected nav. Condensed grotesque (Barlow Condensed) carries the pass amount; Public Sans carries body; Roboto Mono carries labels, stamps, money tables. Hairline rules, notches, perforation, tabular figures, and precise ticket density; standard Material 3 navigation and controls.

STORY: The owner reads the pass and knows what is left, what is safe to spend today, and whether the month is on plan. A notification capture makes the pass reprint, inserts a lit NOW row, and dismisses. The row stays lit until opened or the next capture.

FIRST VIEWPORT: Dark board ground. Top bar: PSYCHE, month + week, settings. White pass at full width: BUDGET PASS / SEQ, period and days remaining, large remaining amount with cap and percent left, three segments for week / daily allowance / status, functional period reference bars and NOTIFICATION LOG state. Below: LIVE ACTIVITY board with TIME / DESCRIPTION / AMOUNT / LEFT columns, newest row amber with NOW chip, older rows receding. Bottom Material 3 navigation (Home, Activity, Budgets, Accounts) and a yellow FAB at the lower right.

FORM: Budget Pass, chosen from the dealt direction hand; code-led, matched to `.impeccable/review/sketches/challenger-boarding-pass.png`; seed:efbbef6a3b015237.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.
