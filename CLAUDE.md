# Sendline (repo cut-tracker)

Personal Android app for one user. Offline, local-only, no accounts. The only network call is the recipe import in Meals, gated by Settings > Data > Online features (off by default). Package `com.relentnet.cuttracker`.

Read `CUT_TRACKER_HANDOFF.md` first. The prototype (`cut-tracker-prototype.html`, same as https://claude.ai/artifact/PorWqiakMS8xcDNxi5s3m9) is the source of truth for look, copy and behavior. Where this file, the handoff and the prototype differ, the prototype wins.

## Commands

- Build: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Install on a connected phone: `./gradlew :app:installDebug`
- Release: `./gradlew :app:assembleRelease` (needs `keystore.properties`, see README)

Builds run without a daemon on this machine (global `~/.gradle/gradle.properties`); they are slow and that is accepted.

## Rules

- No em dashes in any user-facing copy.
- No analytics, no crash reporting. INTERNET is used by `RecipeImport` alone; any new network use is its own switch under Online features and a line in `docs/privacy.html`.
- Never hardcode a color in a composable; use `CutTheme` tokens.
- Never `fallbackToDestructiveMigration`. Room migrations from v1 on.
- XP, chain and level are derived in `ProgramEngine` from Room rows, never stored.
- Unattended runs follow the protocol in the handoff and report in `PROGRESS.md`.
