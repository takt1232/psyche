---
version: 2
slug: "java-com-psyche-app-ui-home-homescreen-kt-8dfd3a0a"
primary_target: "app/src/main/java/com/psyche/app/ui/screens/HomeScreen.kt"
related_targets: ["app/src/main/java/com/psyche/app/MainActivity.kt","app/src/main/java/com/psyche/app/widget/QuickTransactionWidget.kt","app/src/main/java/com/psyche/app/widget/QuickWidgetRenderer.kt","app/src/main/java/com/psyche/app/ui/PsycheApp.kt","app/src/main/java/com/psyche/app/ui/components/BalancePassCard.kt","app/src/main/java/com/psyche/app/ui/components/ActivityBoard.kt","app/src/main/java/com/psyche/app/ui/sheets/FinanceSheets.kt"]
---

# Finance Tracker — home and capture

**Scope:** the finance tracker spine: Home (balance pass + account balances + live activity board + one-tap strip), the home-screen one-tap widget, Activity, Accounts, savings goals, and local persistence.
**Mode:** Operate.
**Audience and job:** one owner, one Android phone, glancing for "could I settle every card today?" and logging a preset expense from the widget in one tap.
**Constraints:** PHP / en-PH / 12-hour time; net position = active account money + active savings goals − total credit-card outstanding; full outstanding counts immediately; card payments and goal contributions are net-neutral transfers; archived accounts and goals are excluded from totals; local-only; no fabricated financial data.
**States owned:** first run, zero balance, debt-free, can settle, short (negative net), empty month, lit new row, month rollover, edited/deleted row with undo, filter-empty, no one-tap actions, four-slot limit, widget save failure, 320dp width at 1.3 font scale, dark and light.

## Direction contract

THESIS: The balance is a boarding pass; activity is a live gate board. It refuses the generic hero-metric card by making the signed net position a segmented document whose barcode is a real, copyable snapshot reference, with the board kept subordinate as the record.

OWN-WORLD: Ink #0B0D10 ground, slate #1C2127 rows, steel #6B7178 quiet labels, white #FFFFFF pass, panel #E6E8EB daylight ground, alert #FFD400 used only for the pass's active row, FAB, and selected nav. Condensed grotesque (Barlow Condensed) carries the net amount; Public Sans carries body; Roboto Mono carries labels, stamps, money tables. Hairline rules, notches, perforation, tabular figures, and precise ticket density; standard Material 3 navigation and controls.

STORY: The owner reads the pass and knows the signed net position, how much money and card debt make it up, and whether the money covers every card. A one-tap widget action logs the preset, makes the widget and the pass reprint, and inserts a lit NOW row. The row stays lit until opened or the next capture.

FIRST VIEWPORT: Dark board ground. Top bar: PSYCHE, date + status, settings. White pass at full width: BALANCE PASS / AS OF, large signed net position with MONEY and OWED side stack, debt-coverage bar when cards are owed, three segments for CASH / SAVED / STATUS, functional snapshot reference bars and ONE-TAP status. Below: ACCOUNTS rows with calculated balances, then the ONE-TAP strip with up to four preset buttons, then the LIVE ACTIVITY board with TIME / DESCRIPTION / AMOUNT / NET columns, newest row amber with NOW chip, older rows receding. Bottom Material 3 navigation (Home, Activity, Accounts) and a yellow FAB at the lower right.

FORM: Balance Pass, extending the chosen departure-board world; code-led, matched to `.impeccable/review/sketches/challenger-boarding-pass.png`; seed:efbbef6a3b015237.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.
