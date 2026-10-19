package com.relentnet.cuttracker.ui.settings

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Backup
import com.relentnet.cuttracker.data.backupJson
import com.relentnet.cuttracker.data.buildBackup
import com.relentnet.cuttracker.data.restoreFrom
import com.relentnet.cuttracker.data.writeZip
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.SettingsRow
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.components.Switch
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/** Settings is a list of categories; each opens its own page, the way the phone's own Settings does. */
object SettingsSections {
    const val PROGRAM = "program"
    const val TARGETS = "targets"
    const val NUDGES = "nudges"
    const val APPEARANCE = "appearance"
    const val DATA = "data"
    const val ABOUT = "about"
}

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onSection: (String) -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val context = LocalContext.current
    val notifOn = remember { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Today", title = "Settings", onBack = onBack)
        Card(Modifier.fillMaxWidth()) {
            CategoryRow(CutIcons.Route, "Program", "${s.lengthWeeks} weeks from ${s.startDate.format(shortDate)} · ${fmt(s.startWeight)} to ${fmt(s.targetWeight)} lb") { onSection(SettingsSections.PROGRAM) }
            CategoryRow(CutIcons.Zap, "Targets", "${s.calories} cal · ${s.protein} g protein · ${s.workoutsPerWeek} workouts a week") { onSection(SettingsSections.TARGETS) }
            CategoryRow(CutIcons.Today, "Nudges", (if (notifOn) "Weigh-in ${s.weighInTime.format(timeFmt)} · pick a day ${s.pickTime.format(timeFmt)}" else "Notifications are off")) { onSection(SettingsSections.NUDGES) }
            CategoryRow(CutIcons.Layers, "Appearance", when (s.theme) { "dark" -> "Dark"; "light" -> "Light"; else -> "Follows the system" } + (if (s.haptics) " · haptics on" else " · haptics off")) { onSection(SettingsSections.APPEARANCE) }
            CategoryRow(CutIcons.Camera, "Data", "Export, import, reset") { onSection(SettingsSections.DATA) }
            CategoryRow(CutIcons.Info, "About", "Sendline, privacy, what stays on the phone", last = true) { onSection(SettingsSections.ABOUT) }
        }
        Sub("Meals, lifts and checkpoints live on the Beta tab.", size = 12)
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, title: String, sub: String, last: Boolean = false, onClick: () -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.pressable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(14.dp, 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.ground), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = c.accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = c.text, style = cutStyle(16, 700, 1.1f))
                Text(sub, color = c.text2, style = cutStyle(12, 500, 1.25f, tabular = true))
            }
            Icon(CutIcons.Forward, null, Modifier.size(12.dp), tint = c.text2)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

/** One category page. Program, Targets and Nudges edit a draft and Save; the others apply at once. */
@Composable
fun SettingsSectionScreen(vm: AppViewModel, section: String, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val saved = st.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var d by remember(saved) { mutableStateOf(saved) }
    var len by remember(saved) { mutableStateOf(saved.lengthWeeks.toString()) }
    var sw by remember(saved) { mutableStateOf(fmt(saved.startWeight)) }
    var tw by remember(saved) { mutableStateOf(fmt(saved.targetWeight)) }
    var cal by remember(saved) { mutableStateOf(saved.calories.toString()) }
    var pro by remember(saved) { mutableStateOf(saved.protein.toString()) }
    var wpw by remember(saved) { mutableStateOf(saved.workoutsPerWeek.toString()) }
    var resetStage by remember { mutableIntStateOf(0) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { vm.repo.restoreFrom(it) } }.isSuccess
            }
            vm.showToast(if (ok) "Imported" else "Could not read that file")
        }
    }

    fun export(withPhotos: Boolean) = scope.launch {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val name = "sendline-" + LocalDate.now()
            if (withPhotos) File(dir, "$name.zip").also { vm.repo.writeZip(it) }
            else File(dir, "$name.json").apply { writeText(backupJson.encodeToString(Backup.serializer(), vm.repo.buildBackup())) }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (withPhotos) "application/zip" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Export Sendline"))
    }

    fun save() {
        val weeks = len.toIntOrNull()?.coerceIn(1, 52) ?: saved.lengthWeeks
        val s = d.copy(
            lengthWeeks = weeks,
            startWeight = sw.toDoubleOrNull() ?: saved.startWeight,
            targetWeight = tw.toDoubleOrNull() ?: saved.targetWeight,
            caloriesP1 = cal.toIntOrNull() ?: saved.caloriesP1,
            protein = pro.toIntOrNull() ?: saved.protein,
            workoutsPerWeek = wpw.toIntOrNull()?.coerceIn(0, 7) ?: saved.workoutsPerWeek,
        )
        vm.saveSettings(s) { vm.showToast("Saved"); onBack() }
    }

    val title = when (section) {
        SettingsSections.PROGRAM -> "Program"; SettingsSections.TARGETS -> "Targets"; SettingsSections.NUDGES -> "Nudges"
        SettingsSections.APPEARANCE -> "Appearance"; SettingsSections.DATA -> "Data"; else -> "About"
    }
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Settings", title = title, onBack = onBack)
        when (section) {
            SettingsSections.PROGRAM -> {
                Sub("The route itself. Changing the start or the length re-draws the calendar; nothing you logged is touched.")
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow("Start date") {
                        Picker(d.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                            DatePickerDialog(context, { _, y, m, day -> d = d.copy(startDate = LocalDate.of(y, m + 1, day)) }, d.startDate.year, d.startDate.monthValue - 1, d.startDate.dayOfMonth).show()
                        }
                    }
                    SettingsRow("Length") {
                        NumField(len, { len = it }, 56.dp)
                        val weeks = len.toIntOrNull() ?: saved.lengthWeeks
                        Sub("weeks · ends ${d.startDate.plusDays(weeks * 7L).format(shortDate)}")
                    }
                    SettingsRow("Start weight") { NumField(sw, { sw = it }); Sub("lb") }
                    SettingsRow("Target weight", last = true) { NumField(tw, { tw = it }); Sub("lb") }
                }
                BigButton("Save", onClick = ::save, modifier = Modifier.fillMaxWidth(), confirm = true)
            }
            SettingsSections.TARGETS -> {
                Sub("Protein is the one that keeps the chain: weigh in and hit it and the day counts.")
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow("Calories") { NumField(cal, { cal = it }, 84.dp) }
                    SettingsRow("Protein") { NumField(pro, { pro = it }, 64.dp); Sub("g") }
                    SettingsRow("Workouts per week", last = true) { NumField(wpw, { wpw = it }, 56.dp); Sub("blocks on Climb and Off days") }
                }
                BigButton("Save", onClick = ::save, modifier = Modifier.fillMaxWidth(), confirm = true)
            }
            SettingsSections.NUDGES -> {
                Sub("Two notifications a day at most. Nothing about streaks.")
                Card(Modifier.fillMaxWidth()) {
                    // Re-read on every resume so coming back from system settings updates the row.
                    var tick by remember { mutableIntStateOf(0) }
                    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
                    val notifOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
                    val exactOn = remember(tick) { Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
                    SettingsRow(
                        "Reminders",
                        onClick = when {
                            !notifOn -> ({ context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) })
                            !exactOn && Build.VERSION.SDK_INT >= 31 -> ({ context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) })
                            else -> null
                        },
                    ) {
                        Sub(
                            when {
                                !notifOn -> "Off. Allow notifications ›"
                                !exactOn -> "On, timing may drift. Allow exact alarms ›"
                                else -> "On"
                            },
                            color = if (notifOn) c.text2 else c.accent,
                        )
                    }
                    SettingsRow("Weigh-in reminder") { TimePicker(d.weighInTime) { d = d.copy(weighInTime = it) } }
                    SettingsRow("\"Pick a day\" if unset") { TimePicker(d.pickTime) { d = d.copy(pickTime = it) } }
                    SettingsRow("Day rolls over at", last = true) { TimePicker(LocalTime.of(d.resetHour, 0)) { d = d.copy(resetHour = it.hour) } }
                }
                BigButton("Save", onClick = ::save, modifier = Modifier.fillMaxWidth(), confirm = true)
            }
            SettingsSections.APPEARANCE -> {
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow("Theme") {
                        for ((k, l) in listOf("system" to "System", "dark" to "Dark", "light" to "Light")) Pill(l, saved.theme == k, { vm.saveSettings(saved.copy(theme = k)) })
                    }
                    SettingsRow("Haptics and confetti", last = true) { Switch(saved.haptics) { vm.saveSettings(saved.copy(haptics = !saved.haptics)) } }
                }
                Sub("Applies at once.", size = 12)
            }
            SettingsSections.DATA -> {
                Sub("Everything lives on this phone. Export is how it moves to another one.")
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow("Export JSON", onClick = { export(false) }) { Sub("›") }
                    SettingsRow("Export with photos", onClick = { export(true) }) { Sub("zip ›") }
                    SettingsRow("Import JSON or zip", last = true, onClick = { importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain", "*/*")) }) { Sub("›") }
                }
                Eyebrow("Danger")
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow(
                        when (resetStage) { 0 -> "Reset program"; 1 -> "Tap again to wipe everything"; else -> "Last chance. Tap to reset" },
                        last = true, labelColor = c.accent,
                        onClick = {
                            if (resetStage < 2) resetStage++ else { resetStage = 0; vm.resetProgram { vm.showToast("Program reset") } }
                        },
                    ) { Sub(if (resetStage == 0) "double confirm" else "${2 - resetStage + 1} more") }
                }
            }
            else -> {
                val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
                Card(Modifier.fillMaxWidth()) {
                    SettingsRow("Sendline") { Sub(version) }
                    SettingsRow("Privacy policy", onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://doughknee.github.io/cut-tracker/privacy.html"))) }) { Sub("›") }
                    SettingsRow("Everything stays on this phone", last = true) { Sub("no account, no network") }
                }
                Sub("Sendline is a logbook and a scoreboard, not medical advice. Your targets are yours; talk to a doctor before a cut if you have a condition.", size = 12)
            }
        }
    }
}

@Composable
fun NumField(value: String, onChange: (String) -> Unit, width: Dp = 76.dp) {
    Field(value, onChange, Modifier.width(width), height = 36.dp, fontSize = 14, weight = 700, center = true, borderWidth = 1.dp, radius = 8.dp, padding = 6.dp, keyboard = KeyboardType.Decimal)
}

@Composable
private fun Picker(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.height(36.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, style = cutStyle(14, 700, 1f, tabular = true)) }
}

@Composable
private fun TimePicker(value: LocalTime, onPick: (LocalTime) -> Unit) {
    val context = LocalContext.current
    Picker(value.format(timeFmt)) {
        TimePickerDialog(context, { _, h, m -> onPick(LocalTime.of(h, m)) }, value.hour, value.minute, true).show()
    }
}

internal fun fmt(v: Double) = if (v % 1.0 == 0.0) "%.1f".format(Locale.US, v) else v.toString()
