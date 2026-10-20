# Changelog

What changed in Sendline, from the user's side. Newest first.

## 1.3.0 (versionCode 4), 2026-10-20

### History stays put

- A past day keeps the result it was logged with. Raising protein, changing what a kind of day asks for or editing a meal no longer turns old Sends into Min days or uses up freezes.
- Editing or deleting a library meal leaves the days that logged it as they were.
- Two quick edits never undo each other: steppers can be tapped fast, switches flipped back to back.
- Editing a past day can only raise its result, unless you untick something or change its kind.
- "Change" on a day keeps its ticks, so picking the same kind again loses nothing.
- A lift block done on a Rest day counts as a workout.

### Fewer taps, nothing lost

- Back closes what is open first: the weigh-in, a meal picker, the lifts, the Route day popover, a takeover, a sheet. On an earlier day it goes back to today. In onboarding it goes back a step.
- A done weigh-in shows its reading; tap it to fix a typo.
- Every stepper repeats while held, and its number can be tapped to type it: all of Targets, and weight and reps on Today's lifts.
- Lift numbers typed on Today survive closing the panel.
- Numbers read the way you type them: 181,4 and 2,250 work, and a decimal no longer saves as 0.
- A focused field always scrolls above the keyboard, and a number field selects its value so typing replaces it.
- Deleting a meal, a lift, a kind, a checkpoint or a custom look shows Undo for six seconds.
- A sheet with unsaved edits asks before a swipe or a tap outside throws them away.
- Search matches every word in any order and ignores accents. The lift picker and, past eight meals, Today's meal picker get a search box.

### Meals and the scanner

- What you type in New meal, a custom look or a day-plan sheet survives a dark-mode switch or the app being closed in the background.
- Removing the last ingredient clears its numbers, and reading a label while a meal has ingredients adds it as one more line.
- Network problems read as plain sentences, and an http link says only https links can be read.
- The scanner lets go of the camera when it closes, offers Open settings when the camera is denied, and the label reader waits for a clean frame and says so when it cannot read one.
- A meal made from Today's picker lands under that meal's kind.
- Pasting a code for a look you already have selects it.

### Route, Targets and the widget

- Changing the start, goal, weeks or start date on Targets moves the checkpoints with it, unless you set them by hand.
- The widget shows a plain card before you set up, before a future start and after the route, and a tap on a widget left over from yesterday never ticks today.
- Past the end of the route, being at goal no longer reads as behind pace.
- A second route's chart starts at its own start; Today says when a future route starts instead of a negative day.
- A new checkpoint lands on the first free day a week out; "today" and "tomorrow" replace "in 0 days".
- Starting a second route can be called off at the first step.

### Reminders

- Each reminder has its own switch, and the times follow your phone's 12 or 24-hour clock.
- The weigh-in reminder stays quiet once you weighed in.
- The pick-a-day reminder has the evening card's two answers, Yep, Rest day and I climbed, right in the notification.
- Reminders come back after a time zone or clock change.

### Data

- Import and Reset ask first and offer Export first. A failed export says so.
- Restoring a backup is all or nothing: a file the app cannot read leaves your data as it was.
- Data shows when you last exported, because the phone holds the only copy until you do.
- Save a copy writes the backup straight to a file you pick, next to Export through the share menu.
- Moving phones: a fresh install can restore a backup from the first screen, no set-up first.
- A denied camera offers Open settings, and coming back with it allowed opens the viewfinder.

### Easier to read and reach

- Large text on a small screen wraps instead of squeezing: Today's totals, Route's header and the Beta grid, which scrolls when it no longer fits.
- Small buttons have 48dp targets; TalkBack hears switch and tick states, tab selection, every Route day and the weight chart.
- Row descriptions wrap to two lines instead of cutting off.

## 1.2.0 (versionCode 3), 2026-10-20

### Beta

- Beta is a grid of pages that fills the screen: Meals, Lifts, the three kinds of day, Checkpoints with its progress bar, Targets and This week.
- Each kind of day has its own page. Pick which meal slots it asks for, which lifts are in its block, and switch the rest of the script on or off. The default plans match the old script exactly.
- Targets is the one place to change the route's numbers: start weight, goal, weeks, start date, protein, calories and workouts, with the route math updating as you type.
- This week shows the day strip, protein per day against what you logged, and workouts so far.
- Checkpoints is a card of flags with a sheet to edit each one. A flag's date is its identity: adding on a date that already has a flag opens that flag instead of stacking a second one.
- "Day n of 84" under today's date opens the Route.

### Meals and lifts

- Meals and Lifts are libraries grouped by kind, with search, a chip per kind, and a sheet to add or edit. New kinds become slots on the script (meals) or names (lifts).
- A meal can add up from ingredients. Search a bundled USDA food table (8,041 foods, offline), tap a food to add a line in grams, and the meal's protein and calories become the sum. The manual fields stay editable.
- Read a nutrition label with the camera. The label reader runs on the phone and pulls protein, calories and the serving size into New meal, then says which lines it trusted.
- Share a link or some text into Sendline from any app and New meal opens with it: a link lands in the recipe field, other text becomes the name.
- The meal picker on Today leads with the five meals that slot logged most in the last 30 days, then a Same as yesterday chip, then the library.
- Widget meal rows now name the meal a tap logs.

### Looks

- Appearance is a page with thirteen looks in a grid, each tile drawn in its own stone, and a corner of Today at the top that plays: tick the item, log it, and the look's confetti goes up.
- Dark at night switches to dark between 19:00 and 07:00.
- Calendar mode gives every month a look and lets five holidays take the wall for their days: Valentine's, the Fourth, Halloween, Winter and New Year. Holiday looks pin a pumpkin, snow, a heart, sparks or bunting to the top edge of every hold and scatter motifs on the wall behind the week.
- Chalk your own look: drag a hue ring for the accent, stand it on one of six stones, name it. Custom looks save with your backup and carry a four-letter SL- code you can paste on another phone.
- Changing looks eases every color over a moment instead of snapping.

### Route

- Route is the record of the whole route. The Workouts row moved to Beta's This week, so the hold calendar starts right under the header.
- Checkpoint days wear a ring and a flag that sit on the day, green once the checkpoint is hit. Tap one ahead and it names the checkpoint, its target and the days to go, with a button to Checkpoints. Tap one behind and the day's result comes first with the checkpoint under it.
- The week column counts program weeks, 1 to 12, the same as the wall on Today.
- The next-checkpoint card is labelled, and the wall's color key says "Freeze or skip" like the Route legend.

### Settings

- Settings keeps the app's chrome: Nudges, Appearance, Data, About. Program and Targets moved to Beta.
- The landing opens with a privacy card that says "Offline, nothing leaves the phone" or how many online features are on. Tapping it opens Data and points at the Online features card.
- Every setting applies at once. The Save buttons are gone.

### Online features

- Two features need the network, both off by default, each with its own switch under Settings > Data > Online features and a link to the code that makes the request.
- Recipe import: paste a recipe link into New meal and the app downloads that one page and fills in the name, servings, protein and calories when the page lists them. HTTPS only, 2 MB cap.
- Barcode lookup: scan a barcode in New meal and the app asks Open Food Facts for that one product. The request carries the barcode digits and nothing else.
- The first use of either shows a card that says what the fetch does and lets you allow it in place.

## 1.1.0 (versionCode 2)

The build the Play listing was prepared on: the Wall, the Route, Weight, Beta and Settings, with reminders, backups, progress photos, the home screen widget, and a back gesture that slides between tabs.
