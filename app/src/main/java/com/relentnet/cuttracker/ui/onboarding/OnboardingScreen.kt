package com.relentnet.cuttracker.ui.onboarding

import com.relentnet.cuttracker.ui.components.int
import com.relentnet.cuttracker.ui.components.num
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.launch
import com.relentnet.cuttracker.data.restoreFrom
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)

/** Four steps. Finishing writes the program settings, logs the first weigh-in and lands on Today. */
@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Back walks the steps backwards before it leaves.
    androidx.activity.compose.BackHandler(enabled = step > 0) { step-- }
    var sw by rememberSaveable { mutableStateOf("") }
    var tw by rememberSaveable { mutableStateOf(ProgramEngine.fmt1(s.targetWeight)) }
    var start by rememberSaveable { mutableStateOf(com.relentnet.cuttracker.engine.ProgramEngine.programDay(java.time.LocalDateTime.now(), s.resetHour).toString()) }
    var weeks by rememberSaveable { mutableStateOf(s.lengthWeeks.toString()) }
    var time by rememberSaveable { mutableStateOf(s.weighInTime.toString()) }
    var cal by rememberSaveable { mutableStateOf("") }
    var pro by rememberSaveable { mutableStateOf("") }
    // Starting the next route keeps every log, badge and XP; only the program changes.
    val again = st.logs.isNotEmpty() || st.weighIns.isNotEmpty()
    var starting by rememberSaveable { mutableStateOf(false) }
    // A second route can be called off at the first step: back returns to the route that just ended.
    androidx.activity.compose.BackHandler(enabled = again && step == 0) { vm.updateSettings { s -> s.copy(onboarded = true) } }
    var swMissing by remember { mutableStateOf(false) }
    // Moving phones: a backup brings the route, the logs and the settings back in one step, no set-up first.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var restoring by rememberSaveable { mutableStateOf(false) }
    val restore = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
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

    fun next() {
        when (step) {
            1 -> if (sw.num() == null) { swMissing = true; swFocus.requestFocus(); return }
            2 -> {
                // Prefill the targets once, from the weights: a common starting point, not a prescription.
                val a = sw.num() ?: s.startWeight
                val b = tw.num() ?: s.targetWeight
                if (cal.isBlank()) cal = ((a * 12 / 50).toInt() * 50).coerceIn(1200, 4000).toString()
                if (pro.isBlank()) pro = ((b * 0.9 / 5).toInt() * 5).coerceIn(80, 300).toString()
            }
            4 -> {
                if (starting) return   // a second tap while the first save runs does nothing
                starting = true
                val startWeight = sw.num() ?: s.startWeight
                val targetWeight = tw.num() ?: s.targetWeight
                val startDate = runCatching { LocalDate.parse(start) }.getOrDefault(LocalDate.now())
                val w = weeks.int()?.coerceIn(1, 52) ?: 12
                vm.updateSettings(then = {
                    vm.repo.scope.launch { if (!startDate.isAfter(LocalDate.now())) vm.repo.logWeight(startWeight); vm.showToast("Day 1. Pick a day.") }
                    onDone()
                }) { s -> s.copy(
                        onboarded = true, phase = 1, startWeight = startWeight, targetWeight = targetWeight,
                        startDate = startDate, lengthWeeks = w, weighInTime = runCatching { LocalTime.parse(time) }.getOrDefault(s.weighInTime),
                        caloriesP1 = cal.int() ?: s.caloriesP1, protein = pro.int() ?: s.protein,
                        checkpoints = ProgramSpec.defaultCheckpoints(startDate, startWeight, targetWeight, w),
                        welcomeBackSeen = null, lastOpenDate = null,
                    ) }
                return
            }
        }
        step++
    }

    Column(
        Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.systemBars).padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..4) Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) c.accent else c.hairline))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (step) {
                0 -> {
                    Text("SENDLINE", color = c.accent, style = cutStyle(12, 800, 1f, 0.14f))
                    Big(if (again) "Next route." else "Twelve weeks.\nFour taps a day.")
                    Body(if (again) "Your logs, badges and xp stay. Set a new start, a new goal and a new length." else "Weigh in, eat the script, do the block, tick it off. The app keeps score so you don't have to. Nothing here resets, ever.")
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        HoldKey(HoldShape.CRIMP, "Climb day", "session + push block")
                        HoldKey(HoldShape.JUG, "Off day", "a short block at home")
                        HoldKey(HoldShape.SLOPER, "Rest day", "weigh in, eat the script")
                    }
                    Hint("Every day is a hold. Pick which kind, finish the script, and the hold goes green. Not medical advice; check with a doctor if you have a condition, and stop if you feel unwell.")
                    if (!again) Text(
                        if (restoring) "RESTORING…" else "MOVING PHONES? RESTORE A BACKUP",
                        color = c.accent, style = cutStyle(11, 800, 1f, 0.08f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = !restoring, role = androidx.compose.ui.semantics.Role.Button) {
                            restore.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain", "*/*"))
                        }.padding(vertical = 14.dp),
                    )
                }
                1 -> {
                    Big("Where you're starting.")
                    LaunchedEffect(Unit) { swFocus.requestFocus() }
                    Fld("This morning, before food") {
                        Field(sw, { sw = it; swMissing = false }, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, placeholder = "181.8", radius = 12.dp, padding = 18.dp, background = c.surface, border = if (swMissing) c.accent else c.mutedBorder, focusRequester = swFocus, imeAction = ImeAction.Next, onDone = { })
                    }
                    Fld("Where you want to land") {
                        Field(tw, { tw = it }, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, radius = 12.dp, padding = 18.dp, background = c.surface, onDone = ::next)
                    }
                    val a = sw.num(); val b = tw.num(); val w = (weeks.int() ?: 12).coerceIn(1, 52)
                    Hint(if (swMissing) "Type this morning's weight to go on. A guess is fine; fix it tomorrow." else if (a != null && b != null) "${ProgramEngine.fmt1(a - b)} lb over $w weeks is ${"%.2f".format(Locale.US, (a - b) / w)} lb a week." else "About 1 lb a week is the pace.", alert = swMissing)
                }
                2 -> {
                    Big("When it runs.")
                    val d = runCatching { LocalDate.parse(start) }.getOrDefault(LocalDate.now())
                    Fld("Start") {
                        PickerBox(d.toString()) { DatePickerDialog(context, { _, y, m, day -> start = LocalDate.of(y, m + 1, day).toString() }, d.year, d.monthValue - 1, d.dayOfMonth).show() }
                    }
                    Fld("Weeks") {
                        Field(weeks, { weeks = it }, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, radius = 12.dp, padding = 18.dp, background = c.surface, onDone = ::next)
                    }
                    val w = (weeks.int() ?: 12).coerceIn(1, 52)
                    Hint("Ends ${d.plusDays(w * 7L).format(longDate)}. ${w * 7} holds on the route.")
                }
                3 -> {
                    Big("The daily targets.")
                    Fld("Calories") {
                        Field(cal, { cal = it }, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, radius = 12.dp, padding = 18.dp, background = c.surface, imeAction = ImeAction.Next, onDone = { })
                    }
                    Fld("Protein, grams") {
                        Field(pro, { pro = it }, Modifier.fillMaxWidth(), height = 64.dp, fontSize = 32, radius = 12.dp, padding = 18.dp, background = c.surface, onDone = ::next)
                    }
                    Hint("Prefilled from your weights as a common starting point. Protein is the one that keeps the chain: weigh in and hit it, and the day counts even when the rest slips. Change both any time on Beta, under Targets.")
                }
                else -> {
                    Big("One nudge a day.")
                    val t = runCatching { LocalTime.parse(time) }.getOrDefault(s.weighInTime)
                    Fld("Weigh-in reminder") {
                        PickerBox(t.toString()) { TimePickerDialog(context, { _, h, m -> time = LocalTime.of(h, m).toString() }, t.hour, t.minute, true).show() }
                    }
                    Body("That's the daily one, and it stays quiet once you have weighed in. A second fires at 7pm only if no day is picked yet. Either can be switched off in Settings.")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (again && step == 0) BigButton("Not yet", onClick = { vm.updateSettings { s -> s.copy(onboarded = true) } }, modifier = Modifier.width(120.dp), secondary = true)
            else BigButton("Back", onClick = { if (step > 0) step-- }, modifier = Modifier.width(120.dp).alpha(if (step > 0) 1f else 0f), secondary = true, enabled = step > 0)
            BigButton(when (step) { 0 -> "Let's go"; 4 -> "Start the route"; else -> "Next" }, onClick = ::next, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HoldKey(shape: HoldShape, title: String, sub: String) {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Hold(shape, c.good, 40.dp)
        Text(title, color = c.text, style = cutStyle(13, 700, 1.1f))
        Text(sub, color = c.text2, style = cutStyle(11, 500, 1.25f), modifier = Modifier.widthIn(max = 96.dp))
    }
}

@Composable
private fun Big(text: String) = Text(text, color = CutTheme.colors.text, style = cutStyle(40, 800, 1f, -0.02f, display = true))

@Composable
private fun Body(text: String) = Text(text, color = CutTheme.colors.text2, style = cutStyle(15, 500, 1.45f), modifier = Modifier.widthIn(max = 340.dp))

@Composable
private fun Hint(text: String, alert: Boolean = false) = Text(text, color = if (alert) CutTheme.colors.accent else CutTheme.colors.text2, style = cutStyle(12, 500, 1.3f, tabular = true))

@Composable
private fun Fld(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = CutTheme.colors.text2, style = cutStyle(11, 700, 1f, 0.12f))
        content()
    }
}

@Composable
private fun PickerBox(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().height(64.dp).clip(shape).background(c.surface).border(2.dp, c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button).padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, color = c.text, style = cutStyle(22, 800, 1f, tabular = true)) }
}
