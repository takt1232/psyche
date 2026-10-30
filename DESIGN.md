---
name: Psyche Finance Pass
description: A monthly budget boarding pass over a live gate board, issued locally on one Android phone.
colors:
  night-ground: "#0B0D10"
  night-row: "#15191E"
  night-line: "#2A2F35"
  night-panel: "#1C2127"
  pass-paper: "#FFFFFF"
  pass-muted: "#5C636B"
  pass-line: "#DDE0E3"
  day-ground: "#E6E8EB"
  day-panel: "#F4F5F6"
  day-line: "#D5D8DC"
  ink: "#0B0D10"
  steel: "#6B7178"
  steel-quiet: "#9BA1A8"
  nav-night-inactive: "#8A9199"
  alert: "#FFD400"
  alert-ink: "#7A5B00"
  danger-night: "#FFB4AB"
  danger-day: "#B3261E"
typography:
  display:
    fontFamily: "Barlow Condensed, sans-serif"
    fontSize: "56px"
    fontWeight: 700
    lineHeight: "56px"
    letterSpacing: "-1.2px"
  headline:
    fontFamily: "Public Sans, sans-serif"
    fontSize: "28px"
    fontWeight: 600
    lineHeight: "34px"
  title:
    fontFamily: "Public Sans, sans-serif"
    fontSize: "19px"
    fontWeight: 600
    lineHeight: "25px"
  body:
    fontFamily: "Public Sans, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  label:
    fontFamily: "Roboto Mono, monospace"
    fontSize: "10px"
    fontWeight: 500
    lineHeight: "14px"
    letterSpacing: "0.8px"
  pass-amount:
    fontFamily: "Barlow Condensed, sans-serif"
    fontSize: "58px"
    fontWeight: 700
    lineHeight: "58px"
    letterSpacing: "-1.2px"
  brand:
    fontFamily: "Barlow Condensed, sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: "24px"
    letterSpacing: "1.7px"
  mono-micro:
    fontFamily: "Roboto Mono, monospace"
    fontSize: "7px"
    fontWeight: 400
    lineHeight: "10px"
    letterSpacing: "0.7px"
rounded:
  stamp: "2px"
  field: "4px"
  control: "8px"
  button: "10px"
  card: "12px"
  panel: "14px"
  pass: "16px"
  sheet: "22px"
  pill: "999px"
spacing:
  micro: "4px"
  tight: "8px"
  base: "12px"
  roomy: "16px"
  block: "22px"
  section: "26px"
components:
  button-primary:
    backgroundColor: "{colors.alert}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.button}"
    padding: "0px 24px"
    height: "52px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.pass-paper}"
    typography: "{typography.label}"
    rounded: "{rounded.button}"
    padding: "0px 20px"
    height: "48px"
  pass-card:
    backgroundColor: "{colors.pass-paper}"
    textColor: "{colors.ink}"
    typography: "{typography.pass-amount}"
    rounded: "{rounded.pass}"
    padding: "16px"
  field:
    backgroundColor: "transparent"
    textColor: "{colors.pass-paper}"
    typography: "{typography.body}"
    rounded: "{rounded.field}"
    padding: "16px"
  stamp:
    backgroundColor: "transparent"
    textColor: "{colors.steel}"
    typography: "{typography.mono-micro}"
    rounded: "{rounded.stamp}"
    padding: "4px 6px"
  navigation:
    backgroundColor: "{colors.night-ground}"
    textColor: "{colors.nav-night-inactive}"
    typography: "{typography.mono-micro}"
    rounded: "0px"
    height: "66px"
  board-row:
    backgroundColor: "{colors.night-row}"
    textColor: "{colors.pass-paper}"
    typography: "{typography.body}"
    rounded: "0px"
    padding: "7px 0px"
  fab:
    backgroundColor: "{colors.alert}"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    size: "60px"
  feedback-banner:
    backgroundColor: "{colors.night-panel}"
    textColor: "{colors.pass-paper}"
    typography: "{typography.body}"
    rounded: "{rounded.card}"
    padding: "10px 12px"
---

# Design System: Psyche Finance Pass

## Overview

**Creative North Star: "The Departure Board"**

The Finance Tracker behaves like a small private terminal: a white boarding pass states what is left this month, and a quiet gate board records what just happened. The pass is the one document lifted off the ground; the board is the working record beneath it. The app should feel issued rather than marketed: precise, local, and slightly mechanical, with the warmth of a personal object.

Dark is the native condition — an ink terminal at night. Light is not an inversion but the same terminal during the day: cool paper ground, white pass, darker amber for text. System theme is the default; Settings can pin Light or Dark without changing the palette. Geometry, type, and hierarchy stay fixed; only the ground/pass relationship changes. Text size follows Android rather than an app-only scale. Material 3 governs navigation, sheets, fields, switches, and feedback so the custom pass/board composition never becomes a novelty control set.

The interface is dense but never cramped. Every label is short enough to scan from the notification shade, every amount is tabular, and every state has a plain-language name. No dashboard cards, no gradient hero, no invented financial data.

**Key Characteristics:**
- One white pass over a flat board; the pass is the only surface allowed a resting shadow.
- Amber is attention, not decoration: lit row, FAB, selected controls, and the notification dot.
- Condensed display numerals, plain body text, and mono labels in a strict hierarchy.
- Hairline rules, perforations, notches, and barcode bars instead of ornamental card stacks.
- Dark-first terminal; daytime paper variant with the same ticket geometry.
- Native Material 3 behavior, edge-to-edge insets, and a 320dp-first compact composition.

### Named Rules

**The One Lifted Surface Rule.** The pass may lift; the board stays flat. Sheets and the capture overlay may lift because they interrupt the surface, not because every container needs a shadow.

**The Amber Means Attention Rule.** Alert yellow is reserved for the lit new row, the FAB, selected navigation and controls, progress fills, and the pass's notification dot. It is never body copy or a large decorative field.

**The Honest Numbers Rule.** Never prefill a balance, cap, or category amount. Empty states say what to do; they do not demonstrate with fake money.

## Colors

The palette is a two-condition terminal: ink and paper, with one signal yellow and a small set of danger/muted neutrals. It is intentionally fixed; dynamic color is off.

### Primary
- **Alert Amber** (`alert`): the signal color. Used for the FAB, selected filter chips, active navigation, row highlight, progress fills, and the pass's small notification dot. On light grounds, use the darker amber text token (`alert-ink`) for words and icons rather than the bright fill color.
- **Signal Ink** (`ink`): the foreground on the pass and on amber. Also the light-theme ground text. It is near-black, not pure black, so the white pass has a slight material contrast.

### Secondary
- **Card Danger** (`danger-night`, `danger-day`): over-budget amounts and destructive warnings. It is a separate semantic channel from amber; never use red for a merely tight budget.

### Neutral
- **Night Ground** (`night-ground`): the default app background and navigation container.
- **Night Row** (`night-row`): board rows, caption containers, and inset panels on dark ground.
- **Night Hairline** (`night-line`): dividers and quiet borders on dark.
- **Night Panel** (`night-panel`): raised input/switch panels that need separation from the ground without a shadow.
- **Pass Paper** (`pass-paper`): the pass, entry detail, and the inside of secondary confirmation surfaces.
- **Pass Muted** (`pass-muted`): secondary labels printed on paper.
- **Pass Hairline** (`pass-line`): perforation and field rules on the pass.
- **Day Ground** (`day-ground`): the light-theme paper-board ground.
- **Day Panel** (`day-panel`): light-theme inset panels and the top feedback banner.
- **Day Hairline** (`day-line`): light-theme dividers and quiet outlines.
- **Steel** (`steel`): secondary body text and inactive light-theme navigation.
- **Steel Quiet** (`steel-quiet`): tertiary labels on dark; `steel` takes that role on light.
- **Night Nav Inactive** (`nav-night-inactive`): unselected navigation icons/labels on the ink bar.

### Named Rules

**The Signal Rarity Rule.** Amber may own the FAB and the lit row, but it should not become a page background or a body-text color. Its power comes from being scarce and momentary.

**The Paper Inversion Rule.** On light grounds, text/icons that were amber become `alert-ink`; amber remains a fill for shapes. Do not put bright amber words on white.

## Typography

**Display Font:** Barlow Condensed (bundled) with a condensed grotesque fallback  
**Body Font:** Public Sans (bundled) with a system sans fallback  
**Label/Mono Font:** Roboto Mono (bundled) with a monospace fallback

**Character:** A travel-document voice: condensed numerals carry the emotional weight, Public Sans keeps instructions human, and Roboto Mono makes the ledger feel measured. The mix is utilitarian, not retro for its own sake.

### Hierarchy
- **Display** (`display`, 56px/56, bold, −1.2px): the pass amount and receipt-like totals. Auto-size down to keep a single line at 320dp.
- **Headline** (`headline`, 28px/34, semibold): screen and sheet titles that need a human voice.
- **Title** (`title`, 19px/25, semibold): row names, section titles, empty-state headings.
- **Body** (`body`, 14px/20, regular): instructions, notes, settings copy, detail values. Keep measures short on phone.
- **Label** (`label`, 10px/14, medium, 0.8px): mono labels, table headers, button text, stamps. Uppercase for table/status labels; sentence case for prose labels.
- **Pass amount** (`pass-amount`, 58px/58, bold): the single remaining/over amount. It is the largest thing in the app by intention.
- **Brand** (`brand`, 22px/24, bold, 1.7px): the PSYCHE wordmark only.
- **Mono micro** (`mono-micro`, 7px/10, regular, 0.7px): ticket labels, metadata, stamps, barcode references. Minimum practical size; do not go smaller.

### Named Rules

**The Tabular Money Rule.** Every amount column, time column, running balance, and cap uses tabular figures. Money is compared by digit position, never by proportional rhythm.

**The One Display Rule.** Only one display-sized number may appear in a viewport. The pass amount wins; entry detail may use a smaller pass amount, but not a second competing hero.

## Layout

The phone layout is a single column with 16dp outer gutters. The pass occupies the full content width and stacks above the board; the FAB and bottom navigation sit above the system inset. The home list uses 4dp top and 108dp bottom content padding so the last row clears the FAB and bar.

Use a 4dp micro rhythm, 8dp within tight groups, 12–16dp between groups, and 22–26dp before a new section. Tables use a hairline under each row; do not wrap rows in separate cards. The board is compact below 315dp of content width, tightening time/amount/left columns and truncating descriptions before they can collide.

At 320dp and 1.3 font scale, preserve the pass first and the board second: the pass amount auto-sizes, labels ellipsize, and horizontal chip rows scroll rather than wrap. Sheets are bottom-anchored, inset-aware, and scroll internally; capture keeps the amount pad and Save reachable above the IME.

Responsive behavior:
- **Phone portrait (320–599dp):** bottom navigation, full-width pass, single-column board, sheets.
- **Expanded width (600dp+):** content remains centered as a readable column; the pass does not stretch into a billboard. Navigation remains the compact phone bar for this single-owner phone product.
- **Landscape/IME:** preserve window insets, scroll the sheet body, and never let the primary action disappear under the keyboard.

## Elevation & Depth

This is a flat, tonal system with exactly one lifted document. The pass uses a soft 18dp shadow on night and a 2dp shadow plus hairline on paper; the board and list rows are flat and separate with color and hairlines. Sheets use Material's surface elevation because they are transient layers. FAB elevation is the standard Material lift; nothing else gets a custom drop shadow.

Depth is communicated by layering, not by a shadow vocabulary: ground → board row → inset panel → white pass. The lit row uses amber fill as a state change, not elevation. If a surface needs to feel important, raise its contrast, not its blur.

## Shapes

The form language comes from tickets and printed tables: 16dp clipped corners on the pass, 22dp top corners on sheets, 2dp stamps, hairline rules, and pill progress tracks. Controls use 8–10dp corners so they read as Material rather than paper cutouts. Pills are reserved for progress tracks, tags, and the FAB; cards stay subtly rounded, never floating blobs.

Barcode bars are a functional reference, not a decorative pattern: each bar is derived from the period text and copies the summary when tapped. Perforation lines, notches, and top strips use the same 1dp hairline weight as table rules.

## Components

### Buttons
- **Shape:** 10dp radius, minimum 48–52dp height.
- **Primary:** amber fill with ink text, mono label, 24dp horizontal padding. Used for Save, Create pass, Set cap.
- **Secondary:** transparent fill with a 1dp outline and on-ground text, 48dp minimum. Used for Cancel, Delete, transfers, and settings sub-actions.
- **Quiet / Text:** mono or body text with a 48dp tap region; no filled background. Used for MANAGE, FULL ACTIVITY, and dismissive actions.
- **Focus / pressed:** Material state layers; the control keeps its shape and text weight. Disabled controls drop to panel-line fill and faint text.

### Chips
- **Style:** pill shape, 1dp outline, mono label. Selected fills amber with ink text; unselected stays transparent with a hairline border.
- **State:** selected = filter/type/category choice; horizontal scroll for long lists. Never wrap chip rows on phone.

### Cards / Containers
- **Corner Style:** pass 16dp; sheets 22dp top; cards/panels 12–14dp; board is square.
- **Background:** pass paper, night row, night panel, or day panel according to layer. No card grid.
- **Border:** hairlines at 1dp; the pass gets a border only in light mode.
- **Internal Padding:** 12–16dp; board rows use vertical padding and full-width rules.

### Inputs / Fields
- **Style:** Material 3 outlined text field, 4dp default radius, transparent container, 1dp outline.
- **Focus:** amber-tinted cursor/label on dark; ink focus on light. Keep field labels in body-small Public Sans.
- **Note fields:** the mono label sits above the field with a live `0/80` counter at the end of the same row; the counter turns `accentText` near the limit.
- **Error / Disabled:** danger outline and a mono error line; disabled text uses faint. Amount fields use grouped digits and tabular figures.

### Navigation
- **Style:** bottom bar, 66dp content height plus the navigation inset, four destinations. Icons are 21dp, labels are 8px mono uppercase with 1.1px tracking. Selected icon/label uses signal amber on dark and `alert-ink` on light; unselected uses the muted nav token. The FAB is a separate 60dp amber circle above the bar.
- **Behavior:** System Back from a non-Home tab returns Home; back from Home exits. Sheets and the capture overlay own their back dismissal. Predictive Back is enabled at the activity level.

### Signature Components
- **Budget Pass:** a white ticket with a top strip (BUDGET PASS + sequence), month/days header, auto-sized remaining amount, “LEFT/OVER BY — OF CAP — % LEFT” side stack, a three-segment week/daily/status row, and a functional barcode footer with notification state. The empty version says “SET YOUR CAP” without numbers.
- **Live Activity Board:** four-column table (TIME / DESCRIPTION / AMOUNT / LEFT), newest row amber with a NOW chip, older rows on the night row. The board is the record; it never becomes a stack of cards.
- **Stamp:** a 2dp-corner mono tag used for FIRST RUN, DEFAULT, TIGHT, OVER, category labels, and flow state. Outlined by default; filled only when the state is the amber attention state.
- **Top Feedback Banner:** a floating notification-like strip at the top of the app, with a slide/fade entrance, amber check tile, mono NOTICE label, message, and optional text action. It appears above app content and carries save, permission, copy, and undo confirmations. It is distinct from Android's ongoing pass notification.

## Do's and Don'ts

### Do:
- **Do** keep the pass as the only lifted document; let the board sit directly on the ground.
- **Do** use `alert-ink` for amber-like text/icons on light grounds and bright `alert` only as a fill.
- **Do** preserve tabular figures, uppercase mono table labels, and 1dp hairlines.
- **Do** show “SET YOUR CAP” with no fabricated values until the owner sets a cap.
- **Do** auto-size the pass amount and allow long board descriptions to ellipsize.
- **Do** keep every primary action at 48dp or more and every sheet scrollable above the IME.
- **Do** describe over-budget as a positive “over by” amount in tables and notifications; keep the negative sign only where the ledger's running left truly goes below zero.

### Don't:
- **Don't** turn rows or feature areas into nested cards or icon+heading+text tiles.
- **Don't** use amber for body copy, large backgrounds, or decoration unrelated to a live state.
- **Don't** put bright amber text on white or rely on color alone to explain tight/over.
- **Don't** add a second hero number, gradient text, glass/blur decoration, or arbitrary drop shadows.
- **Don't** prefill balances, caps, categories, or activity to make the empty state look busy.
- **Don't** invent XP, levels, or gaming chrome inside the finance surface; that mechanic is not designed yet.
