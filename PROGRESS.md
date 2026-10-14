# Cut Tracker build progress

Unattended run started 2026-10-14. Steps per `CUT_TRACKER_HANDOFF.md` build order.

- [ ] 1. Project scaffold, theme, bottom nav, Settings route
- [ ] 2. Room entities, DAOs, repository, ProgramSpec + DataStore, day-boundary logic + tests
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
