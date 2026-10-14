# Cut Tracker build progress

Unattended run started 2026-10-14. Steps per `CUT_TRACKER_HANDOFF.md` build order.

- [x] 1. Project scaffold, theme, bottom nav, Settings route
- [x] 2. Room entities, DAOs, repository, ProgramSpec + DataStore, day-boundary logic + tests
- [ ] 3. Today screen end to end
- [ ] 4. XP engine, level, Send/Min, bonus roll, level-up takeover, chain/freeze tests
- [ ] 5. Weight screen with Canvas chart
- [ ] 6. Lift screen
- [ ] 7. Route screen: holds, anchors, checkpoint sheet, CameraX, badges, weekly project
- [ ] 8. Settings and sub-screens, export/import
- [ ] 9. Notifications, boot re-registration
- [ ] 10. Phase 2 switch
- [ ] 11. Home screen widget, app icon
- [ ] 12. Export with photos, Auto Backup, Room migration scaffold
- [ ] 13. Release build, signed APK, README

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
