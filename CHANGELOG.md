# Changelog

What changed in Sendline, from the user's side. Newest first.

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
