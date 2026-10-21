package com.relentnet.cuttracker.ui.onboarding

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.Clock
import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Dates
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.Starters
import com.relentnet.cuttracker.ui.components.Chip
import com.relentnet.cuttracker.ui.components.count
import androidx.compose.foundation.horizontalScroll
import com.relentnet.cuttracker.data.Units
import com.relentnet.cuttracker.data.Vocab
import com.relentnet.cuttracker.data.restoreFrom
import com.relentnet.cuttracker.data.w
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.Takeover
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.MiniStepper
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.int
import com.relentnet.cuttracker.ui.components.num
import com.relentnet.cuttracker.ui.settings.SwitchRow
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.LocalVocab
import com.relentnet.cuttracker.ui.theme.Palettes
import com.relentnet.cuttracker.ui.theme.ThemeMode
import com.relentnet.cuttracker.ui.theme.cutStyle
import com.relentnet.cuttracker.ui.today.Wall
import com.relentnet.cuttracker.ui.today.resultName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class Step { HELLO, WORDS, START, GOAL, PROTEIN, MEALS, LIFTS, NUDGE, READY }

/**
 * The first run, one question a screen, every answer filled with a default so Next is one tap. The steps climb a row
 * of holds that go green as they are answered; the hello plays a week on the real wall. Finishing writes the program
 * settings, logs the first weigh-in and lands on Today under the start call. [preview] (Developer options) starts
 * from a fresh install's settings and writes nothing; a second route ([again]) skips the words and keeps the look.
 */
@Composable
fun OnboardingScreen(vm: AppViewModel, preview: Boolean = false, onDone: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = if (preview) remember { Settings() } else st.settings
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Starting the next route keeps every log, badge and XP; only the program changes.
    val again = !preview && (st.logs.isNotEmpty() || st.weighIns.isNotEmpty())
    // A next route keeps its words and its plan; only the numbers are asked again.
    val steps = remember(again) { if (again) Step.entries - setOf(Step.WORDS, Step.MEALS, Step.LIFTS) else Step.entries.toList() }
    var at by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[at.coerceIn(steps.indices)]
    var forward by remember { mutableStateOf(true) }
    // A recap row sent the user back to one step; Next returns to the recap instead of walking every step again.
    var toReady by rememberSaveable { mutableStateOf(false) }

    // The answers. Weights are typed in the unit picked beside them; everything is saved in lb.
    var climbs by rememberSaveable { mutableStateOf(s.training == null) }
    var trainName by rememberSaveable { mutableStateOf(s.training ?: "") }
    var kg by rememberSaveable { mutableStateOf(s.kg) }
    var sw by rememberSaveable { mutableStateOf(if (preview) "" else st.weighIns.firstOrNull { it.date == st.today }?.lb?.let { s.w(it) } ?: "") }
    var tw by rememberSaveable { mutableStateOf("") }
    var twSeen by rememberSaveable { mutableStateOf(false) }
    var weeks by rememberSaveable { mutableIntStateOf(s.lengthWeeks.coerceIn(1, 52)) }
    var start by rememberSaveable { mutableStateOf(ProgramEngine.programDay(LocalDateTime.now(), s.resetHour).toString()) }
    var pro by rememberSaveable { mutableStateOf(if (again) s.protein.toString() else "") }
    var cal by rememberSaveable { mutableStateOf(if (again) s.caloriesP1.toString() else "") }
    var targetsSeen by rememberSaveable { mutableStateOf(again) }
    var remind by rememberSaveable { mutableStateOf(!again || s.weighReminder || s.pickReminder) }
    var time by rememberSaveable { mutableStateOf(s.weighInTime.toString()) }
    var theme by rememberSaveable { mutableStateOf(s.theme) }
    var sunday by rememberSaveable { mutableStateOf(s.prefs.weekStartsSunday) }
    var denied by rememberSaveable { mutableStateOf(false) }
    // The plan: which slots are eaten, the starter each logs by default ("b=lib_b;l=..."), vegetarian, where the lifts happen.
    var mealsOn by rememberSaveable { mutableStateOf(s.meals.joinToString(",") { it.id }) }
    var pickText by rememberSaveable { mutableStateOf("") }
    var veg by rememberSaveable { mutableStateOf(false) }
    var mealsSeen by rememberSaveable { mutableStateOf(false) }
    var gear by rememberSaveable { mutableStateOf(Starters.gear[0].id) }
    // The one problem on screen: which field, and what to do about it.
    var err by remember { mutableStateOf<Pair<String, String>?>(null) }
    var starting by rememberSaveable { mutableStateOf(false) }

    val v = if (climbs) Vocab.CLIMBING else Vocab.plain(trainName)
    val unitWord = if (kg) "kg" else "lb"
    fun lb(x: Double?) = x?.let { if (kg) it / Units.KG_PER_LB else it }
    val startDate = runCatching { LocalDate.parse(start) }.getOrDefault(LocalDate.now())
    val today = ProgramEngine.programDay(LocalDateTime.now(), s.resetHour)
    val reminderTime = runCatching { LocalTime.parse(time) }.getOrDefault(s.weighInTime)
    val on = mealsOn.split(',').filter { it.isNotBlank() }
    val picks = pickText.split(';').mapNotNull { e -> e.split('=').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap().filterKeys { it in on }
    fun refit() { pickText = Starters.fit(on, veg, pro.int() ?: s.protein, cal.int() ?: s.caloriesP1).entries.joinToString(";") { "${it.key}=${it.value}" } }
    val dayProtein = picks.values.sumOf { Starters.meal(it)?.protein ?: 0 }
    val dayCalories = picks.values.sumOf { Starters.meal(it)?.calories ?: 0 }
    val needsAsk = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun problem(of: Step): Pair<String, String>? {
        val a = sw.num(); val b = tw.num()
        return when (of) {
            Step.START -> when {
                a == null -> "sw" to "Type this morning's weight to go on. A guess is fine; fix it tomorrow."
                !Onboard.weightOk(a, kg) -> "sw" to "That doesn't read as ${if (kg) "kilograms" else "pounds"}. Check the unit above."
                else -> null
            }
            Step.GOAL -> when {
                b == null -> "tw" to "Type where you want to land."
                !Onboard.weightOk(b, kg) -> "tw" to "That doesn't read as ${if (kg) "kilograms" else "pounds"}."
                a != null && b >= a -> "tw" to "Set the goal below this morning's weight. Sendline runs a cut."
                else -> null
            }
            Step.PROTEIN -> when {
                pro.int()?.let { it in 40..400 } != true -> "pro" to "Protein between 40 and 400 g a day."
                cal.int()?.let { it in 1000..6000 } != true -> "cal" to "Calories between 1,000 and 6,000 a day."
                else -> null
            }
            else -> null
        }
    }

    // Defaults land as a step opens, from what was typed before it, and stay put once the step has been seen.
    fun prefill(to: Step) {
        val a = sw.num()
        if (to == Step.GOAL && !twSeen && a != null) tw = Units.fmt1(Onboard.goalFor(a, weeks, kg))
        if ((to == Step.PROTEIN || to == Step.READY) && !targetsSeen) {
            lb(tw.num())?.let { pro = Onboard.proteinFor(it).toString() }
            lb(a)?.let { cal = Onboard.caloriesFor(it).toString() }
        }
        // The starter day is fitted to the targets as the meals step opens, until it has been seen.
        if ((to == Step.MEALS || to == Step.READY) && !mealsSeen && !again) {
            val p = pro.int() ?: s.protein; val c = cal.int() ?: s.caloriesP1
            val slots = Starters.startingSlots(s.meals.map { it.id }, veg, p, c)
            mealsOn = slots.joinToString(",")
            pickText = Starters.fit(slots, veg, p, c).entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    fun go(to: Int) {
        val target = steps[to.coerceIn(steps.indices)]
        if (step == Step.GOAL && to > at) twSeen = true
        if (step == Step.PROTEIN && to > at) targetsSeen = true
        if (step == Step.MEALS && to > at) mealsSeen = true
        prefill(target)
        forward = to > at; err = null; at = to.coerceIn(steps.indices)
        if (target != Step.START) keyboard?.hide()
        haptics.tick()
    }

    fun advance() {
        if (toReady) { toReady = false; go(steps.indexOf(Step.READY)) } else go(at + 1)
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> denied = !granted; advance() }

    fun finish() {
        // A recap jump could have changed a number another step checked; the first one still wrong is opened.
        for (k in listOf(Step.START, Step.GOAL, Step.PROTEIN)) problem(k)?.let { go(steps.indexOf(k)); err = it; return }
        val w = weeks.coerceIn(1, 52)
        val startWeight = lb(sw.num())!!
        val targetWeight = lb(tw.num())!!
        val future = startDate.isAfter(today)
        val dayN = ChronoUnit.DAYS.between(startDate, today) + 1
        if (preview) {
            vm.land(Takeover.Start("Preview · nothing saved", v.startCall, "This is where a new route lands on Today. Nothing you typed was kept.", "Back"), ripple = false)
            onDone(); return
        }
        if (starting) return   // a second tap while the first save runs does nothing
        starting = true
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val landing = Takeover.Start(
            eyebrow = if (future) "Starts ${Dates.wmd(startDate)}" else "Day $dayN of ${w * 7}",
            big = v.startCall,
            sub = when {
                future -> "Nothing to do until then." + if (remind) " The weigh-in nudge starts that morning." else ""
                again -> "This morning's weigh-in is in. Your xp and badges came with you. Pick what kind of day today is."
                else -> "This morning's weigh-in is in and ticked. Now pick what kind of day today is."
            },
            button = if (future) "Got it" else "Pick a day",
        )
        vm.updateSettings(then = {
            vm.repo.scope.launch { if (!future) vm.repo.logWeight(startWeight) }
            vm.land(landing)
            onDone()
        }) { s -> s.copy(
            onboarded = true, phase = 1, startWeight = startWeight, targetWeight = targetWeight,
            startDate = startDate, lengthWeeks = w, weighInTime = reminderTime,
            caloriesP1 = cal.int() ?: s.caloriesP1, protein = pro.int() ?: s.protein,
            checkpoints = ProgramSpec.defaultCheckpoints(startDate, startWeight, targetWeight, w),
            welcomeBackSeen = null, lastOpenDate = null, training = if (climbs) null else trainName.trim(), kg = kg,
            // Onboarding taught the wall, so the guide does not open itself over the first Today.
            tourSeen = true,
            weighReminder = if (!remind) false else s.weighReminder || !s.pickReminder,
            pickReminder = if (!remind) false else s.pickReminder || !s.weighReminder,
            // A second route keeps the look and the week it already had, and any update note still due.
            theme = if (again) s.theme else theme,
            prefs = if (again) s.prefs else s.prefs.copy(weekStartsSunday = sunday),
            seenVersion = if (again) s.seenVersion else version ?: s.seenVersion,
        ).let { n -> if (again) n else Starters.apply(n, on, picks, veg, gear) } }
    }

    fun next() {
        problem(step)?.let { err = it; return }
        when (step) {
            Step.NUDGE -> if (remind && needsAsk && !preview) { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS); return }
            Step.READY -> { finish(); return }
            else -> Unit
        }
        advance()
    }

    // Back walks the steps before it leaves. A second route can be called off at the first step; a preview just leaves.
    BackHandler(enabled = at > 0) { toReady = false; go(at - 1) }
    BackHandler(enabled = again && at == 0) { vm.updateSettings { s -> s.copy(onboarded = true) } }
    BackHandler(enabled = preview && at == 0, onBack = onDone)

    // Moving phones: a backup brings the route, the logs and the settings back in one step, no set-up first.
    var restoring by rememberSaveable { mutableStateOf(false) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        restoring = true
        val resolver = context.contentResolver
        // App lifetime, not this screen's: the restore flips onboarding, which can take this screen away mid-way.
        vm.repo.scope.launch {
            val ok = runCatching { resolver.openInputStream(uri)!!.use { vm.repo.restoreFrom(it) } }.isSuccess
            val onboarded = vm.repo.settings.current().onboarded
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                restoring = false
                when {
                    !ok -> vm.showToast("Could not read that file. Nothing changed.")
                    onboarded -> { vm.showToast("Restored. Everything from the backup is in place."); onDone() }
                    else -> vm.showToast("Restored. Set up the next route.")
                }
            }
        }
    }
    val swFocus = remember { FocusRequester() }
    val calm = CutTheme.calm

    // The look picked on the last step shows at once; it is only saved when the route starts.
    val palette = remember(s.palette, s.paletteByCalendar, s.customLooks) { Palettes.of(s, LocalDate.now()) }
    CutTheme(ThemeMode.of(theme), palette, LocalTime.now(), highContrast = s.prefs.highContrast) {
    CompositionLocalProvider(LocalVocab provides v, LocalForward provides forward) {
    val c = CutTheme.colors
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
        if (step != Step.HELLO) Column(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (preview) PreviewTag()
            Climb(steps.size, at, onJump = { toReady = false; go(it) })
        }
        AnimatedContent(
            step, Modifier.weight(1f), label = "step",
            transitionSpec = {
                // Forward climbs: the next step comes up from below on a spring while the last lifts away; back lowers.
                if (calm) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                else (androidx.compose.animation.slideInVertically(spring(dampingRatio = 0.86f, stiffness = 320f)) { if (forward) it / 6 else -it / 6 } + fadeIn(tween(220, delayMillis = 40))) togetherWith
                    (androidx.compose.animation.slideOutVertically(tween(200)) { if (forward) -it / 10 else it / 10 } + fadeOut(tween(140)))
            },
        ) { shown ->
            if (shown == Step.HELLO) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (again) Wall(st, st.logs.maxOfOrNull { it.date }?.takeIf { it <= st.today } ?: st.today, rememberScrollState(), onPickDay = {}, onWeek = {}, onInfo = null, heightScale = 0.72f)
                else DemoWall(s)
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (preview) PreviewTag()
                    if (again) {
                        Eyebrow("${v.sendCount(st.sends)} · chain ${st.chain.count} · ${v.level(st.level)}", color = c.accent)
                        Big("Next route.")
                        Body("Your last week on the wall. Every log, badge and xp stays; set a new start, a new goal and a new length.")
                    } else {
                        Eyebrow("Sendline", color = c.accent)
                        Big(if (weeks == 12) "Twelve weeks.\nFour taps a day." else "$weeks weeks.\nFour taps a day.")
                        Body("Weigh in, eat the script, do the block, tick it off. The app keeps score so you don't have to. Nothing here resets, ever.")
                        Text(
                            if (restoring) "RESTORING…" else "MOVING PHONES? RESTORE A BACKUP",
                            color = c.accent, style = cutStyle(11, 800, 1f, 0.08f),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = !restoring, role = Role.Button) {
                                if (preview) { vm.showToast("Preview: restoring is off here."); return@clickable }
                                restore.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain", "*/*"))
                            }.padding(vertical = 14.dp),
                        )
                    }
                }
            } else Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (shown) {
                    Step.WORDS -> {
                        Big("Do you climb?")
                        Body("Sendline talks like a bouldering gym. If you don't climb, it can talk plainly instead. Same wall, same rules.")
                        Choice(climbs, HoldShape.CRIMP, "I climb", "Climb days, Sends and V grades on a wall of holds.") { climbs = true }
                        Choice(!climbs, HoldShape.JUG, "Something else", "Plain words: training days, full days and levels.") { climbs = false }
                        if (!climbs) Fld("What do you call your training?") {
                            Field(trainName, { trainName = it }, Modifier.fillMaxWidth(), height = 56.dp, fontSize = 22, keyboard = KeyboardType.Text, placeholder = "Run, Gym, Practice", radius = 12.dp, padding = 16.dp, background = c.surface, onDone = ::next)
                        }
                        Hint("Today will read: ${v.trainDay}, ${v.session}, a ${v.send}. Switch it any time on You, under App.")
                    }
                    Step.START -> {
                        Big("Where you're starting.")
                        LaunchedEffect(Unit) { delay(280); runCatching { swFocus.requestFocus() } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Switching converts what is typed, so a number is never read in the wrong unit.
                            fun to(k: Boolean) {
                                if (k == kg) return
                                val f = if (k) Units.KG_PER_LB else 1 / Units.KG_PER_LB
                                sw.num()?.let { sw = Units.fmt1(it * f) }; tw.num()?.let { tw = Units.fmt1(it * f) }
                                kg = k; err = null
                            }
                            Pill("Pounds", on = !kg, onClick = { to(false) })
                            Pill("Kilograms", on = kg, onClick = { to(true) })
                        }
                        Fld("This morning, before food") {
                            Big(sw, { sw = it; err = null }, err?.first == "sw", focus = swFocus, placeholder = if (kg) "82.5" else "181.8", onDone = ::next)
                        }
                        Hint(err?.second ?: "After the bathroom, before breakfast. A guess is fine; fix it tomorrow.", alert = err != null)
                    }
                    Step.GOAL -> {
                        Big("Where you want to land.")
                        Fld("Goal, $unitWord") {
                            Big(tw, { tw = it; twSeen = true; err = null }, err?.first == "tw", onDone = { keyboard?.hide() })
                        }
                        Fld("How long") {
                            MiniStepper("$weeks", onMinus = { if (weeks > 1) weeks-- }, onPlus = { if (weeks < 52) weeks++ }, unit = if (weeks == 1) "week" else "weeks", onType = { weeks = it.roundToInt().coerceIn(1, 52) })
                        }
                        StartLine(startDate, today) { picked -> start = picked.toString() }
                        val a = sw.num(); val b = tw.num()
                        if (err != null) Hint(err!!.second, alert = true)
                        else if (a != null && b != null && b < a) Projection(a, b, weeks, startDate, unitWord, v) { weeks = it }
                    }
                    Step.PROTEIN -> {
                        Big("Protein first.")
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Hold(HoldShape.JUG, c.goodDim, 44.dp, mark = DayResult.MIN)
                            Body("On a cut, protein is what keeps ${v.keeps}. Weigh in and hit it, and the day is a Min day: the chain holds even when nothing else gets done.")
                        }
                        Fld("Protein, grams a day") {
                            Big(pro, { pro = it; targetsSeen = true; err = null }, err?.first == "pro", keyboard = KeyboardType.Number, imeAction = ImeAction.Next)
                        }
                        Fld("Calories a day") {
                            Field(cal, { cal = it; targetsSeen = true; err = null }, Modifier.fillMaxWidth(), height = 56.dp, fontSize = 24, keyboard = KeyboardType.Number, radius = 12.dp, padding = 18.dp, background = c.surface, border = if (err?.first == "cal") c.accent else c.mutedBorder, onDone = ::next)
                        }
                        Hint(
                            err?.second ?: ((if (again) "Your targets from the last route. " else "Started from your weights: about ${if (kg) "2 g a kilo" else "0.9 g a pound"} of goal, and ${if (kg) "26 calories a kilo" else "12 calories a pound"} of you now. A common start, not a prescription. ") +
                                "Change both any time on ${v.beta}, under Targets. Not medical advice; check with a doctor if you have a condition, and stop if you feel unwell."),
                            alert = err != null,
                        )
                    }
                    Step.MEALS -> {
                        Big("A day of food.")
                        val target = pro.int() ?: s.protein
                        ProteinLine(dayProtein, target, dayCalories, on.size)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Vegetarian", on = veg, onClick = { veg = !veg; mealsSeen = true; refit() })
                        }
                        for (m in s.meals) {
                            val isOn = m.id in on
                            Card(Modifier.fillMaxWidth()) {
                                SwitchRow(m.name, if (isOn) "around ${m.time}" else "skipped", isOn) {
                                    mealsSeen = true
                                    mealsOn = s.meals.map { it.id }.filter { if (it == m.id) !isOn else it in on }.joinToString(",")
                                    refit()
                                }
                                if (isOn) {
                                    val options = Starters.options(m.id, veg)
                                    val chosen = options.indexOfFirst { it.id == picks[m.id] }
                                    val row = androidx.compose.foundation.lazy.rememberLazyListState()
                                    // A pick the fit made off to the right slides into view, so every slot shows what it logs.
                                    LaunchedEffect(chosen) {
                                        if (chosen >= 0 && row.layoutInfo.visibleItemsInfo.none { it.index == chosen && it.offset >= 0 && it.offset + it.size <= row.layoutInfo.viewportEndOffset }) {
                                            if (calm) row.scrollToItem(chosen) else row.animateScrollToItem(chosen)
                                        }
                                    }
                                    androidx.compose.foundation.lazy.LazyRow(
                                        Modifier.fillMaxWidth().padding(bottom = 14.dp), state = row,
                                        contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(options.size) { k ->
                                            val o = options[k]
                                            Chip("${o.name} · ${o.protein} g", on = k == chosen, Modifier.semantics { selected = k == chosen }) {
                                                mealsSeen = true
                                                pickText = (picks + (m.id to o.id)).entries.joinToString(";") { "${it.key}=${it.value}" }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Hint("Picked to land near your protein; tap another to swap. Numbers are typical portions, rounded. Every one of them, and anything you add, lives on ${v.beta}, under Meals.")
                    }
                    Step.LIFTS -> {
                        Big("Where do you lift?")
                        Body("The block after a ${v.trainDay.lowercase()}'s session, and the short one on an off day. Rest days never lift.")
                        for ((g, shape) in Starters.gear.zip(listOf(HoldShape.CRIMP, HoldShape.JUG, HoldShape.SLOPER, HoldShape.PINCH))) {
                            // Mid-sentence, a name drops its capital unless it is an initialism: "push-ups", "DB floor press".
                            fun names(ids: List<String>) = ids.mapNotNull { Starters.lift(it)?.name?.let { n -> if (n.length > 1 && n[1].isUpperCase()) n else n.replaceFirstChar(Char::lowercase) } }.joinToString(", ")
                            Choice(gear == g.id, shape, g.label, if (g.train.isEmpty() && g.off.isEmpty()) "Just the script: weigh in, eat, and the ${v.session.lowercase()}." else "${v.trainDay}: ${names(g.train)}. Off day: ${names(g.off)}.") { gear = g.id }
                        }
                        Hint("Sets, reps and weights for each lift live on ${v.beta}, under Lifts.")
                    }
                    Step.NUDGE -> {
                        Big("One nudge a day.")
                        Card(Modifier.fillMaxWidth()) {
                            SwitchRow("Remind me", if (remind) "each goes quiet once it's done" else "off; nothing will ever buzz", remind) { remind = !remind }
                        }
                        if (remind) Fld("Weigh-in reminder") {
                            PickerBox(Clock.text(context, reminderTime), "Weigh-in reminder, ${Clock.text(context, reminderTime)}. Change") {
                                TimePickerDialog(context, { _, h, m -> time = LocalTime.of(h, m).toString() }, reminderTime.hour, reminderTime.minute, Clock.is24(context)).show()
                            }
                        }
                        Body(if (remind) "The weigh-in nudge comes in the morning. If no day is picked by ${Clock.text(context, s.pickTime)}, a second one asks which kind it was. Nothing about streaks, ever." else "No notifications at all. Turn either one on later on You, under Nudges.")
                        when {
                            !remind -> Unit
                            denied -> Hint("The phone said no, so nothing will show. You, under Nudges, can allow them later.", alert = true)
                            preview && needsAsk -> Hint("Preview: the phone's ask is skipped here.")
                            needsAsk -> Hint("Next, the phone asks whether Sendline may send notifications.")
                        }
                    }
                    else -> {
                        Big(if (again) "The next route." else "Your route.")
                        val a = sw.num(); val b = tw.num()
                        val end = startDate.plusDays(weeks * 7L)
                        Card(Modifier.fillMaxWidth()) {
                            Recap("Start", "${a?.let { Units.fmt1(it) } ?: "?"} $unitWord, ${if (startDate == today) "today" else Dates.wmd(startDate)}") { toReady = true; go(steps.indexOf(Step.START)) }
                            Recap("Goal", "${b?.let { Units.fmt1(it) } ?: "?"} $unitWord by ${Dates.wmd(end)}, $weeks ${if (weeks == 1) "week" else "weeks"}") { toReady = true; go(steps.indexOf(Step.GOAL)) }
                            Recap("Every day", "${pro.int() ?: "?"} g protein, ${cal.int()?.let { "%,d".format(Locale.US, it) } ?: "?"} calories") { toReady = true; go(steps.indexOf(Step.PROTEIN)) }
                            if (!again) {
                                Recap("Meals", "${count(on.size, "meal")} a day, $dayProtein of ${pro.int() ?: "?"} g protein") { toReady = true; go(steps.indexOf(Step.MEALS)) }
                                Recap("Lifts", Starters.gear.first { it.id == gear }.label.lowercase()) { toReady = true; go(steps.indexOf(Step.LIFTS)) }
                            }
                            Recap("Nudge", if (!remind) "off" else "weigh-in at ${Clock.text(context, reminderTime)}" + if (denied) ", phone says no" else "", last = again) { toReady = true; go(steps.indexOf(Step.NUDGE)) }
                            if (!again) Recap("Words", if (climbs) "climbing" else "plain, ${v.session}", last = true) { toReady = true; go(steps.indexOf(Step.WORDS)) }
                        }
                        if (a != null && b != null) {
                            val cps = ProgramSpec.defaultCheckpoints(startDate, lb(a)!!, lb(b)!!, weeks).count { !it.free }
                            Hint("${weeks * 7} ${v.holds} on the route and $cps checkpoints on the way. Tap a line to change it.")
                        }
                        if (!again) {
                            Fld("Look") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for ((id, label) in listOf("system" to "Phone", "dark" to "Dark", "light" to "Light")) Pill(label, on = theme == id, onClick = { theme = id })
                                }
                            }
                            Fld("Week starts") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Pill("Monday", on = !sunday, onClick = { sunday = false })
                                    Pill("Sunday", on = sunday, onClick = { sunday = true })
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                at == 0 && preview -> BigButton("Exit", onClick = onDone, modifier = Modifier.width(120.dp), secondary = true)
                at == 0 && again -> BigButton("Not yet", onClick = { vm.updateSettings { s -> s.copy(onboarded = true) } }, modifier = Modifier.width(120.dp), secondary = true)
                at > 0 -> BigButton("Back", onClick = { toReady = false; go(at - 1) }, modifier = Modifier.width(120.dp), secondary = true)
            }
            val label = when {
                step == Step.HELLO -> "Let's go"
                step == Step.READY -> "Start the route"
                step == Step.NUDGE && remind && needsAsk && !preview -> "Allow reminders"
                toReady -> "Done"
                else -> "Next"
            }
            BigButton(label, onClick = ::next, modifier = Modifier.weight(1f))
        }
    }
    }
    }
}

/** The numbers behind the defaults and the pace line, kept pure so they are tested. */
internal object Onboard {
    /** Typed weights a person could be, in the unit picked: anything else is the wrong unit or a typo. */
    fun weightOk(v: Double, kg: Boolean) = if (kg) v in 40.0..320.0 else v in 90.0..700.0

    /** A goal at about half a percent a week, a round number in the unit typed. */
    fun goalFor(start: Double, weeks: Int, kg: Boolean): Double {
        val g = start * (1 - 0.005 * weeks.coerceIn(1, 52))
        return if (kg) (g * 2).roundToInt() / 2.0 else g.roundToInt().toDouble()
    }

    /** About 0.9 g a pound of goal weight, to the nearest 5. */
    fun proteinFor(goalLb: Double) = ((goalLb * 0.9 / 5).roundToInt() * 5).coerceIn(80, 300)

    /** About 12 calories a pound of start weight, to the nearest 50. */
    fun caloriesFor(startLb: Double) = ((startLb * 12 / 50).roundToInt() * 50).coerceIn(1200, 4000)

    enum class Pace { UP, GENTLE, STEADY, BRISK, FAST }

    /** Percent of the start weight lost a week. */
    fun percentAWeek(start: Double, goal: Double, weeks: Int) = (start - goal) / weeks.coerceAtLeast(1) / start * 100

    fun pace(start: Double, goal: Double, weeks: Int): Pace = percentAWeek(start, goal, weeks).let { p ->
        when { p <= 0 -> Pace.UP; p < 0.25 -> Pace.GENTLE; p <= 0.75 -> Pace.STEADY; p <= 1.0 -> Pace.BRISK; else -> Pace.FAST }
    }

    /** The fewest weeks that keep the cut at 1% a week or under. */
    fun weeksForOnePercent(start: Double, goal: Double) = ceil((start - goal) / (start * 0.01) - 1e-9).toInt().coerceIn(1, 52)
}

/** The line from start to goal, said back: where it lands, how fast, and whether that pace is kind. */
@Composable
private fun Projection(a: Double, b: Double, weeks: Int, startDate: LocalDate, unit: String, v: Vocab, onWeeks: (Int) -> Unit) {
    val c = CutTheme.colors
    val pace = Onboard.pace(a, b, weeks)
    val pct = Onboard.percentAWeek(a, b, weeks)
    Card(Modifier.fillMaxWidth(), border = if (pace == Onboard.Pace.FAST) c.accent else c.hairline, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${Units.fmt1(b)} $unit by ${Dates.wmd(startDate.plusDays(weeks * 7L))}", color = c.text, style = cutStyle(22, 800, 1.1f, -0.01f, tabular = true))
            Text(
                "${"%.2f".format(Locale.US, (a - b) / weeks)} $unit a week · ${"%.1f".format(Locale.US, pct)}% of you",
                color = c.text2, style = cutStyle(13, 600, 1.2f, tabular = true),
            )
            Text(
                when (pace) {
                    Onboard.Pace.GENTLE -> "Gentle. Slow to see, easy to keep."
                    Onboard.Pace.STEADY -> "Steady. The pace that keeps ${v.keeps}."
                    Onboard.Pace.BRISK -> "Brisk. Doable; protein matters most at this pace."
                    else -> "Fast. Past 1% a week, ${v.keeps} tends to go with the fat."
                },
                color = if (pace == Onboard.Pace.FAST) c.accent else c.good, style = cutStyle(13, 700, 1.35f),
            )
            if (pace == Onboard.Pace.FAST) {
                val n = Onboard.weeksForOnePercent(a, b)
                if (n > weeks) Text(
                    "MAKE IT $n WEEKS", color = c.accent, style = cutStyle(11, 800, 1f, 0.08f),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { onWeeks(n) }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

/** When the route starts: today unless changed, a month back to three months ahead. */
@Composable
private fun StartLine(start: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val words = when { start == today -> "Starts today"; start.isAfter(today) -> "Starts ${Dates.wmd(start)}"; else -> "Started ${Dates.wmd(start)}" }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(words, color = c.text, style = cutStyle(15, 700, 1.2f), modifier = Modifier.weight(1f, fill = false))
        Text(
            "CHANGE", color = c.accent, style = cutStyle(11, 800, 1f, 0.08f),
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClickLabel = "Change the start date") {
                DatePickerDialog(context, { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) }, start.year, start.monthValue - 1, start.dayOfMonth).apply {
                    datePicker.minDate = today.minusDays(30).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    datePicker.maxDate = today.plusDays(90).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.show()
            }.padding(horizontal = 8.dp, vertical = 14.dp),
        )
    }
}

/**
 * The progress, as a short climb: one hold a step, green once answered, the current one ringed, the rope following.
 * A green hold goes back to its step.
 */
@Composable
private fun Climb(n: Int, at: Int, onJump: (Int) -> Unit) {
    val c = CutTheme.colors
    val calm = CutTheme.calm
    val shapes = listOf(HoldShape.JUG, HoldShape.CRIMP, HoldShape.SLOPER, HoldShape.JUG, HoldShape.CRIMP, HoldShape.SLOPER, HoldShape.CRIMP)
    val hold = 22.dp
    val rise = 18.dp
    BoxWithConstraints(Modifier.fillMaxWidth().height(hold + rise + 12.dp).semantics { contentDescription = "Step ${at + 1} of $n" }) {
        val gap = (maxWidth - hold) / (n - 1)
        fun x(i: Int) = gap * i
        fun y(i: Int) = rise * (1f - i / (n - 1f)) + 6.dp
        // The rope follows on a spring: a step answered clips that hold green, then the rope (and a chalk dot at its
        // end, the climber) travels up to the next one. Back lowers it the same way.
        val rope by androidx.compose.animation.core.animateFloatAsState(
            at.toFloat(), if (calm) snap() else spring(dampingRatio = 0.82f, stiffness = 170f), label = "climbRope",
        )
        Canvas(Modifier.fillMaxSize()) {
            fun center(i: Int) = Offset((x(i) + hold / 2).toPx(), (y(i) + hold / 2).toPx())
            for (i in 1 until n) {
                val t = (rope - (i - 1)).coerceIn(0f, 1f)
                if (t <= 0f) break
                drawLine(
                    c.accent.copy(alpha = 0.95f), center(i - 1), center(i - 1) + (center(i) - center(i - 1)) * t,
                    3.dp.toPx(), StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 7.dp.toPx())),
                )
            }
            val k = rope.toInt().coerceIn(0, n - 1)
            val tip = if (k >= n - 1) center(n - 1) else center(k) + (center(k + 1) - center(k)) * (rope - k)
            drawCircle(c.accent, 4.dp.toPx(), tip)
        }
        for (i in 0 until n) {
            val fill by animateColorAsState(when { i < at -> c.good; i == at -> c.surface; else -> c.hairline }, tween(if (calm) 0 else 300), label = "climbHold")
            val pop = remember { Animatable(1f) }
            LaunchedEffect(i < at) { if (i < at && !calm) { pop.snapTo(0.6f); pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) } }
            Box(
                Modifier.offset(x(i) - 11.dp, y(i) - 11.dp).size(44.dp)
                    .then(if (i < at) Modifier.clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClickLabel = "Go back to it") { onJump(i) }.semantics { contentDescription = "Step ${i + 1}, done" } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Hold(shapes[i % shapes.size], fill, hold, Modifier.scale(pop.value), stroke = if (i == at) c.accent else null, strokeWidth = if (i == at) 2.dp else 0.dp)
            }
        }
    }
}

/**
 * A week played on the real wall: a Send, a Min day, a frozen miss, a rest-day Send, and the open day the rope runs to.
 * Each hold explains itself when tapped. Calm motion shows the whole week at once.
 */
@Composable
private fun DemoWall(s: Settings) {
    val c = CutTheme.colors
    val v = CutTheme.vocab
    val calm = CutTheme.calm
    val mon = remember { ProgramEngine.weekStart(LocalDate.now()) }
    val days = remember(mon) { (0 until 7).map { mon.plusDays(it.toLong()) } }
    val story = listOf(DayMode.CLIMB to DayResult.SEND, DayMode.OFF to DayResult.MIN, null to DayResult.FREEZE, DayMode.REST to DayResult.SEND, null to DayResult.TODAY)
    var beat by rememberSaveable { mutableIntStateOf(if (calm) story.size else 0) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    val haptics = LocalHaptics.current
    // Each beat is one move up the wall: the rope reaches out (the newest segment draws in) as the hold lands and
    // pops, a light tick under the thumb, then a pause to read the line before the next move.
    val reach = remember { Animatable(1f) }
    LaunchedEffect(calm) {
        if (calm) { beat = story.size; return@LaunchedEffect }
        delay(650)
        while (beat < story.size && picked == null) {
            reach.snapTo(0f)
            beat++
            haptics.tick()
            reach.animateTo(1f, tween(460, easing = androidx.compose.animation.core.EaseOutCubic))
            delay(1250)
        }
    }
    val shownBeat = if (picked != null) story.size else beat
    val results = buildMap {
        for (i in 0 until shownBeat.coerceAtMost(4)) put(days[i], story[i].second)
        // The open day joins the rope only at the end, so the rope never skips the days still to play.
        if (shownBeat >= story.size) put(days[4], DayResult.TODAY)
    }
    val logs = (0 until shownBeat.coerceAtMost(4)).mapNotNull { i -> story[i].first?.let { DayLog(days[i], mode = it) } }
    val st = AppState(settings = s.copy(startDate = mon), today = days[4], results = results, logs = logs, loaded = true)
    val focus = picked ?: if (beat in 1..story.size) beat - 1 else null
    // Before the first move the chalk waits on Monday, where the climb starts; after the story it rests on the open day.
    Wall(
        st, days[focus ?: if (shownBeat >= story.size) 4 else 0], rememberScrollState(), onPickDay = { d -> days.indexOf(d).takeIf { it in story.indices }?.let { picked = it; haptics.tick() } },
        onWeek = {}, onInfo = null, heightScale = 0.72f, ropeDraw = if (picked == null) reach.value else 1f,
        popOn = setOf(DayResult.SEND, DayResult.MIN, DayResult.FREEZE, DayResult.TODAY),
    )
    Column(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .semantics(mergeDescendants = true) { if (picked != null) liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The line climbs in from below with each move and the old one lifts away.
        AnimatedContent(
            focus, label = "demoLine",
            transitionSpec = {
                if (calm) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (androidx.compose.animation.slideInVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) { it / 2 } + fadeIn(tween(220))) togetherWith
                    (androidx.compose.animation.slideOutVertically(tween(180)) { -it / 3 } + fadeOut(tween(140)))
            },
        ) { f ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (f != null) {
                    val (mode, r) = story[f]
                    val d = days[f]
                    val parts = listOfNotNull("${d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)} ${d.dayOfMonth}", mode?.let { v.day(it) }, resultName(r, v))
                    Eyebrow(parts.joinToString(" · "), color = if (r == DayResult.TODAY) c.accent else c.text2)
                }
                Text(
                    when (f) {
                        null -> v.onboardHold
                        0 -> "Every item ticked: a ${v.send}. The ${v.hold} goes green."
                        1 -> "Weighed in and hit protein, nothing else: a Min day. Still on the chain."
                        2 -> "Missed. A freeze covers it, two a month, so the chain holds."
                        3 -> "A rest day, script done: another ${v.send}."
                        else -> "Still open. The rope is your chain, and it never resets."
                    },
                    color = c.text, style = cutStyle(14, 600, 1.35f),
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(shownBeat >= story.size, enter = fadeIn(tween(if (calm) 0 else 400, delayMillis = if (calm) 0 else 300))) {
            Text("Tap a hold to read it.", color = c.text2, style = cutStyle(12, 500, 1.3f))
        }
    }
}

/** The day's protein against the target: a bar that turns green near it, and a line on how close. */
@Composable
private fun ProteinLine(protein: Int, target: Int, calories: Int, meals: Int) {
    val c = CutTheme.colors
    val off = protein - target
    val near = kotlin.math.abs(off) <= (target * 0.04).roundToInt().coerceAtLeast(5)
    val fill by androidx.compose.animation.core.animateFloatAsState((protein.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f), tween(if (CutTheme.calm) 0 else 420), label = "dayProtein")
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$protein of $target g protein · ${"%,d".format(Locale.US, calories)} cal", color = c.text, style = cutStyle(17, 800, 1.1f, tabular = true))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.hairline)) {
            Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (near) c.good else c.accent))
        }
        Text(
            when {
                meals == 0 -> "No meals on. Protein is still the target; log what you eat as you go."
                near -> "Right on your target."
                off > 0 -> "$off g over your target. Skip a meal or swap a lighter one if that's too much."
                else -> "${-off} g under. A shake or a bigger portion covers it."
            },
            color = if (near) c.good else c.text2, style = cutStyle(12, 600, 1.3f),
        )
    }
}

/** One of two answers to a question, the whole card the target. */
@Composable
private fun Choice(on: Boolean, shape: HoldShape, title: String, sub: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val r = RoundedCornerShape(12.dp)
    Row(
        Modifier.rise(110).fillMaxWidth().clip(r).background(c.surface).border(2.dp, if (on) c.accent else c.hairline, r)
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Hold(shape, if (on) c.good else c.mutedBorder, 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = c.text, style = cutStyle(17, 800, 1.1f))
            Text(sub, color = c.text2, style = cutStyle(13, 500, 1.35f))
        }
    }
}

/** A line of the recap: what it is, what was picked, and a tap back to that step. */
@Composable
private fun Recap(label: String, value: String, last: Boolean = false, onClick: () -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.clickable(role = Role.Button, onClickLabel = "Change $label", onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label.uppercase(), color = c.text2, style = cutStyle(10, 800, 1f, 0.1f), modifier = Modifier.width(78.dp))
            Text(value, color = c.text, style = cutStyle(14, 700, 1.3f, tabular = true), modifier = Modifier.weight(1f))
            Text("›", color = c.text2, style = cutStyle(16, 700, 1f))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

@Composable
private fun PreviewTag() = Text("PREVIEW · NOTHING IS SAVED", color = CutTheme.colors.accent, style = cutStyle(10, 800, 1f, 0.1f))

@Composable
private fun Big(text: String) = Text(text, color = CutTheme.colors.text, style = cutStyle(40, 800, 1f, -0.02f, display = true), modifier = Modifier.rise(0).semantics { heading() })

/** Which way the last step change went; the pieces of a step rise into place from that side. */
private val LocalForward = androidx.compose.runtime.staticCompositionLocalOf { true }

/**
 * A piece of a step settling into place a beat after the headline: [delayMs] staggers the headline, the words, the
 * fields and the hints so a step lands top to bottom like weight coming onto a hold. Calm motion shows it at once.
 */
private fun Modifier.rise(delayMs: Int): Modifier = composed {
    if (CutTheme.calm) return@composed this
    val up = LocalForward.current
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 260f)) }
    this.graphicsLayer { alpha = a.value.coerceIn(0f, 1f); translationY = (1f - a.value) * (if (up) 22.dp else (-22).dp).toPx() }
}

/** The big number field: 64dp, 32sp, the border turns accent when it is the problem. */
@Composable
private fun Big(
    value: String, onChange: (String) -> Unit, wrong: Boolean, focus: FocusRequester? = null, placeholder: String? = null,
    keyboard: KeyboardType = KeyboardType.Decimal, imeAction: ImeAction = ImeAction.Done, onDone: () -> Unit = {},
) {
    val c = CutTheme.colors
    Field(value, onChange, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, keyboard = keyboard, imeAction = imeAction, placeholder = placeholder, radius = 12.dp, padding = 18.dp, background = c.surface, border = if (wrong) c.accent else c.mutedBorder, focusRequester = focus, onDone = onDone)
}

@Composable
private fun Body(text: String) = Text(text, color = CutTheme.colors.text2, style = cutStyle(15, 500, 1.45f), modifier = Modifier.rise(60).widthIn(max = 340.dp))

@Composable
private fun Hint(text: String, alert: Boolean = false) = Text(
    text, color = if (alert) CutTheme.colors.accent else CutTheme.colors.text2, style = cutStyle(12, 500, 1.35f, tabular = true),
    modifier = Modifier.rise(180).then(if (alert) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
)

@Composable
private fun Fld(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.rise(110), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = CutTheme.colors.text2, style = cutStyle(11, 700, 1f, 0.12f))
        content()
    }
}

@Composable
private fun PickerBox(label: String, description: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(c.surface).border(2.dp, c.mutedBorder, shape)
            .clickable(onClick = onClick, role = Role.Button).semantics { contentDescription = description }.padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, color = c.text, style = cutStyle(22, 800, 1f, tabular = true)) }
}
