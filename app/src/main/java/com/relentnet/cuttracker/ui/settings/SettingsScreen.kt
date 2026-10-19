package com.relentnet.cuttracker.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Backup
import com.relentnet.cuttracker.data.Settings
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
import com.relentnet.cuttracker.ui.components.Switch
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

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onMeals: () -> Unit, onExercises: () -> Unit, onCheckpoints: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val saved = st.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Program, targets and nudges apply on Save. Appearance applies live (the prototype switches theme instantly).
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
            caloriesP1 = if (d.phase == 1) cal.toIntOrNull() ?: saved.caloriesP1 else d.caloriesP1,
            caloriesP2 = if (d.phase == 2) cal.toIntOrNull() ?: saved.caloriesP2 else d.caloriesP2,
            protein = pro.toIntOrNull() ?: saved.protein,
            workoutsPerWeek = wpw.toIntOrNull()?.coerceIn(0, 7) ?: saved.workoutsPerWeek,
        )
        vm.saveSettings(s) { vm.showToast("Saved"); onBack() }
    }

    Screen(bottomInset = true) {
        BackHeader(backLabel = "Today", title = "Settings", onBack = onBack)
        Section("Appearance") {
            SettingsRow("Theme") {
                for ((k, l) in listOf("system" to "System", "dark" to "Dark", "light" to "Light")) Pill(l, saved.theme == k, { vm.saveSettings(saved.copy(theme = k)) })
            }
            SettingsRow("Haptics and confetti", last = true) { Switch(saved.haptics) { vm.saveSettings(saved.copy(haptics = !saved.haptics)) } }
        }
        Section("Program") {
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
            SettingsRow("Target weight") { NumField(tw, { tw = it }); Sub("lb") }
            SettingsRow("Phase", last = true) {
                Pill("1 Cut", d.phase == 1, { d = d.copy(phase = 1); cal = d.caloriesP1.toString() })
                Pill("2 Build", d.phase == 2, { d = d.copy(phase = 2); cal = d.caloriesP2.toString() })
            }
        }
        Section("Targets") {
            SettingsRow("Calories") { NumField(cal, { cal = it }, 84.dp) }
            SettingsRow("Protein") { NumField(pro, { pro = it }, 64.dp); Sub("g") }
            SettingsRow("Workouts per week") { NumField(wpw, { wpw = it }, 56.dp); Sub("blocks on Climb and Off days") }
            SettingsRow("Meals", onClick = onMeals) { Sub("${saved.meals.size} items ›") }
            SettingsRow("Exercises", onClick = onExercises) { Sub("${saved.exercises.size} items ›") }
            SettingsRow("Checkpoints and free days", last = true, onClick = onCheckpoints) { Sub("${saved.checkpoints.size} ›") }
        }
        Section("Nudges") {
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
        Section("Data") {
            SettingsRow("Export JSON", onClick = { export(false) }) { Sub("›") }
            SettingsRow("Export with photos", onClick = { export(true) }) { Sub("zip ›") }
            SettingsRow("Import JSON or zip", onClick = { importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain", "*/*")) }) { Sub("›") }
            SettingsRow(
                when (resetStage) { 0 -> "Reset program"; 1 -> "Tap again to wipe everything"; else -> "Last chance. Tap to reset" },
                last = true, labelColor = c.accent,
                onClick = {
                    if (resetStage < 2) resetStage++ else { resetStage = 0; vm.resetProgram { vm.showToast("Program reset") } }
                },
            ) { Sub(if (resetStage == 0) "double confirm" else "${2 - resetStage + 1} more") }
        }
        Section("About") {
            val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
            SettingsRow("Sendline") { Sub(version) }
            SettingsRow("Privacy policy", onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://doughknee.github.io/cut-tracker/privacy.html"))) }) { Sub("›") }
            SettingsRow("Everything stays on this phone", last = true) { Sub("no account, no network") }
        }
        BigButton("Save", onClick = ::save, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun Section(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Eyebrow(title, Modifier.padding(bottom = 8.dp))
        Card(Modifier.fillMaxWidth(), content = content)
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
