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
