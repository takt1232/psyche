# Finance Tracker — Build Brief

**Status:** Shaped. Balance pivot built and verified on the Android phone. Version 1.1.0 installed.
**Product truth:** [PRODUCT.md](PRODUCT.md)
**Design system:** [DESIGN.md](DESIGN.md)
**Surface mode:** Operate
**Build path:** code-led
**Chosen direction:** Balance Pass
**Chosen catalog world:** `vernacular-ephemera-boarding-pass-and-gate-board`
**Direction seed reference:** `seed:efbbef6a3b015237`
**Chosen sketch:** `.impeccable/mocks/decision/challenger-boarding-pass.html`
**Sketch capture:** `.impeccable/review/sketches/challenger-boarding-pass.png`
**Reference board:** `.impeccable/mocks/refs/vernacular-ephemera-boarding-pass-and-gate-board.webp`
**Reference hero:** `.impeccable/mocks/refs/vernacular-ephemera-boarding-pass-and-gate-board-hero.webp`

> **Revision · 12 Nov 2026.** The monthly budget and category-allocation feature
> has been removed. The pass now states the owner's net position across active
> accounts and savings goals minus total credit-card outstanding. Navigation is
> Home, Activity, Accounts. The full outstanding counts immediately; monthly
> card payments are transfers and do not change net.

---

## 1. Job and audience

- One owner, one Android phone. The tracker is glanced at on the home-screen widget or opened for a quick check.
- Primary job: answer **“if every card were paid today, would I have money left?”** in seconds, and log an expense in one tap without opening the app.
- Success: the pass shows an honest signed net position, the owner can scan every active account and card outstanding, capture from the widget works, and the record survives process death.

## 2. Outcome and proof

- The owner trusts the number and logs without dread. The pass shows net position, money, owed, debt coverage, cash, savings, and status. The activity board shows the running net after each row.
- Product-specific truth: single owner, local-only, one-tap preset capture from the home-screen widget, balance-first.
- No fabricated financial claims. The app starts empty; synthetic sample data appears only in previews and tests, labeled as such.
- Anti-goals: no bank sync, no multi-user sharing, no generic journal, no confetti/coin gamification, no monthly cap.

## 3. Selected direction — Balance Pass

- **World:** travel documents and live gate boards — a segmented balance pass above a departure-style activity board. The pass is the lit object; the board is the record.
- **Palette:** ink `#0B0D10`, slate `#1C2127`, steel `#6B7178`, panel `#E6E8EB`, surface `#FFFFFF`, alert `#FFD400`; a functional danger red for negative net, shortfall, and destructive actions.
- **Type:** condensed grotesque display for the signed net, mono for labels/timestamps/figures, humanist sans for body. Fonts bundled, never fetched at runtime.
- **Thesis:** the pass is a solvency document. Net position is the hero; money and owed are the two components; the debt-coverage bar answers the cover question; the activity board keeps the running record.
- **First viewport:** dark board ground; top app bar with “Psyche” and date/status; a white pass carrying `BALANCE PASS`, as-of date, signed net, `MONEY` and `OWED`, a debt-coverage bar when cards are owed, `CASH / SAVED / STATUS`, and a snapshot reference. Below it: active accounts with calculated balances, one-tap strip, live activity board with `NET`, bottom navigation, yellow FAB.
- **Signature:** a card payment is a transfer: cash and outstanding fall together, and the net stays still. A card purchase lowers net. A one-tap widget capture logs the preset, reprints the widget/pass, and inserts a lit **NOW** row.
- **Sequence:** widget tap → log + toast → widget reprints; in-app, the pass reprints + lit row. In-app navigation: Home ↔ Activity ↔ Accounts.
- **Honest risk:** the travel metaphor can become a costume. The board must not outrank the pass, and the barcode must be a real snapshot reference, not a fake scanner.
- **Composition reference:** the chosen sketch above. The build keeps its arrangement, palette, type, and density while extending it to the balance model, responsive behavior, light theme, and every state.

### The chosen first viewport, concretely

- Dark ground, status bar, app bar: `PSYCHE` + date/status.
- White balance pass:
  - top strip `BALANCE PASS`, `AS OF 12 NOV`;
  - `NET POSITION`, active account count;
  - large signed net amount;
  - `MONEY` and `OWED` side stack;
  - debt-coverage bar and percentage when cards are owed;
  - `CASH / SAVED / STATUS` segments;
  - snapshot reference block and `ONE-TAP` status.
- Below the pass: `ACCOUNTS` rows with calculated balances, card limits and available credit; goals compact progress rows.
- `ONE-TAP` strip with up to four presets.
- `LIVE ACTIVITY` board with `TIME / DESCRIPTION / AMOUNT / NET`.
- Newest row at top, highlighted amber, with a `NOW` chip; older rows below with the running net.
- Standard bottom navigation: Home, Activity, Accounts.
- Yellow FAB for the in-app capture fallback.
- **Truth requirement:** all figures derive from one ledger. Sample data must reconcile dates, account balances, savings, outstanding, and running net.

## 4. Scope and boundaries

- **First build spine:** home (balance pass + accounts + one-tap + board); home-screen one-tap widget; transaction edit/delete; active cash/debit/credit accounts and calculated balances; local persistence.
- **Included at a simple level:** income logging; savings goals with transfer contributions; expense categories as labels.
- **Explicitly removed:** monthly cap, category allocations, budget editor, allocations sheet, dedicated Budgets screen.
- **Follows in the same direction:** search/filter, month trend, account/card detail, widget sizing and privacy settings.
- **Later:** recurring bills, bank sync/import, multi-currency, shared access, advanced reports, XP rules.
- **Target:** Android phone first; minSdk 26, target 36; Compose Material 3; edge-to-edge; predictive Back; font scale 1.3; dark and light.
- **Untouched:** product name, single-owner/local-only constraint, existing test scaffolding where useful.

## 5. States and ranges

- **First run:** default Cash at ₱0 and no entries → pass shows ₱0; a setup panel offers Set cash and Add card. Never a fake number.
- **Empty:** no activity today → quiet board; no goals → add prompts.
- **Position:** debt-free; can settle every card; short (negative net, danger red, `SHORT`); no active accounts.
- **Capture:** amount pad; zero/negative invalid; save success; save failure with retry; widget unavailable or no presets → in-app FAB fallback plus the one-tap setup prompt.
- **Board:** new row lit; edited row; deleted row with undo; long description truncation; many rows; filter-empty state.
- **Amounts:** ₱0.01–₱999,999.99 per entry, grouped; the pass amount scales down rather than clipping. Dates “12 NOV”; time “9:41.”
- **Privacy:** the widget is a home-screen surface only; it has no lock-screen or notification surface. No analytics.
- **Data ranges:** 0–20 transactions/day typical; 0–50 categories; 1–10 accounts; 0–10 goals; 12 months of history.

## 6. Interaction and layout

- **Home hierarchy:** balance pass first; active accounts with calculated balances; one-tap strip; live board; bottom navigation; FAB.
- **Capture:** a widget preset logs its stored amount, account, and category immediately and shows a toast. In-app, the same presets appear on the Home strip with an Undo banner; the FAB opens the amount pad. Amount-only capture remains the quick path.
- **Accounts:** type, calculated balance, card last four; credit shows limit, outstanding, and available credit. Transfers between accounts are not expenses.
- **Card payments:** a transfer from cash/debit to a credit account, capped at outstanding; net is unchanged.
- **Goals:** list and contribute from an account; contributions are transfers, and goal money counts in net.
- **Activity:** tap a row → detail sheet for amount, category, account, date, note; edit/delete with undo; the running net updates.
- **Motion:** one authored moment — pass reprint plus row light. Respect Remove animations with a crossfade or instant cut.
- **Responsiveness:** portrait phone first; hold at 320dp width and 1.3 font scale; don’t break at 600dp; system Back always works.

## 7. Constraints and open decisions

- **Persistence:** Room is required; the database is local-only. Version 4 drops the retired budget/allocation tables and preserves accounts, entries, goals, one-tap actions, and settings.
- **Widget:** a RemoteViews home-screen widget with the net position and four preset buttons; taps are handled by a manifest broadcast receiver that logs through the repository; no notification permission, no ongoing notification, and the FAB is the fallback.
- **Theme:** dynamic color disabled in favor of the fixed palette; dark is the primary composition, light is the day-terminal variant.
- **Barcode:** a functional snapshot reference — tapping copies a plain-text balance summary.
- **Cards:** account types plus last four; no card-network integration.
- **XP:** rules are still undecided, so no XP or levels appear on the finance home in this build. The pass’s DEBT-FREE / CAN SETTLE / SHORT status is the only progress signal; the shell leaves room for XP later.

### Currency / locale

**PHP (₱), en-PH, 12-hour time.**

## Next action

1. Verify the balance pass and activity board on the phone in dark and light at font scale 1.3.
2. Verify the widget prints the net position and money/owed line.
3. Run the finish review, refresh DESIGN.md/sidecar if needed, and ship the version 1.1.0 build.
