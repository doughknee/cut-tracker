# Cut Tracker: Claude Code handoff

Personal Android app for one user (Doni). Offline, local-only, no accounts, no network. It runs a fixed 12-week cut program (Oct 14 2026 to Jan 6 2027) and then a chest-build phase.

**The source of truth for look and behavior is the interactive prototype: https://claude.ai/artifact/PorWqiakMS8xcDNxi5s3m9.** Click through every tab, the gear, a checkpoint anchor, check every item on a Climb day to see the Send takeover and bonus roll, and flip Theme in Settings. Match timing, copy and states exactly. The static canvas at https://claude.ai/artifact/1S1ezfp5CQHrGX5SAPjswK is older; the prototype wins where they differ.

## Stack

- Kotlin, Jetpack Compose, Material 3, single Activity, Navigation Compose
- Room for storage, DataStore (Preferences) for settings
- minSdk 26, targetSdk latest stable, Gradle Kotlin DSL, version catalog
- CameraX for the progress photo. Photos stay in app-private storage (`filesDir/photos`). No READ_MEDIA permissions
- No analytics, no crash reporting, no internet permission in the manifest
- Package: `com.relentnet.cuttracker`

## Visual spec (from the design)

- Ground `#0C0D0E`, surface `#15171A`, hairline `#23252A`, muted border `#3A3D44`
- Text `#F2EEE6`, secondary text `#8A8780`, dim `#5A5D64`
- Accent `#FF5A1F` (primary action, current state), success `#C8F55A` (done, progressed, on pace)
- Font: Archivo (bundle woff2 or ttf from Google Fonts as a Compose `FontFamily`). Display numerals weight 800, letter-spacing -0.02em, tabular figures. Section labels 12sp, weight 600, letter-spacing 0.12em, uppercase
- Corner radius 10 to 12dp on cards and buttons. Touch targets 44dp minimum, most are 48 to 56dp
- Two themes, System / Dark / Light in Settings (default System). Light tokens: ground `#F3F1EC`, surface `#FFFFFF`, hairline `#E3E0D8`, muted border `#C9C5BB`, text `#141413`, secondary `#6B685F`, dim `#A09C92`, accent `#E8450A` with white on-accent text, success `#4F8A0E`, success-dim `#C9DDB0`, freeze `#2F7FD6`, missed `#D8D4CB`, future `#E9E6DF`. Build with Material 3 color scheme objects, one per theme, and never a hardcoded color in a composable
- Bottom nav, 4 tabs: Today, Weight, Lift, Route. Icons are 2px stroke outlines, active tab in accent. Settings is a full-screen route from the gear on Today. Checkpoints is a full-screen route from a Route anchor

## Program constants (hardcode in `ProgramSpec.kt`, make them editable later if needed)

```
PHASE_1_START = 2026-10-14
PHASE_1_END   = 2027-01-06   (84 days, 12 weeks)
START_WEIGHT  = 181.8 lb
TARGET_WEIGHT = 170.0 lb
CALORIES_P1   = 2250
CALORIES_P2   = 2700
PROTEIN_G     = 165
DAY_RESET_HOUR = 4   (a "day" runs 04:00 to 03:59 local)
PHOTO_INTERVAL_DAYS = 14
```

Meals (protein g, calories):
- Breakfast 9:00: Greek yogurt + whey + banana (48, 430)
- Lunch 1:00: chicken or turkey + rice + veg (55, 620)
- Snack 4:00: string cheese + apple or shake (20, 250)
- Dinner 7:30: one of three rotation picks (45, 950 max)

Exercises:
| id | name | where | sets | rep target | step | unit |
|---|---|---|---|---|---|---|
| dips | Dips | climb day, gym | 3 | 6 to 10 | 5 lb | lb |
| pushups | Push-ups | climb day, gym | 2 | to 2 shy of failure | 1 | reps |
| roller | Ab roller | off day, home | 3 | 12 to 15 | 1 | reps |
| legraise | Lying leg raise | off day, home | 3 | 15 | 1 | reps |
| floorpress | DB floor press | off day, home (fallback for dips if gym has no station) | 3 | 8 to 12 | 5 lb/side | lb |
| pullover | DB pullover | off day, home | 2 | 12 | 5 lb | lb |

Checkpoints (date, target avg weight, note):
- 2026-10-28, 179.5, "Stop tracking food. You know the portions now."
- 2026-11-11, 177.5, "Upper abs clear in bathroom light. Dips +10 lb or 15 bodyweight reps. Behind? Cut 200 cal."
- 2026-11-26, free day, "Eat whatever. One day. Script resumes the 27th."
- 2026-12-09, 174.0, "4-pack standing relaxed. Floor press up 20+ lb."
- 2026-12-24 to 12-25, free days, "Two days. Then back."
- 2027-01-06, 170.0, "6-pack or close. Cut ends regardless. Phase 2 unlocks."

## Data model (Room)

```kotlin
@Entity data class DayLog(
  @PrimaryKey val date: LocalDate,        // program day, after 4am reset
  val mode: DayMode?,                     // CLIMB, OFF, or null = not chosen yet
  val doneItemIds: Set<String>            // script item ids checked off
)

@Entity data class WeighIn(
  @PrimaryKey val date: LocalDate,
  val lb: Double
)

@Entity data class LiftSet(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: LocalDate,
  val exerciseId: String,
  val weightLb: Double,                   // 0 for bodyweight
  val reps: Int,
  val setIndex: Int
)

@Entity data class ProgressPhoto(
  @PrimaryKey val date: LocalDate,
  val path: String
)
```

Settings (DataStore): `phase` (1 or 2), `startWeight`, `targetWeight`, optional overrides for calories and protein.

## Screens

### 1. Today (start destination)
- Header: "Wed Oct 14 · Day 1 of 84" left, "Phase 1 Cut" right in accent. Below, 44sp "Climb day" or "Off day" or "Pick a day" when mode is null
- Two toggle buttons, Climb day / Off day. Selected one is filled accent with dark text. Changing mode mid-day keeps already-checked meal items (same ids) and drops workout items
- Script list: the day's items as full-width check rows (label, sub, time). Climb script: weigh in, breakfast, climb, push block, lunch, snack, dinner. Off script: weigh in, breakfast, core + push block, lunch, snack, dinner. Tapping toggles. Done rows get strikethrough and the box fills accent. Counter "n / total" top right
- Tapping "Weigh in" opens the Weight screen's input sheet. Tapping a workout item opens Lift with that day's block preselected
- Dinner row: first tap expands three options inline (Burrito bowl 46g/820, Salmon + potatoes 44g/760, Steak + salad 48g/900). Picking one checks the row and writes the pick into the DayLog (`dinnerIndex`). The tiles use the picked meal's numbers. Options list comes from Settings > Meals
- Two tiles: Protein n/165g and Calories n/2250, summed from checked meals only
- Welcome-back card: shown above the XP strip on the first open after 2+ missed days. Copy exactly: "Welcome back." / "Chain paused at N. Nothing reset. Today counts, and a Min day (weigh in + protein) picks the chain right back up." / button "Got it". Freeze-colored border, no red anywhere. Awards the Came Back badge the first time
- Send takeover: full-screen, eyebrow "Send", "+50" at 120sp, "Bonus roll" label, a reel that spins "??" for 900ms then slams the result (0, +25 or +100) with a heavy haptic, sub-copy "Every item checked. Today's hold just went green.", button "Nice". Confetti is accent and success colors only, launched from the nav bar upward, ~1.2s. Level-up reuses the same takeover with eyebrow "Level up", the new grade as the big text, the reel showing "V3 → V4", sub-copy "New grade. The bar starts over, the chain does not."
- Day boundary is 04:00 local. Past 04:00, a new DayLog is created lazily on first open
- One rest day per week is just an Off day with the workout item unchecked, nothing special

### 2. Weight
- Hero tile: 7-day trailing average, 46sp. Sub-line: delta since START_WEIGHT and pace. Pace is "on pace" (green) when the average is at or below the linear line from START_WEIGHT to TARGET_WEIGHT over the 84 days, otherwise "behind, cut 200 cal" (accent)
- Second tile: target 170.0, lb to go
- Chart: 12-week x axis, y range 170 to 182. Dashed target line, accent 7-day average line, daily readings as dim dots hidden by default. Long-press the chart reveals the dots while held. Draw with Compose Canvas, no chart library
- Input: numeric field, one decimal, Log button. One entry per program day, logging again overwrites. Enter key submits
- 7-day average uses the last 7 logged days, not 7 calendar days, until 7 entries exist, then trailing 7 calendar days with gaps ignored

### 3. Lift
- Shows today's block based on Today's mode. Climb day: dips, push-ups. Off day: ab roller, lying leg raise, DB floor press, DB pullover. If mode is null, show both blocks with headers
- Each exercise card: name, "Last: 50 × 10" (from the most recent session that has this exercise), weight stepper and reps stepper. Steppers use the exercise step. Values prefill from last session. A value above last is drawn green. The card border goes green when either value beat last
- Header pill: "Week won" green once any exercise in the current week (Mon to Sun) beat its previous session, else "Not yet" muted
- Save session writes one LiftSet row per set with the same weight and reps (good enough, per-set editing is a later feature)
- Bodyweight exercises store weightLb 0 and show "Knees" or "Slow" in the weight slot as in the design, with the stepper hidden

### 4. Goals (Checkpoints)
- Photo banner at top when a progress photo is due (first on day 1, then every 14 days, or if none taken in the last 14 days). Shoot button opens CameraX, saves to filesDir/photos/YYYY-MM-DD.jpg, writes ProgressPhoto. Banner shows a thumbnail grid of past photos when none is due. Long-press a thumbnail to delete
- Timeline of checkpoints. State per row: hit (green check, shows the actual avg weight that day), current (accent ring, shows target and days until), upcoming (dim). Free days are dim with "Free day" instead of a weight. Thanksgiving and Christmas rows also suppress the calorie tile on Today for those dates
- Jan 6 row: when reached, button "Start Phase 2" flips `phase` to 2

### Phase 2 behavior
- Calories tile reads 2700. Today header reads "Phase 2 Chest"
- Lift blocks change: climb day = dips (weighted) 4 sets, push-ups 2; off day = DB floor press 4 sets, incline DB press 3 sets, DB pullover 2, ab roller 1 set. Add `inclinepress` exercise id
- Checkpoints screen shows "Phase 2, no fixed end" and keeps the photo cadence

## Progression system (the reason he keeps opening it)

Design rules, in priority order. Every feature below must satisfy all five:
1. Reward happens within 300ms of the action. Never on a later screen, never "at the end of the day."
2. Nothing ever goes to zero. No streak resets, no red, no "you missed 4 days." Missed days are grey holds on the route and that is the whole consequence.
3. The minimum day is always within reach: weigh in + hit protein = a Min day, which keeps the chain alive. A full script = a Send.
4. Variable reward on top of fixed reward. Fixed XP is predictable, bonus XP is a small random roll.
5. Re-entry is celebrated. Opening the app after 2+ missed days shows "Welcome back, chain paused at N, today counts" and the Came Back badge the first time. No guilt copy anywhere.

### XP
- Script items: weigh in 10, breakfast 10, lunch 10, snack 5, dinner 10, climb 25, push block 30, core + push block 30
- Lift session saved 20. Each exercise that beat its last session +15. "Week won" the first time in a week +50
- Progress photo 40
- Send (all items checked) +50 plus a bonus roll: 60% chance 0, 30% chance 25, 10% chance 100. Roll animates as a slot reel for ~900ms, then the number slams in with a heavy haptic
- Min day (weigh in + protein ≥ target) +20 if it is not already a Send

### Levels
V-grades, because he boulders. Thresholds (cumulative XP): V0 0, V1 300, V2 700, V3 1200, V4 1800, V5 2500, V6 3300, V7 4200, V8 5200, V9 6300, V10 7500. A disciplined 12 weeks lands around V8 to V9. Level badge sits top-left of the XP strip on Today. Level-up: full-screen takeover for 1.5s, big "V4" in accent, confetti burst, haptic pattern, dismiss on tap or auto.

### Chain
- Chain = consecutive program days that were Min or Send. A missed day pauses the chain (shown as "paused at 11"), it does not reset.
- Freezes: 2 per calendar month, auto-applied to the first two missed days each month. A frozen day shows blue on the route and keeps the chain unbroken. Freeze count is visible next to the chain on Today. Unused freezes do not roll over.
- Chain badges at 7, 14, 30, 60, 84.

### Weekly project
One rotating challenge, Monday to Sunday, shown at the top of Route with a 3-segment progress bar. Pool (pick one at random, never repeat the previous week's):
- Beat last week's dips three times
- Log weight all 7 days
- Three Sends in a row
- Two push blocks on climb days
- Hit protein 6 of 7 days
- One full Off-day block before noon, three times
Completing one: +150 XP and a named badge (Overhang, Crimp, Dyno, Heel Hook, Mantle, Top Out). Failing one: nothing happens, next Monday a new one appears.

### Route screen (replaces the Goals tab, checkpoints fold into it)
- Header: Day N of 84 and percent, Send count
- Weekly project card
- 84-hold grid, 12 rows of 7, week 12 at the top, week 1 at the bottom (you climb upward). Hold states: Send green `#C8F55A`, Min dim green `#4F5A3A`, Freeze blue `#5AA9FF`, Missed `#2A2C31`, Today hollow with accent ring, Future `#1C1E22`. Anchor icon at the right of rows 2, 4, 8, 12 for checkpoints; tapping an anchor opens the checkpoint sheet (the old Checkpoints screen content, including the photo flow). Anchor color: green when hit, accent when current, muted when upcoming
- Badge shelf: 4 across, earned badges filled with their color, unearned dashed outline with a + icon and name in secondary text
- Long-press a hold to see that day's summary (mode, items done, XP, weight)

### Today screen, v3 behavior (prototype is authoritative)
- Day picker: when mode is null, two tall cards (Climb day / Off day) with an icon, a one-line description and the item count + total XP. No segmented control. Once picked, the cards collapse into a small pill under the headline: icon, "Climb day", "Change" in accent. Change clears the day's items and returns to the cards
- Done items leave the main list: they drop into a collapsed "Done · n" group below the list, rendered compact (one line, no sub-label, small check). Tap the divider to expand. The main list only ever shows what is left, with "Up next" on the first row
- Weigh in is inline: tapping the row expands a one-line input prefilled with yesterday's reading, -.2 / +.2 chips, Log. Enter logs. Logging checks the row, awards +10, and updates Weight tab data. No navigation
- Protein tile is a progress bar toward the target with the fraction in the label; bar turns success green at target. Calories tile shows "n left" instead of a second fraction
- "Chain safe" pill: appears in success green to the right of the mode pill the moment weigh-in is logged and protein reaches target (a Min day). The chain number in the XP strip increments and its caption reads "secured today". This is the single most important state on the screen: the user can see the day is already a win before the workout
- Send progress bar above the nav and the "Sent" headline carry over from v2
- 7pm state: if mode is still null at or after the "Climb or off?" time (default 19:00), the day cards are replaced by one accent-bordered card: "It's 7pm and no day is picked." / "Guessing Off day. Weigh in and hit protein and the chain is safe. Nothing else required tonight." / buttons "Yep, Off day" (primary) and "I climbed" (secondary, sets Climb and pre-checks the Climb item). This is also the body of the 19:00 notification; tapping it deep-links here
- Swipe to check: any row in the main list swipes right (threshold 80dp, max travel 140dp) over a success-green "Done" underlay; release past the threshold checks it with a haptic. Dinner swiped without a pick takes the first option. Tap still works. Done rows do not swipe back; uncheck from the Done group by tap

### UX details (all visible in the prototype)
- "Up next": the first unchecked script item gets a small accent "UP NEXT" eyebrow and a slightly stronger border. There is always exactly one, so the eye lands somewhere
- Send progress: a 3dp bar sits directly above the bottom nav on every tab, fills accent as items are checked, turns success green on a Send. It is the one piece of Today state visible from any screen
- Sent state: when all items are checked the Today headline reads "Sent" in success green instead of the day mode
- Numbers never swap, they count: XP total, protein and calorie tiles and the 7-day average animate from old to new value over 350ms with ease-out
- Weight input prefills with yesterday's reading and has four quick-adjust chips (-1.0, -0.2, +0.2, +1.0) so most mornings are one tap plus Log. Enter submits. Logging also checks "Weigh in" on Today
- Pace copy on Weight is computed against the linear line from start to target weight at today's day number, with 0.3 lb tolerance: "on pace" (green) or "behind, cut 200 cal" (accent)
- Lift has a 90-second rest timer card at the top: Start / Stop / Reset, border goes accent while running and green at zero, triple haptic at zero
- Route holds are tappable (not long-press): a bottom popover names the date, the day's result (Send / Min / Freeze / Missed / Today / Upcoming) with its XP, and one line of detail
- Settings > Meals is a working editor: name, description, time, protein, calories per row, remove, add. Saving updates the Today rows and tiles immediately. Exercises and Checkpoints editors follow the same pattern

### Feedback polish
- Checking an item: box fills accent with a 150ms spring scale, small haptic (CONFIRM), the row's "+10" flips from dim to green and the XP bar on the strip animates forward
- Unchecking: no sound, no haptic, XP bar animates back. No confirmation dialog
- Send: 500ms confetti from the bottom nav upward in accent and green only, haptic HEAVY_CLICK, then the bonus roll
- Daily counter "n / total" ticks with a number-roll animation
- All of this respects the Settings "Haptics and confetti" toggle

### Data additions
```kotlin
@Entity data class XpEvent(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: LocalDate, val at: Instant, val kind: String, val amount: Int, val ref: String?
)
@Entity data class Badge(@PrimaryKey val id: String, val earnedAt: Instant)
@Entity data class WeeklyProject(
  @PrimaryKey val weekStart: LocalDate, val id: String, val progress: Int, val done: Boolean
)
```
XP total is `SUM(amount)`. Level is derived, never stored. Chain is derived from DayLog + freezes, never stored. Unchecking an item deletes its XpEvent, so XP is always reproducible from DayLog.

## Settings screen
Opened from the gear next to the phase label on Today. Everything in `ProgramSpec` becomes editable here and persists in DataStore; `ProgramSpec` becomes the defaults.
- Program: start date (date picker), length in weeks (recomputes end date and the 84-hold grid), start weight, target weight, phase (1 Cut / 2 Chest)
- Targets: calories, protein. Meals (sub-screen: edit name, sub-label, time, protein, calories, add or remove, reorder). Exercises (sub-screen: name, which block, sets, rep target, step, unit, add or remove). Checkpoints and free days (sub-screen: date, target weight or "free", note, add or remove)
- Nudges: weigh-in time, "Climb or off?" time, day rollover hour, haptics and confetti toggle
- Danger zone at the bottom, below a divider: Export JSON (share sheet), Import JSON, Reset program (double confirm, wipes everything)
- Changing start date or length re-derives Day N and the route grid but never deletes logs. Logs outside the new window still exist and still count for XP.
- Save button at the bottom. Changes apply on Save, not live.

## First launch (onboarding)
Four full-screen steps with a 4-segment progress bar at the top, Back / Next at the bottom. Copy is final:
1. Headline "Twelve weeks. Four taps a day." Body: "Weigh in, eat the script, do the block, check it off. The app keeps score so you don't have to. Nothing here resets, ever." Button "Let's go"
2. "Where you're starting." Two big numeric fields: "This morning, before food" (required, 64dp tall, 32sp) and "Where you want to land" (prefilled 170). Live hint under them: "11.8 lb over 12 weeks is 0.98 lb a week." Next is blocked with the field outlined accent if start weight is empty
3. "When it runs." Start date (today) and Weeks (12). Live hint: "Ends Wednesday, January 6. 84 holds on the route."
4. "One nudge a day." Weigh-in reminder time (08:30). Body: "That's the only daily notification. A second one fires at 7pm only if you haven't picked a day." Button "Start the route"
Finishing writes START_WEIGHT, TARGET_WEIGHT, start date, length and reminder time to DataStore, logs the first WeighIn for today, and lands on Today in the "Pick a day" state with a toast "Day 1. Pick a day." Onboarding never shows again unless Reset program runs. Prototype: Settings > "Prototype: show first launch".

## Settings editors
All three are the same pattern: a list of rows in one card, each row editable inline, an × on the row to remove, "Add" below the list, "Save" at the bottom applying everything at once. Reorder is drag on the row handle (not in the prototype).
- Meals: name, description, time, protein, calories
- Exercises: name, block (Climb / Off pill pair), sets, step in lb. Step 0 means bodyweight: the Lift screen shows the label ("Body", "Knees", "Slow") in the weight slot and hides the weight stepper. Lift screen filters by today's block, so a Climb day shows Dips and Push-ups, an Off day shows the four home lifts
- Checkpoints: date, Target / Free pill pair, target weight (hidden when Free), note. List stays sorted by date. Route anchors and the Checkpoints sheet are derived from this list

## Notifications
- One daily notification at 08:30 local: "Weigh in" (opens Weight). Use AlarmManager exact alarm with the permission flow, or WorkManager periodic if exact is denied
- One notification at 19:00 on any day where mode is still null: "Climb or off?"
- Nothing else. No streak nagging

## Home screen widget (Glance)
This is the ADHD feature that matters most after the Send moment: the app has to be visible without being opened. One 4×2 Glance widget, dark and light variants from the same tokens:
- Left: today's mode label ("Climb day" / "Off day" / "Pick a day") and the "Up next" item name
- Middle: the Send progress as a 7-segment bar (one segment per script item)
- Right: chain number with the link icon, and "Chain safe" in green once the Min day is hit
- Tap on the left opens Today. Tap on the "Up next" item checks it directly from the widget (Glance action) with the same haptic, and the widget re-renders within a second
- A 2×1 variant shows only the chain number and the progress bar
Widget updates on every DayLog change and at the rollover hour.

## App icon
Adaptive icon: ground `#0C0D0E` background layer, foreground is a single hold shape (rounded 7-gon, like the route grid holds) in accent `#FF5A1F` with the chain link icon knocked out in the ground color. Monochrome layer is the same silhouette. Provide the SVG in `app/src/main/res/` and generate mipmaps with the Android Studio asset tool.

## Resilience
- Day boundary is computed in the device's current zone with java.time; a timezone change mid-program never moves existing DayLog dates
- Android Auto Backup on (`android:allowBackup="true"`, `fullBackupContent` including the Room db and DataStore, excluding the photos folder). Photos are also included in the JSON export as a zip when the user chooses "Export with photos"
- Room migrations from v1 on, even in development. Never `fallbackToDestructiveMigration`
- All XP, chain and level values are derived in one `ProgramEngine` class with pure functions over the Room rows. Unit tests cover: rollover at 03:59 vs 04:00, chain with 0, 1 and 3 misses in a month, freeze rollover at month boundary, Min vs Send detection, level thresholds, pace line at days 1, 42 and 84
- Reminder alarms re-register on BOOT_COMPLETED and after the app updates

## Unattended run protocol
The person is not watching. Follow this exactly so the run is either finished or stopped at a known point.

1. Before step 1, create `PROGRESS.md` with the 13 build steps as checkboxes. After every step: tick it, write three lines (what was built, decisions made, anything deferred), commit with the message `step N: <title>`. Read `PROGRESS.md` first whenever context is lost.
2. Every step ends with `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` passing. A step is not done until both pass. Fix build failures before moving on; if a dependency version fails to resolve, look up the current stable version in the Maven index and pin it in the version catalog, never guess a number.
3. Try at most three fixes for any one failure. On the third failure, write the error and what was tried into `PROGRESS.md` under "BLOCKED", commit, and stop. Do not skip the step and continue.
4. Never ask a question. Decide, write the decision in `PROGRESS.md`, keep going. Every ambiguity in this document has a default: when in doubt, copy the prototype.
5. Do not add features, libraries or screens not in this document. No analytics, no Hilt unless it is already the project convention, no extra abstraction layers.
6. Emulator pass, after step 13. First check `adb devices`. If no device is listed, boot one yourself: `emulator -list-avds` and start the first AVD with `emulator -avd <name> -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &`, then `adb wait-for-device` and poll `adb shell getprop sys.boot_completed` until it returns 1 (give it up to 4 minutes). If `emulator` or `adb` is not on PATH, look under `$ANDROID_HOME`, `$ANDROID_SDK_ROOT`, `~/Android/Sdk` or `~/Library/Android/sdk` and use the full path. Once booted: `./gradlew :app:installDebug`, launch the app with `adb shell am start -n com.relentnet.cuttracker/.MainActivity`, walk the acceptance checks that do not need a real clock by driving the UI with `adb shell input tap/text/swipe` (log the coordinates you used), and save `adb exec-out screencap -p` for each of the five main screens, the Settings screen, onboarding step 2 and the Send takeover into `screenshots/`. Open each PNG and compare it to the prototype. Fix visible differences in spacing, type size, color and copy, rebuild, reshoot. Two rounds maximum. If the emulator will not boot after two attempts, write that in `PROGRESS.md` and finish without the pass. Kill the emulator when done.
7. Finish by writing `PROGRESS.md` "DONE" with: APK path, which acceptance checks passed, which could not be run, and a list of anything that differs from the prototype and why.

Kickoff prompt to paste into Claude Code:
"Read CLAUDE.md, then CUT_TRACKER_HANDOFF.md in full, then open cut-tracker-prototype.html and read it as the UI reference. Follow the Unattended run protocol section exactly. Build all 13 steps. Do not stop to ask questions. When finished or blocked, PROGRESS.md is your report."

## Repo setup for Claude Code
Create `CLAUDE.md` at the repo root with: the build command (`./gradlew :app:assembleDebug`), the test command (`./gradlew :app:testDebugUnitTest`), the install command for a connected phone (`./gradlew :app:installDebug`), the package name, "read CUT_TRACKER_HANDOFF.md first", "the prototype link is the source of truth for look and copy", and "no em dashes in any user-facing copy". Commit the handoff file and the prototype's HTML export beside it so the spec survives if the link changes.

## Non-goals (do not build)
- No food database, barcode scanning or calorie search. Meals are fixed items
- No cloud sync or accounts (local JSON export/import is fine)
- No leaderboards, no social, no sharing cards
- No tablet layout
- No per-set rep editing in v1
- No Google Fit / Health Connect

## Build order
1. Project scaffold, theme (colors, Archivo type), bottom nav with 4 empty screens (Today, Weight, Lift, Route) plus Settings route
2. Room entities, DAOs, repository, ProgramSpec defaults + DataStore overrides, day-boundary logic with unit tests (rollover hour, Day N of length, 7-day average, pace line)
3. Today screen end to end, including XP strip, item XP, check animation and haptics
4. XP engine: XpEvent writes and deletes, level derivation, Send and Min detection, bonus roll, level-up takeover. Unit tests for chain and freeze logic
5. Weight screen with Canvas chart
6. Lift screen with steppers, prefill, "week won", lift XP
7. Route screen: hold grid, anchors opening the checkpoint sheet, CameraX photo flow, badge shelf, weekly project engine
8. Settings screen and its three sub-screens, export/import
9. Notifications, boot re-registration
10. Phase 2 switch
11. Home screen widget, app icon
12. Export/import with photos, Auto Backup config, Room migration scaffold
13. Release build, signed APK, sideload instructions in README

## Acceptance checks
- Fresh install on Oct 14 shows Day 1 of 84, "Pick a day", V0, 0 xp, chain 0, 2 freezes
- Checking breakfast shows Protein 48 / 165g, the row's +10 goes green, XP reads 10, bar animates
- Logging 181.8 then 181.2 shows avg 181.5 and "on pace"
- Checking every Climb-day item triggers confetti, +50, a bonus roll, and that day's hold turns green on Route
- Weigh in + protein ≥ 165 with items left unchecked marks a Min day (dim green hold) and keeps the chain
- Skipping Oct 20 entirely: Route shows it blue (freeze), chain unbroken, freezes read 1
- Skipping three more days that month: third is grey, chain shows "paused at N", next open shows the welcome-back card and the Came Back badge
- Saving floor press at 55 × 10 after a prior 50 × 10 turns the card border green, +15 xp, and the header to "Week won" (+50 the first time that week)
- Reaching 1200 xp shows the V3 takeover
- Settings: changing length to 10 weeks re-renders the route as 70 holds and moves the end date; no logs lost
- At 03:59 the day is still yesterday; at 04:00 it rolls (or whatever rollover hour is set)
- Airplane mode changes nothing
