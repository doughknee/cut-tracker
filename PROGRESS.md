# Cut Tracker build progress

Unattended run started 2026-10-14. Steps per `CUT_TRACKER_HANDOFF.md` build order.

- [x] 1. Project scaffold, theme, bottom nav, Settings route
- [x] 2. Room entities, DAOs, repository, ProgramSpec + DataStore, day-boundary logic + tests
- [x] 3. Today screen end to end
- [x] 4. XP engine, level, Send/Min, bonus roll, level-up takeover, chain/freeze tests
- [x] 5. Weight screen with Canvas chart
- [x] 6. Lift screen
- [x] 7. Route screen: holds, anchors, checkpoint sheet, CameraX, badges, weekly project
- [x] 8. Settings and sub-screens, export/import
- [x] 9. Notifications, boot re-registration
- [x] 10. Phase 2 switch
- [x] 11. Home screen widget, app icon
- [x] 12. Export with photos, Auto Backup, Room migration scaffold
- [x] 13. Release build, signed APK, README

## Environment decisions

- Toolchain copied from the sibling `wren` project, known to build on this machine: AGP 9.1.1, Kotlin 2.4.10, Gradle 9.5.0 wrapper, JDK 21 (Temurin), compileSdk 36.
- No Gradle daemon (global setting, low-RAM host). Every build is a cold JVM.
- Emulator: AVD `wren4` (android-36, google_apis, x86_64) exists for the step-13 pass.

## Step log

### Step 1: scaffold, theme, nav
- Built: Gradle KTS + version catalog (AGP 9.1.1, Kotlin 2.4.10, Compose BOM 2026.09.00, Navigation 2.10.2, Room 2.8.4 via KSP 2.3.11), `CutTheme` with dark and light token sets as `CutColors` plus matching M3 color schemes, Archivo variable font bundled (`res/font/archivo.ttf`, width axis used for the 90% headline face), stroke icons rebuilt from the prototype SVG paths, bottom nav with four tabs, Send bar placeholder, Settings route with working Theme pills, adaptive launcher icon (hold 7-gon with chain link).
- Decisions: compileSdk 37 (the Compose BOM refuses 36). No Material NavigationBar; the nav is custom to match the 76px prototype bar. Theme mode lives in memory until step 8 persists it.
- Deferred: nothing.

### Step 2: data layer and engine
- Built: Room entities (DayLog with `dinnerIndex`, WeighIn, LiftSet, ProgressPhoto, XpEvent, Badge, WeeklyProject), DAOs with Flows, `CutDb` v1 with exported schema and an empty migration list, `SettingsStore` (DataStore Preferences, lists stored as JSON), `ProgramSpec` defaults, `ProgramEngine` pure functions (program day, day number, script, totals, Min/Send, levels, bonus roll, 7-day average, pace, results/freezes/chain, lifts, photo cadence). 14 unit tests pass.
- Decisions: end date = start + weeks*7 (Jan 6), holds are days 1..84 (Oct 14 to Jan 5), Jan 6 is the final checkpoint "when reached". Checkpoint week label = whole weeks elapsed (Oct 28 = week 2) to match the prototype. Chain = total count of Send + Min + Freeze days (never resets; "paused" when yesterday was an unfrozen miss), which reproduces the prototype's 11 to 12 behavior. Free days count as Send. 7-day average falls back to the last 7 logged readings if the trailing window is empty. Dinner options stay fixed in ProgramSpec (the prototype's Meals editor does not edit them).
- Deferred: nothing.

### Step 3: Today screen
- Built: `Repo` (single write path, one combined `AppState` flow), `AppViewModel`, `Haptics` (Vibrator predefined effects, gated by the toggle), Today v3: header with phase gear, welcome-back card, XP strip (level badge, counting XP, bar, chain + freeze caption), pick cards / mode chip + "Chain safe" / 7pm card, script rows with "Up next", swipe-to-check (80dp threshold, 140dp travel, green underlay), inline weigh-in (prefill, -.2/+.2 chips, Enter logs), inline dinner pick, collapsed Done group, protein bar tile and calories tile (hidden on free days), Send bar above the nav on every tab, toast pill.
- Decisions: chain caption reads "secured today" / "paused · N freezes" / "N freezes". Mode "Change" clears the day's items (prototype); picking a mode from null keeps weigh-in and meal ids. Tapping a workout item checks it (v3 prototype) rather than navigating to Lift (v2 copy).
- Deferred: nothing.

### Step 4: XP engine
- Built: `syncDay` reconciles the XpEvent ledger with the DayLog after every write (item events, send + one stored bonus roll, min when not a send; unchecking deletes). Level-ups detected by comparing level before/after each write. Send takeover: 900ms reel then slam + heavy haptic, confetti (accent + success only, 70 frames from the nav upward), "Nice". Level-up takeover auto-dismisses at 1.5s or on tap and queues behind a Send. Chain/freeze/Min/Send unit tests landed in step 2.
- Decisions: the bonus is its own XpEvent (amount may be 0) so a Send never re-rolls; undoing the Send deletes it.
- Deferred: nothing.

### Step 5: Weight screen
- Built: 7-day average hero (46sp, counts), delta + pace copy (green / accent), target tile with "lb to go", Canvas chart (y from floor(target) to ceil(start) or wider if readings demand, three gridlines, dashed target line, accent 7-day-average line with end dot, daily dots only while pressed, start/end date labels), 64dp input with Log, four quick chips, Enter submits. Logging also checks "Weigh in" on Today via the repo.
- Decisions: the average line uses the trailing-average rule at each logged date.
- Deferred: nothing.

### Step 6: Lift screen
- Built: block header with Week won / Not yet pill (live from stepper values or saved sets this week), 90s rest timer card (accent while running, green at zero, triple haptic), exercise cards with Last line, weight stepper (hidden for bodyweight, label in its place), reps stepper, green values and border when beating last, Save session (one LiftSet per set, +20 session, +15 per beat, +50 first Week won per week).
- Decisions: a never-logged exercise prefills 0 lb × 10 reps and cannot "beat" anything.
- Deferred: per-set editing (non-goal).

### Step 7: Route, checkpoints, photos, badges, projects
- Built: Route header (Day N · pct, N sends), weekly project card (3-segment bar, +150 and badge copy), hold grid (week 12 at top, states Send/Min/Freeze/Missed/Today/Future, anchors on non-free checkpoint rows colored hit/current/upcoming), tap popover with date, result + XP and a detail line (mode, items, weight, lifts), badge shelf 4 across (13 badges: First Send, 5 chain, Came Back, 6 projects; unearned dashed with +). Checkpoints route: photo-due banner (first day, then every 14 days) with Shoot, thumbnail grid with long-press delete (inline confirm), timeline rows (hit with actual avg, current with accent ring and days-to-go, passed-not-hit shows the avg against the target, free days), "Start Phase 2" on the final checkpoint once reached, Phase 2 note. CameraX capture screen (permission flow, back camera, JPEG to filesDir/photos/YYYY-MM-DD.jpg, +40 xp once per day).
- Decisions: holds open on tap (prototype) not long-press. Photo deletion confirms inline instead of a dialog. A passed checkpoint that was not hit stays a muted dot and shows "avg · aimed target".
- Deferred: nothing.

### Step 8: Settings, editors, export/import, onboarding
- Built: Settings (Appearance live; Program, Targets and Nudges apply on Save; platform DatePickerDialog / TimePickerDialog; length re-derives the end date; Phase pills switch the calories field; Data section with Export JSON via FileProvider share sheet, Import JSON via the document picker, Reset program as a 3-tap double confirm on the row), Meals / Exercises / Checkpoints editors (inline rows, × to remove, Add, Save applies all), `Backup` DTOs with `buildBackup` / `restoreBackup`, four-step onboarding with the final copy (blocked Next with accent outline when start weight is empty, live pace hint, live end-date hint), landing on Today with "Day 1. Pick a day." and the first weigh-in logged. The nav waits for DataStore before picking the start destination and returns to onboarding after a reset.
- Decisions: Theme and haptics apply live (the prototype switches theme instantly); everything else on Save. "Prototype: show first launch / 7pm state" rows are prototype-only demo rows and are not in the app. Reorder-by-drag is out (not in the prototype either).
- Deferred: Export with photos (zip) is step 12.

### Step 9: Notifications
- Built: `Reminders` (channel, two daily alarms via AlarmManager: exact when allowed, otherwise `setAndAllowWhileIdle`; "Weigh in" opens the Weight tab, "Climb or off?" posts only when today's mode is still null and opens Today), `AlarmReceiver` reschedules the next day, `BootReceiver` re-registers on BOOT_COMPLETED and MY_PACKAGE_REPLACED, App reschedules whenever the nudge times change, POST_NOTIFICATIONS asked once after onboarding, notification tab deep link through the view model.
- Decisions: inexact AlarmManager instead of WorkManager when exact alarms are denied (same behavior, no new dependency). The notification body for 7pm is the card copy.
- Deferred: nothing.

### Step 10: Phase 2
- Built: already wired through Settings: header "Phase 2 Chest", calories 2700, Phase 2 lift blocks (dips 4, push-ups 2; floor press 4, incline DB press 3, pullover 2, ab roller 1) via `setsPhase2` / `phase2Only`, Checkpoints note "Phase 2, no fixed end", "Start Phase 2" on the final checkpoint. Added `Phase2Test` (2 tests).
- Decisions: Phase 2 keeps the same Today script; only the Lift blocks and calories change (the handoff lists nothing else).
- Deferred: nothing.

### Step 11: Widget and icon
- Built: Glance 4x2 widget (mode label / "Sent", Up next card that checks the item via an ActionCallback with the same haptic, 7-segment Send bar that goes green on a Send, chain count with link icon and CHAIN SAFE), Glance 2x1 widget (chain + bar), day/night ColorProviders from the two token sets, widgets re-render on every state change (debounced 300ms) which covers DayLog edits and the rollover, previews for the picker. Adaptive icon landed in step 1 (ground background, accent 7-gon hold with the chain link in ground, monochrome 7-gon); SVG source in res/raw.
- Decisions: Glance cannot load Archivo (RemoteViews), so the widget uses the system font. Mipmaps are vector-only (minSdk 26 supports adaptive icons everywhere), so no raster generation was needed.
- Deferred: nothing.

### Step 12: Export with photos, Auto Backup, migrations
- Built: "Export with photos" zips backup.json plus the photos folder; Import accepts JSON or zip (zip photos restored into filesDir/photos before rows). `fullBackupContent` + `dataExtractionRules` include cut.db and datastore/, exclude photos/. Migration scaffold documented in Db.kt with the exported schema v1 under app/schemas as the reference; `Migrations.all` is passed to the builder and destructive fallback is never used.
- Decisions: nothing beyond the handoff.
- Deferred: nothing.

### Step 13: Release build
- Built: R8 + resource shrinking release, signed with a new sideload key (`cut-tracker.jks` at the repo root, `keystore.properties` beside it; both gitignored, the password is only in keystore.properties). `apksigner verify` passes. README has build, release and sideload instructions.
- Decisions: the key is a self-signed personal key with 10000-day validity; losing it means uninstall before the next install (README says so).
- Deferred: nothing. Emulator pass follows.

## Emulator pass

AVD `wren4` (android-36, x86_64) booted headless in 25s. `adb root` worked, so the clock was moved with `date` to test rollover, freezes and re-entry. Emulator set to dark (prototype default); one light-theme shot taken. Every tap coordinate is in `screenshots/taps.log`.

Screenshots (`screenshots/`): onboarding-1..4, today-pick, today-climb, today-breakfast, today-dinner-options, send-takeover-spin, send-takeover, today-sent, weight, weight-day2, lift, lift-off-day, lift-won, route, route-freeze, route-popover, route-paused, route-10-weeks, checkpoints, settings, settings-bottom, today-weigh-inline, today-min, today-welcome-back, today-light.

Round 1 fix: Route header percent used (day - 1); the prototype uses day / total (Day 18 = 21%). Fixed, rebuilt, re-shot. No other visible differences in spacing, type, color or copy found; round 2 not needed.

## DONE

- APK: `app/build/outputs/apk/release/app-release.apk` (3.5 MB, R8, signed with `cut-tracker.jks`, `apksigner verify` passes). Debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Gate: `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` (16 tests) pass at the final commit.

Acceptance checks passed on the emulator:
- Fresh install on Oct 14: Day 1 of 84, "Pick a day", V0, 0 xp, chain 0, 2 freezes.
- Checking breakfast: Protein 48 / 165g, +10 green, XP counted up, bar animated (XP read 20 because onboarding had already logged the weigh-in, per spec).
- Logging 181.8 then 181.2 (day 2): avg 181.5, "on pace".
- Every Climb-day item: confetti, +50, bonus roll (rolled 0), hold green on Route, First Send badge.
- Weigh in + protein >= 165 with the workout unchecked: Min day, dim green hold, chain 2, "Chain safe", "secured today".
- Skipping Oct 16: blue freeze hold, chain unbroken (3), freezes read 1.
- Skipping Oct 17 to 20: Oct 17 frozen (second), 18 to 20 grey, chain "paused at 4", welcome-back card on next open, Came Back badge earned and lit on the shelf.
- Floor press 55 x 10 after 50 x 10: green value and border, +15, "Week won" (+50 first time), XP 235 to 320.
- Settings length 10 weeks: Day 8 of 70, 70 holds, end date Dec 23, send and logs kept.
- Rollover: at 04:30 after a clock move the day advanced to Day 2 / Day 4 / Day 8 as expected (03:59 vs 04:00 covered by unit test).
- Notifications: both receivers fired by `am broadcast` posted "Weigh in" and "Climb or off?"; exact alarms scheduled for 08:30 and 19:00.

Not run on the emulator:
- Reaching 1200 xp for the V3 takeover (thresholds unit-tested; V1 level-up fired during the pass but was not captured). The level-up takeover shares the Send takeover code.
- Airplane mode (the app has no INTERNET permission, so nothing can change).
- Home screen widget placement (needs launcher interaction), camera capture (no real camera; permission flow and save path exercised only by code review), export share sheet and import picker (need other apps).

Differences from the prototype, and why:
- Theme and haptics apply live; everything else in Settings applies on Save (handoff asked for Save; the prototype switches theme instantly, so appearance stays live).
- The two "Prototype:" demo rows in Settings are not in the app.
- Widget text is the system font (Glance cannot load Archivo).
- Inexact AlarmManager instead of WorkManager when exact alarms are denied (same behavior, no extra dependency).
- Tapping a workout item checks it (v3 prototype) instead of opening Lift (older v2 copy in the handoff).
- Chain is a count of Send + Min + Freeze days that never resets; "paused" shows when the last completed day was an unfrozen miss (matches the prototype's 11 to 12 behavior).

## Overnight pass, 2026-10-20 (DONI-536)

Unattended, at the user's request: audit everything and fix what a daily user feels. Method: a walk of every screen on the emulator with a seeded sample week, three read-only code audits in parallel (data and engine, Today/Route/Weight, Beta/Settings), fixes in committed rounds that each passed `assembleDebug` and `testDebugUnitTest`, a large-text pass (font scale 1.3 on a 360dp screen), and a fresh-install pass of the signed release on the emulator. Commits `4e9495b` onward, ending with round 8.

- Round 1, data: settings edits are atomic (quick edits no longer undo each other); past days keep the result their ledger recorded, so plan, protein or meal changes never re-score history; meal edits and deletes freeze past picks; "Change" keeps ticks; a Rest-day block counts as a workout; restore is parse-first and one transaction under the write lock; backups carry the new settings; per-reminder switches; the weigh-in reminder skips once weighed; alarms survive time zone and clock changes; kinds are unique ignoring case and a lift-kind rename onto an existing kind merges instead of crashing; the launch intent is not replayed on recreate; keyboard Next and Done work; switches have state for TalkBack.
- Round 2, Today and shared UI: back closes open panels, popovers, takeovers and onboarding steps before leaving; swipes tick the day on screen; a done weigh-in opens to fix a typo; lift drafts survive; steppers repeat while held and can be typed; numbers parse with commas; focused fields scroll above the keyboard; Undo after deletes; import and reset ask first; export cannot crash; photos decode off the main thread.
- Round 3, libraries: word-order and accent-insensitive search, search in the lift and meal pickers, a discard question on sheets with unsaved edits, room under floating add buttons.
- Round 4, access: large text wraps instead of squeezing (Today totals, Route header, Beta grid scrolls); 48dp targets; TalkBack names and states; FAB shadow from tokens.
- Round 5, Settings: last export date shown, Nudges in the phone's clock format, a Data icon, two-line row descriptions.
- Round 6: the pick-a-day reminder answers itself from the shade (Yep, Rest day / I climbed). The weigh-in reminder deliberately gets no inline reply (decision log).
- Round 7, release check: fixed an onboarding crash on a comma-typed start weight and typed stepper values appending to the old number; onboarding copy points targets at Beta.
- Round 8, an independent review of the whole diff found seven regressions, all fixed: editing a past day re-judged it by today's targets (now it can only rise unless you untick or change its kind); steppers fired on touch-down so a scroll could change a value; Save session missed a number still being typed; a meal rename froze past picks; Undo restored the whole settings snapshot; day plan switches used screen state; a stale pick-a-day button could set the next day. Number fields now select their contents on focus.
- Round 9: Save a copy (backup straight to a picked file), Open settings for a denied camera, a scrolling rules sheet, "No day picked." in the Route popover, 48dp targets for Today's small controls.
- Second wave (after 3 am, the user asked to keep going all night): two more audits on areas the first wave did not cover (widget, Route and Weight edges, onboarding, meal sheet, scanner, looks), the widget placed and driven on the release build, store shots retaken on 1.3.0, a 404 page and sitemap for the site.
- Round 10: meal sheet input survives process death (verified with am kill), honest totals, plain network errors, the scanner releases the camera, label reader timing, look-code twins, sheet guards on the chalk and day-plan sheets.
- Round 11: Targets moves default flags (FollowFlagsTest), the widget's quiet card and stale-tap guard, pace clamped to the route, second-route chart and photos, future-start copy, checkpoint add logic, onboarding guards, state derived off the main thread and shared.
- Round 12, a review of rounds 10 and 11 found nine issues, all fixed: a recipe link refetched after a restart and overwrote edits, a protein-only recipe left stale calories, the chart's daily dots showed last route, moved flags could land on a skip, edited flag notes were overwritten, a 0 g serving made NaN, the camera could bind after the scanner closed, the popover could read more ticks than items, the widget said "1 days".
- UI tests (DONI-468, done): seven Compose instrumented tests on the emulator, covering back closing the meal picker, Undo after a delete, typing a target, the discard question, a whole day to a Send, a frozen day filled in late, a backup round trip. Run with the phone disconnected (CLAUDE.md, Commands).
- Round 13, a copy audit of every user-facing string: one word for one thing (No day picked, Freeze, Planned skip, checkpoint, Delete or Take out, tick, item, xp, goal, Ahead, Wipe), honest late-pick copy, plurals, "to a Min day".
- Restore a backup from the first onboarding screen of a fresh install (DONI-466, tested on the emulator end to end, left In Review for the phone).
- Round 14 (fixes from the review of rounds 12 and 13) and the final whole-night review: no high-severity issues; its one edge (1.2.0 scored history with the current settings, so an upgrade could lower a day) is closed by a one-time step that keeps whatever 1.2.0 showed, at 0 xp, tested on the emulator.
- A one-time "New in 1.3.0" note on the first open after the update (UI test: once, then never again), the Route and Checkpoints open state kept through a restart, the untick decision moved inside the write lock.
- Back up every week to a folder picked with the system picker (no network), tested through the real picker with the clock moved eight days; the privacy policy says where the copy goes.
- Light-mode pass of eight screens (one legend fix), and a day-one walk as a new user (no issues found).
- Round 15, mornings (a fresh-eyes audit of the first ten minutes of a day): weigh-in row before a pick, ticks that settle in place, an Off day answer on the late card, a Min toast, the widget's weight opening the field, an Add widget row, lift save ticking inside the write, a message for an unreadable weight, this week not counting the morning as a miss. The release check on the emulator caught the widget's weight landing on whatever page was open (fixed: the flag always goes to Today) and a silent Next on onboarding's empty start weight (now says what is missing). 60 unit and 9 UI tests pass; installed on the Pixel at 04:52, not opened.

Tests added: `HistoryTest` (ledger-scored history, workouts), `InputParsingTest` (numbers and search), BackupTest covers the new settings fields.

Verified on the emulator: back behaviour, weigh-in fix, typed targets, Undo, the discard question, the reminder buttons, large-text layouts, release onboarding and smoke pass with no FATAL in logcat. Installed 1.3.0 (versionCode 4) on the Pixel over 1.2.0 with `install -r`; data kept; the app was not opened there overnight.

Verified later in the night: the widget on the release build (placed with a launcher drag, day pick and item tick from the widget), Save a copy and restore through the system file picker. Still not verified: camera and label reading (no real camera), TalkBack by ear.

Left as is, on purpose: future-day numbers on Route stay dim (below 4.5:1 contrast) because the look depends on it; the weigh-in reminder has no inline reply per the 2026-10-19 decision.
