# Sendline (repo cut-tracker)

Personal Android app for one user. Offline, local-only, no accounts. The only network calls are the recipe import and the barcode lookup in Meals, each gated by Settings > Data > Online features (off by default). The ingredient search is offline, from a bundled USDA table (`app/src/main/res/raw/fdc_tsv.gz`, rebuilt with `python -I tools/build_food_table.py`). Package `com.relentnet.cuttracker`.

Read `CUT_TRACKER_HANDOFF.md` first. The prototype (`cut-tracker-prototype.html`, same as https://claude.ai/artifact/PorWqiakMS8xcDNxi5s3m9) is the source of truth for look, copy and behavior. Where this file, the handoff and the prototype differ, the prototype wins.

## Commands

- Build: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- UI smoke tests (emulator only, they reset the app's data): `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`. Disconnect the phone from adb first (`adb disconnect <ip:port>`).
- Install on a connected phone: `./gradlew :app:installDebug`
- Release: `./gradlew :app:assembleRelease` (needs `keystore.properties`, see README)

Builds run without a daemon on this machine (global `~/.gradle/gradle.properties`); they are slow and that is accepted.

## Rules

- No em dashes in any user-facing copy.
- No analytics, no crash reporting. INTERNET is used by `RecipeImport` and `OpenFoodFacts` alone; any new network use is its own switch under Online features and a line in `docs/privacy.html`.
- Never hardcode a color in a composable; use `CutTheme` tokens.
- Never `fallbackToDestructiveMigration`. Room migrations from v1 on.
- XP, chain and level are derived in `ProgramEngine` from Room rows, never stored.
- Unattended runs follow the protocol in the handoff and report in `PROGRESS.md`.

## Decisions

Where a job lives, so the next pass does not move it back.

- 2026-10-19: Today is the day, Beta is the plan, Route is the record. Workouts this week and the next checkpoint are read on Beta (This week, the Checkpoints tile); Route shows them only on the calendar. The Checkpoints page has no 12-week rail; the calendar and the Weight chart draw the flags.
- 2026-10-19: The route's numbers (start, goal, weeks, start date, protein, calories, workouts) are edited on Beta > Targets only. Settings keeps the app's chrome: Nudges, Appearance, Data, About.
- 2026-10-19: A checkpoint's date is its identity. Adding on an occupied date opens that flag; the store dedupes by date on load.
- 2026-10-19: Looks change stone and result colors only; type and layout never change with a look. Holiday decor sits on the top edge of holds and on the wall, never over the day number.
- 2026-10-19: A weigh-in is typed on Today's weigh-in row (or the widget) and nowhere else; Weight shows the average, the pace, the rate and the chart, and its footer points back to Today. The weigh-in reminder opens Today with that row open.
- 2026-10-19: On a script row that opens something (a meal picker, the lifts), the box ticks the row on its own and the row body opens it; the widget's rows tick the same way.
- 2026-10-19: What a kind of day asks is said in one sentence, `dayWhere` in BetaScreen.kt, used by Beta's day pages, Beta's day tiles (short form) and Today's day cards; the pace sentence is `paceWords` in WeightScreen.kt, used by Weight's hero and Today's weight tile.
- 2026-10-20: The pick-a-day reminder answers itself from the shade with the evening card's two buttons (Yep, Rest day / I climbed), only while the day is unset. The weigh-in reminder only opens Today's weigh-in row; it gets no inline reply, per the weigh-in decision above.
- 2026-10-20: A past day's result is what its ledger rows (send/min XP) recorded when it was written. Changing the plan, protein or a meal never re-scores history. Editing a past day can only raise its result (a late tick, a fixed weigh-in); it drops only when you untick something or change its kind. Meal number edits and deletes freeze past picks as one-off entries. On the first 1.3.0 start (and after restoring an older backup) `Repo.keepOldScoresOnce` gives any past day the old live scoring rated higher a 0 xp send/min row, so the upgrade never lowers a day or moves xp.
- 2026-10-20: Ticked rows stay in place on Today by default; Hide completed (Settings, Appearance) folds them into a Done list. The box on a ticked row unticks it; the weigh-in is fixed by tapping it.
- 2026-10-20: A meal slot starts each day as what it logged the last day it was ticked (set when the kind of day is picked, past days untouched). The meal picker leads with that pick; there is no Same as yesterday chip.
- 2026-10-20: Developer options unlock with seven taps on the version (Settings, About) and live in Settings as their own page. `Settings.devOptions` is this phone only: not in backups, kept through a restore. Preview onboarding runs `OnboardingScreen(preview = true)` from a fresh `Settings()` and must never write: finishing, Exit and back only leave.
- 2026-10-20: Every settings edit goes through `vm.updateSettings { s -> ... }` (atomic on the stored value); never save a copy a screen held.
- 2026-10-20: Climbing words live in `data/Vocab.kt` and nowhere else. `Settings.training` null is climbing; set, the app speaks plainly and the session item takes that word. Screens read `CutTheme.vocab`, the widget, reminders and engine read `settings.vocab`. A new user-facing climbing word gets a Vocab field, never an if on the mode in a screen. Normal mode swaps words only: the wall, hold shapes, rope, Route, Min day, chain, colors and layout stay. The store listing and site stay climbing-first.
