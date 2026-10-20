# Play Store listing draft

Everything the Play Console asks for, in one place. Copy from here; edit freely.

## App name

Sendline

## Short description (max 80 characters)

The cut for climbers. Weigh in, eat the script, send the day. Offline.

## Full description (max 4000 characters)

Sendline turns a diet into a route. Every day is a hold on the wall: pick the kind of day (Climb, Off or Rest), weigh in, eat the script, do the block, check it off. Finish everything and the day is a Send. Weigh in and hit protein and it is a Min day, which keeps your chain alive even when the rest slips.

The wall
Your week is seven holds up a wall. Shape is the kind of day, color is how it went, the rope is your chain. Tap any hold to open that day, including the ones you forgot to log. Late checks count.

The route
Twelve weeks (or however long you choose) as a calendar of holds. Checkpoint days wear a flag, green once you hit them, and a tap on one says what it asks for and how far off it is. Freezes cover two missed days a month, and a day off you plan ahead never costs you the chain.

Beta, the plan
Set the route's numbers in one place: start, goal, weeks, protein, calories, workouts. Decide what each kind of day asks for. See this week's protein and workouts against the plan.

Meals and lifts
A meal library grouped by kind. Build a meal from ingredients with an offline USDA food search that adds up protein and calories, or point the camera at a nutrition label and let the app read it. The picker leads with what you actually ate for that slot. A lift log that remembers last session and shows the trend, a weight chart with the 7-day average against your pace line, progress photos every two weeks.

Looks
Thirteen looks, a dark mode that comes on at night, a calendar mode that dresses the wall for the month and the holidays, and a hue ring to chalk your own and share it with a four-letter code.

Scoreboard, not a lecture
XP for every item, grades from V0 to V10, a workout target you set yourself. No ads, no account, no feed.

Yours
Everything stays on your phone. No analytics, no sign-in. Two features use the network and both are off until you switch them on: paste a recipe link and the app fills in the meal, or scan a barcode and it looks the product up. Export a backup whenever you like.

Sendline is a logbook and a scoreboard, not medical advice. Set your own targets and check with a doctor before a diet if you have a condition.

## Category

Health & Fitness

## Title on Play (max 30 characters)

Sendline: the cut for climbers

## Tags

weight loss, diet tracker, climbing, habit tracker, calorie counter

## Contact

Email: bcharris713@gmail.com
Privacy policy: https://doughknee.github.io/cut-tracker/privacy.html (GitHub Pages, served from `docs/` on `main`; edit `docs/privacy.html` and push to update it).

## Data safety form

- Does your app collect or share any of the required user data types? **No.**
  - The INTERNET permission is used for two things, each only while the user has switched it on: fetching a recipe page the user pastes, and looking a scanned barcode up on Open Food Facts (the request carries the barcode digits and nothing else). No user data is sent. Export is user-initiated and goes to an app the user picks.
- Is all of the user data collected by your app encrypted in transit? No user data is transmitted. The recipe fetch and the barcode lookup are HTTPS only.
- Do you provide a way for users to request that their data is deleted? Yes, Settings > Reset program, and uninstall.
- Health data: the app stores weight and food logs on-device only. Answer "no" to collection because nothing leaves the device; mention on-device health logs in the privacy policy (done).

## Content rating questionnaire

Utility / productivity type app. No violence, sexual content, language, controlled substances, gambling, user interaction, user-generated content shared with others, or location sharing. Expected result: Everyone.

## Permissions to declare

- CAMERA: progress photos, stored privately.
- POST_NOTIFICATIONS: two optional daily reminders.
- SCHEDULE_EXACT_ALARM: reminders at the chosen minute; falls back to inexact when denied. (USE_EXACT_ALARM was removed on purpose: Play only allows it for alarm clock and calendar apps.)
- RECEIVE_BOOT_COMPLETED: reschedule reminders after a reboot.
- VIBRATE: haptic ticks.

## Graphics

- App icon: 512x512 PNG, export from `app/src/main/res/drawable/ic_launcher_foreground.xml` on the `ic_launcher_background` color.
- Feature graphic: 1024x500 PNG. Suggested: the wall texture with one bright green jug and the words "Send the day."
- Phone screenshots: at least 4, 16:9 or 9:16. `store/` has 1080x2201 captures from the emulator (1.2.0, Sandstone look, dark mode, a sample week seeded with `adb shell am start -n com.relentnet.cuttracker/.MainActivity --ez seed true` on a debug build), cropped to the app window between the status bar and the gesture bar:
  - `01-wall.png`: Today. The week as seven holds up the wall, the rope for the chain, the script below.
  - `02-earlier-day.png`: Beta. The plan as a grid: Meals, Lifts, the kinds of day, Checkpoints, Targets, This week.
  - `03-legend.png`: Meals. The library grouped by kind, with ingredients, labels and barcodes behind one sheet.
  - `04-route.png`: Route. Twelve weeks of holds with flags on the checkpoint days.
  - `05-script.png`: Appearance. Thirteen looks in a grid, each with its chalk, send and freeze colors.

## Release checklist

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
3. First upload: opt in to Play App Signing; the current `cut-tracker.jks` becomes the upload key. Keep it, back it up.
4. Closed testing first. New personal developer accounts need 12 testers opted in for 14 days before production unlocks.
5. Fill Data safety, content rating, privacy policy URL, store listing, then roll out to production.
