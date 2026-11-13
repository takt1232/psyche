# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

A single person: the app's owner. Psyche is built for personal use only, tuned to one individual's life, preferences, and data. No other audience, accounts, sharing, or multi-user use has been established.

## Product Purpose

Psyche is a personal "virtual me": one place that brings the owner's finances, job, hobbies, and personal tracking together, with game-like progress (XP and levels) layered over real life. Success means the owner uses it day to day as a trusted, personalized record and control surface for their life. The first feature is a Finance Tracker; other areas follow.

## Positioning

A single-owner life hub that combines practical trackers with RPG-style progression. It is intentionally not a general-purpose journal, notes app, or multi-user finance product: its value comes from being built around one person's actual life and from connecting money, work, hobbies, and progress in one place.

## Operating Context

Used by the owner on their Android phone as a personal, everyday tool. The first workflow is tracking personal finances: income and expenses, account balances, credit outstanding, savings goals, and cards such as ATM cards. The home surface answers one solvency question at a glance: **if every credit card were paid today, would any money be left?** Job-related DTR tracking is part of the wider vision but deferred. Hobbies and other life areas are future modules.

## Capabilities and Constraints

- Current build: Android app (Kotlin, Jetpack Compose, Material 3) with the Finance Tracker as its first module. It includes a live Balance Pass, activity ledger, active cash/debit/credit accounts, credit outstanding, savings goals, categories as expense labels, quick capture, and a home-screen widget with custom one-tap transactions.
- The Balance Pass shows a signed net position: active account money plus savings goals minus total credit-card outstanding. The same number can be positive or negative. Money and owed amounts sit beside it, with a debt-coverage indicator and per-account calculated balances.
- Credit payments are transfers: cash and outstanding fall together, leaving the net position unchanged. Card purchases lower net. Goal contributions are transfers and do not change net.
- Archived accounts and goals are excluded from totals but remain part of the record.
- Persistence is a local Room database on the phone. The app has no network permission, analytics, cloud backup, or account system.
- Confirmed product areas: Finance Tracker (current focus), jobs, hobbies, and a job-related DTR module (deferred). XP and levels are the confirmed gaming mechanic across the product.
- Finance Tracker scope: income and expenses; accounts and calculated balances; credit outstanding and limits; savings goals; cards such as ATM/debit/credit cards as part of accounts; categories as optional expense labels. There is no monthly cap, category allocation, or budget pass. The working defaults are PHP, en-PH, and 12-hour time.
- No multi-user, social, or sharing requirements are established.
- Undecided: XP rules and progression design, how the modules connect, and release plans.

## Brand Commitments

The product name is Psyche. No other brand commitments, voice rules, or assets have been confirmed.

## Evidence on Hand

No real financial data, user testimonials, case studies, or brand assets beyond the current app build and launcher icon. Future work must not fabricate financial figures, users, or proof.

## Product Principles

1. Build for one person. Every decision serves the owner's actual life; avoid generic multi-user product complexity.
2. Make the first module genuinely useful. The Finance Tracker must stand on its own before the wider "virtual me" expands.
3. Let progress serve the data. XP and levels should encourage consistent use without distorting financial truth.
4. Keep money legible and trustworthy. The net position, account balances, and credit outstanding must be accurate and easy to scan.
5. Grow in modules. Leave clean room for jobs, hobbies, and DTR without compromising the current feature.
