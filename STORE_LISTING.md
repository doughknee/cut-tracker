# Play Store listing draft

Everything the Play Console asks for, in one place. Copy from here; edit freely.

## App name

Cut Tracker

## Short description (max 80 characters)

A 12-week cut for climbers. Weigh in, eat the script, send the day.

## Full description (max 4000 characters)

Cut Tracker turns a diet into a route. Every day is a hold on the wall: pick the kind of day (Climb, Off or Rest), weigh in, eat the script, do the block, check it off. Finish everything and the day is a Send. Weigh in and hit protein and it is a Min day, which keeps your chain alive even when the rest slips.

The wall
Your week is seven holds up a wall. Shape is the kind of day, color is how it went, the rope is your chain. Tap any hold to open that day, including the ones you forgot to log. Late checks count.

The route
Twelve weeks (or however long you choose) as a calendar of holds, with checkpoints on the pace line and badges along the way. Freezes cover two missed days a month so one bad day never costs you the chain.

Scoreboard, not a lecture
XP for every item, grades from V0 to V10, a weekly project, a workout target you set yourself. No ads, no account, no feed.

Yours
Everything stays on your phone. No internet permission, no analytics, no sign-in. Export a backup whenever you like.

Meals and lifts
A meal library with recipes and per-slot picks, a lift log with a rest timer that remembers last session, a weight chart with the 7-day average against your pace line, progress photos every two weeks.

Cut Tracker is a logbook and a scoreboard, not medical advice. Set your own targets and check with a doctor before a diet if you have a condition.

## Category

Health & Fitness

## Tags

weight loss, diet tracker, climbing, habit tracker, calorie counter

## Contact

Email: bcharris713@gmail.com
Privacy policy: host `docs/privacy.html` (GitHub Pages works: push the repo, enable Pages on the `docs/` folder, then the URL is `https://<user>.github.io/cut-tracker/privacy.html`).

## Data safety form

- Does your app collect or share any of the required user data types? **No.**
  - The app has no INTERNET permission. Data stays on the device. Export is user-initiated and goes to an app the user picks.
- Is all of the user data collected by your app encrypted in transit? Not applicable, nothing is transmitted.
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
- Phone screenshots: at least 4, 16:9 or 9:16. `store/` has 1080x1920 captures from the emulator: the Wall, an earlier day, the info sheet, the Route, Settings.

## Release checklist

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
3. First upload: opt in to Play App Signing; the current `cut-tracker.jks` becomes the upload key. Keep it, back it up.
4. Closed testing first. New personal developer accounts need 12 testers opted in for 14 days before production unlocks.
5. Fill Data safety, content rating, privacy policy URL, store listing, then roll out to production.
