# Finance Tracker — Build Brief

**Status:** Shaped. Direction chosen. Ready for the build pass.
**Product truth:** [PRODUCT.md](PRODUCT.md)
**Surface mode:** Operate
**Build path:** code-led (no comp owed)
**Chosen direction:** Budget Pass
**Chosen catalog world:** `vernacular-ephemera-boarding-pass-and-gate-board`
**Direction seed reference:** `seed:efbbef6a3b015237`
**Chosen sketch:** `.impeccable/mocks/decision/challenger-boarding-pass.html`
**Sketch capture:** `.impeccable/review/sketches/challenger-boarding-pass.png`
**Reference board:** `.impeccable/mocks/refs/vernacular-ephemera-boarding-pass-and-gate-board.webp`
**Reference hero:** `.impeccable/mocks/refs/vernacular-ephemera-boarding-pass-and-gate-board-hero.webp`

This file is the handoff for the next session. Read it with `PRODUCT.md`, then follow the Impeccable new-work flow. Do not start from the old journaling scaffold as a design direction; it is a placeholder.

---

## 1. Job and audience

- One owner, one Android phone. The tracker is glanced at in the notification shade or opened for a quick check — not used as a budgeting hobby.
- Primary job: answer **“how much is left?”** in seconds, and log an expense in **1–2 taps without navigating the app**.
- Monthly job: stay inside the cap. The pass’s status is the pacing signal; the board is the record.
- Success: capture from the shade in under 5 seconds, the home answers the budget question at a glance, the month ends on plan, and the record survives process death.

## 2. Outcome and proof

- The owner trusts the number and logs without dread. The screen shows the exact remaining budget, cap, percent, days left, daily allowance, and recent transactions with a running balance.
- Product-specific truth: single owner, local-only, **amount-only capture from the notification action**, budget-first.
- No fabricated financial claims. The app starts empty; synthetic sample data appears only in previews and tests, labeled as such.
- Anti-goals: no bank sync, no multi-user sharing, no generic journal, no confetti/coin gamification.

## 3. Selected direction — Budget Pass

- **World:** travel documents and live gate boards — a segmented budget pass above a departure-style activity board. The scene is a phone in mixed light, often glanced at in the shade; the dark board makes the white pass the lit object.
- **Palette:** ink `#0B0D10`, slate `#1C2127`, steel `#6B7178`, panel `#E6E8EB`, surface `#FFFFFF`, alert `#FFD400`; a functional over-budget red with a text-safe darker variant.
- **Type:** condensed grotesque display for the pass amount, mono for labels/timestamps/figures, humanist sans for body. Fonts bundled, never fetched at runtime.
- **Thesis:** the budget is a pass; activity is a live board that keeps changed rows lit until noticed. The world lends type, palette, density, and one signature move; navigation and controls stay standard Material 3.
- **First viewport:** dark board ground; top app bar with “Psyche” and the month; a white pass carrying period, remaining, cap, percent, week/day/status segments, a real period reference, and notification-log status; below it, “Live activity” with time, description, amount, and running balance columns; bottom navigation; yellow FAB. The pass is the hero; the board is the record.
- **Signature:** a notification capture reprints the pass with the new remaining amount and inserts a lit **NOW** row at the top of the board. The row clears when opened or on the next capture. Haptic on save; no confetti.
- **Sequence:** notification action → amount pad → save → pass reprints + row lights → dismiss back to the previous screen. In-app: Home ↔ Activity ↔ Budgets ↔ Accounts.
- **Reach:** the pass/board grammar can later carry a DTR shift pass or a hobby session board, but no other module ships in this build.
- **Honest risk:** the travel metaphor can become a costume. The board must not outrank the budget, and the barcode must be a real period reference, not a fake scanner.
- **Composition reference:** the chosen sketch above. The build matches its arrangement, palette, type, and density, then extends it to responsive, light theme, and every state.

### The chosen first viewport, concretely

- Dark ground, status bar, app bar: `PSYCHE` + month.
- White budget pass card with a segmented structure:
  - period (`SEP 2026`), sequence/reference,
  - remaining amount (`₱8,240`), cap (`₱12,000`), percent (`68.7%`),
  - week (`4/5`), daily allowance (`₱686`), status (`ON PLAN`),
  - period reference block and `NOTIFICATION LOG ON`.
- Below the pass: `LIVE ACTIVITY` board with `TIME / DESCRIPTION / AMOUNT / LEFT` columns.
- Newest row at top, highlighted amber, with a `NOW` chip; older rows below with running balances.
- Standard bottom navigation: Home, Activity, Budgets, Accounts.
- Yellow FAB for the in-app capture fallback.
- **Truth requirement:** all figures derive from one ledger. Sample data must reconcile dates, days left, daily allowance, percentages, and running balances. Do not copy the sketch’s inconsistent “28 SEP / 12 days remaining” combination.

## 4. Scope and boundaries

- **First build spine:** home (pass + board); notification capture; transaction edit/delete; monthly cap + optional category allocations; accounts with cash, debit/e-wallet, and credit types plus card last four; local persistence.
- **Included at a simple level:** income logging; savings goals with transfer contributions. If the build must cut, cut these before the budget/capture spine.
- **Follows in the same direction:** search/filter, month trend, account/card detail, notification and privacy settings.
- **Later:** recurring bills, bank sync/import, multi-currency, shared access, advanced reports, XP rules.
- **Target:** Android phone first; minSdk 26, target 36; Compose Material 3; edge-to-edge; predictive Back; font scale 1.3; dark and light.
- **Untouched:** product name, single-owner/local-only constraint, existing test scaffolding where useful. The placeholder journaling copy and single-entry home are replaced, not preserved.

## 5. States and ranges

- **First run:** no cap or account → set a cap and add a cash account; the pass says “Set your cap,” never a fake number.
- **Empty:** no activity today → quiet board; no categories or goals → add prompts.
- **Capture:** amount pad; zero/negative invalid; save success; save failure with retry; notification permission denied → in-app FAB fallback plus a one-time explanation.
- **Budget:** on plan; tight (low remaining or daily allowance); over (negative, red, “₱X over”); category over but total okay; month rollover.
- **Board:** new row lit; edited row; deleted row with undo; long description truncation; many rows; filter-empty state.
- **Amounts:** ₱0.01–₱999,999.99, grouped; the pass amount scales down rather than clipping. Dates “28 SEP”; time “9:41.”
- **Privacy:** lock-screen notification hides the amount by default, with a setting to show it. No analytics.
- **Data ranges:** 0–20 transactions/day typical; 0–50 categories; 1–10 accounts; 0–10 goals; 12 months of history.

## 6. Interaction and layout

- **Home hierarchy:** pass first (remaining, cap, percent, period, daily allowance, status); live board of the latest 5–8 rows; compact accounts/goals strip; bottom navigation; FAB. The pass is a segmented object, not a generic hero-metric card.
- **Capture:** the notification action opens a lightweight dialog/sheet with the amount pad focused. Default category and account are the last used; one tap Save. A transient confirmation offers “Change category” and “Undo,” then dismisses. No full app navigation; amount-only is explicit, and category/account remain editable afterward.
- **Budget editing:** a sheet for the cap and category allocations with sum validation; changes reprint the pass. No silent budget changes.
- **Activity:** tap a row → detail sheet for amount, category, account, date, note; edit/delete with undo; the running balance updates.
- **Accounts:** type, balance, card last four; credit shows limit and outstanding. Transfers between accounts are not expenses.
- **Goals:** list and contribute from an account; contributions are transfers, not expenses.
- **Motion:** one authored moment — pass reprint plus row light. Respect Remove animations with a crossfade or instant cut.
- **Responsiveness:** portrait phone first; hold at 320dp width and 1.3 font scale; don’t break at 600dp; system Back always works.

## 7. Constraints and open decisions

- **Persistence:** Room is required before this ships; the current in-memory repository is a placeholder. Local-only, no network.
- **Notification:** `POST_NOTIFICATIONS` runtime permission on Android 13+; a silent ongoing channel with “Log expense”; the action launches a dialog-themed capture activity; the FAB is the fallback.
- **Theme:** dynamic color disabled in favor of the fixed palette; dark is the primary composition, light is the day-terminal variant, not an inversion.
- **Barcode:** make it a functional period reference — tapping copies a plain-text month summary — or remove it. Recommend functional, labeled “period ref.”
- **Cards:** account types plus last four; no card-network integration.
- **XP:** rules are still undecided, so no XP or levels appear on the finance home in this build. The pass’s “on plan / tight / over” status is the only progress signal; the shell leaves room for XP later.

### Assumption to confirm — budget definition

The budget-definition question was skipped, so this brief assumes:

- A **monthly spending cap** with optional category allocations that sum to it.
- **Left to spend** = cap − expenses.
- **Credit-card spending** counts against the cap but not cash; paying the card is a transfer.
- **Savings contributions** are transfers, not expenses.
- **Income** is recorded separately and does not silently expand the cap.

If this is wrong, the pass’s math changes.

### Currency / locale

Assumed **PHP (₱), en-PH, 12-hour time**. Confirm or correct.

## Next action

1. Confirm the budget definition and currency above (the only assumptions that change the numbers).
2. Resume the Impeccable new-work flow:
   - run `impeccable context` once,
   - record the direction contract in the Finance Tracker surface brief with the seed reference,
   - implement the home + notification capture + persistence spine against the chosen sketch,
   - verify on the emulator in dark and light, at font scale 1.3,
   - run the finish review and write `DESIGN.md`.
3. Do not ship the old journaling copy, the single-entry home, or any fabricated financial data.

The chosen sketch is the first-viewport composition, not the whole product. Build the working screen, not a poster of it.
