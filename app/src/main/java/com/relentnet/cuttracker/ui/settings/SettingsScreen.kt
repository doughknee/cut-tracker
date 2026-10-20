package com.relentnet.cuttracker.ui.settings

import com.relentnet.cuttracker.ui.components.ConfirmDialog
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.composed
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.Switch
import com.relentnet.cuttracker.ui.theme.Palettes
import com.relentnet.cuttracker.ui.components.pressable
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

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/** Settings is the app's chrome: nudges, looks, data, about. The plan itself lives on Beta. */
object SettingsSections {
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
        // The privacy line comes first, because it is the one thing every settings page promises.
        Card(Modifier.fillMaxWidth().pressable { onSection(SettingsSections.DATA + "/online") }, radius = 14.dp, background = c.ground, border = c.hairline, padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(CutIcons.Shield, null, Modifier.size(22.dp), tint = if (s.onlineFeatures) c.text2 else c.good)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (s.onlineFeatures) "Online features: ${s.onlineCount} on" else "Offline", color = c.text, style = cutStyle(16, 800, 1f, display = true))
                    Text(if (s.onlineFeatures) "Only what you switched on can use the network." else "Nothing leaves the phone.", color = c.text2, style = cutStyle(12, 500, 1.3f))
                }
                Icon(CutIcons.Forward, null, Modifier.size(12.dp), tint = c.text2)
            }
        }
        Card(Modifier.fillMaxWidth(), radius = 14.dp) {
            CategoryRow(CutIcons.Today, "Nudges", when {
                !notifOn -> "Notifications are off"
                !s.weighReminder && !s.pickReminder -> "Both reminders off"
                else -> listOfNotNull(
                    if (s.weighReminder) "weigh-in ${clockText(context, s.weighInTime)}" else null,
                    if (s.pickReminder) "pick a day ${clockText(context, s.pickTime)}" else null,
                ).joinToString(", ").replaceFirstChar { it.uppercase() }
            }) { onSection(SettingsSections.NUDGES) }
            CategoryRow(CutIcons.Layers, "Appearance", appearanceLine(s)) { onSection(SettingsSections.APPEARANCE) }
            CategoryRow(CutIcons.Data, "Data", "Export, import, online features, wipe. " + exportAge(s.lastExport).replaceFirstChar { it.uppercase() } + ".") { onSection(SettingsSections.DATA) }
            CategoryRow(CutIcons.Info, "About", "Sendline, privacy, what stays on the phone", last = true) { onSection(SettingsSections.ABOUT) }
        }
        Sub("The route itself, targets, meals, lifts and checkpoints live on the ${CutTheme.vocab.beta} tab.", size = 12)
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
                Text(title, color = c.text, style = cutStyle(17, 800, 1.1f, -0.01f, display = true))
                Text(sub, color = c.text2, style = cutStyle(12, 500, 1.25f, tabular = true))
            }
            Icon(CutIcons.Forward, null, Modifier.size(12.dp), tint = c.text2)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

/** One category page. Every control applies at once, the way the Beta pages do. [section] may carry "/<focus>". */
@Composable
fun SettingsSectionScreen(vm: AppViewModel, section: String, onBack: () -> Unit) {
    if (section.substringBefore('/') == SettingsSections.APPEARANCE) { AppearancePage(vm, onBack); return }
    val focus = section.substringAfter('/', "")
    val section = section.substringBefore('/')
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var confirm by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { vm.repo.restoreFrom(it) } }.isSuccess
            }
            vm.showToast(if (ok) "Imported. Everything from the backup is in place." else "Could not read that file. Nothing changed.")
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = backupJson.encodeToString(Backup.serializer(), vm.repo.buildBackup())
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
                }.isSuccess
            }
            vm.showToast(if (ok) "Backup saved" else "Could not save the file. Nothing changed.")
            if (ok) vm.updateSettings { s -> s.copy(lastExport = LocalDate.now()) }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        vm.updateSettings(then = {
            scope.launch {
                val ok = com.relentnet.cuttracker.data.AutoBackup.runIfDue(context.applicationContext, vm.repo, force = true)
                if (!ok) {
                    runCatching { context.contentResolver.releasePersistableUriPermission(uri, flags) }
                    vm.updateSettings { s -> s.copy(autoBackupFolder = null) }
                }
                vm.showToast(if (ok) "Backed up. The next copy lands in a week." else "Could not write to that folder. Pick another one.")
            }
        }) { s -> s.copy(autoBackupFolder = uri.toString()) }
    }

    fun export(withPhotos: Boolean) = scope.launch {
        val file = runCatching { withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val name = "sendline-" + LocalDate.now()
            if (withPhotos) File(dir, "$name.zip").also { vm.repo.writeZip(it) }
            else File(dir, "$name.json").apply { writeText(backupJson.encodeToString(Backup.serializer(), vm.repo.buildBackup())) }
        } }.getOrElse { vm.showToast("Export failed. Is the phone out of space?"); return@launch }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (withPhotos) "application/zip" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Export Sendline"))
        vm.updateSettings { s -> if (s.autoBackupFolder != null) s else s.copy(lastExport = LocalDate.now()) }
    }

    when (confirm) {
        "import" -> ConfirmDialog(
            "Replace everything?",
            "Importing puts the backup's logs, photos and settings in place of what is on this phone now. Export first if you want to keep it.",
            "Pick a file", onConfirm = { importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain", "*/*")) },
            onDismiss = { confirm = null }, extra = "Export first" to { export(false) },
        )
        "reset" -> ConfirmDialog(
            "Wipe everything?",
            "Logs, photos, badges, xp and settings are deleted from this phone, and onboarding starts over. This cannot be undone.",
            "Wipe it", onConfirm = { vm.resetProgram { vm.showToast("Everything wiped") } },
            onDismiss = { confirm = null }, extra = "Export first" to { export(false) },
        )
    }
    val title = when (section) { SettingsSections.NUDGES -> "Nudges"; SettingsSections.APPEARANCE -> "Appearance"; SettingsSections.DATA -> "Data"; else -> "About" }
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Settings", title = title, onBack = onBack)
        when (section) {
            SettingsSections.NUDGES -> {
                Sub("Two notifications a day at most. Nothing about streaks.", size = 12)
                // Re-read on every resume so coming back from system settings updates the row.
                var tick by remember { mutableIntStateOf(0) }
                LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
                val notifOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
                val exactOn = remember(tick) { Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
                KindCard("Reminders", if (notifOn) "on" else "off") {
                    PlainRow("Allowed by the phone", when { !notifOn -> "Off. Tap to allow notifications."; !exactOn -> "On, timing may drift. Tap to allow exact alarms."; else -> "On, exact" }) {
                        val fix = when {
                            !notifOn -> ({ context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) })
                            !exactOn && Build.VERSION.SDK_INT >= 31 -> ({ context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) })
                            else -> null
                        }
                        if (fix != null) LinkText("Allow", onClick = fix) else Icon(CutIcons.Check, null, Modifier.size(16.dp), tint = c.good)
                    }
                    PlainRow("Weigh-in", if (s.weighReminder) "before food, quiet once you have weighed in" else "off") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (s.weighReminder) TimePick(s.weighInTime) { vm.updateSettings { s -> s.copy(weighInTime = it) } }
                            Switch(s.weighReminder, "Weigh-in reminder") { haptics.tick(); vm.updateSettings { s -> s.copy(weighReminder = !s.weighReminder) } }
                        }
                    }
                    PlainRow("Pick a day", if (s.pickReminder) "only if no day is picked" else "off") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (s.pickReminder) TimePick(s.pickTime) { vm.updateSettings { s -> s.copy(pickTime = it) } }
                            Switch(s.pickReminder, "Pick-a-day reminder") { haptics.tick(); vm.updateSettings { s -> s.copy(pickReminder = !s.pickReminder) } }
                        }
                    }
                }
                val widgets = context.getSystemService(android.appwidget.AppWidgetManager::class.java)
                if (widgets?.isRequestPinAppWidgetSupported == true) KindCard("On the home screen", "") {
                    PlainRow("The Sendline widget", "weigh in, pick the day and tick meals without opening the app") {
                        LinkText("Add") { widgets.requestPinAppWidget(android.content.ComponentName(context, com.relentnet.cuttracker.widget.CutWidgetReceiver::class.java), null, null) }
                    }
                }
                KindCard("The day", "") {
                    PlainRow("Rolls over at", "a late night still counts as today until then") { TimePick(LocalTime.of(s.resetHour, 0)) { vm.updateSettings { s -> s.copy(resetHour = it.hour) } } }
                }
            }
            SettingsSections.DATA -> {
                Sub("Everything lives on this phone. Export is how it moves to another one.", size = 12)
                KindCard("Backup", exportAge(s.lastExport), sub = "The only copy of your logs is on this phone until you export one. Save it somewhere that is not this phone.") {
                    PlainRow(
                        "Back up every week",
                        s.autoBackupFolder?.let { "to ${com.relentnet.cuttracker.data.AutoBackup.folderName(it)}, when you open the app" } ?: "pick a folder and a copy lands there once a week",
                    ) {
                        if (s.autoBackupFolder == null) LinkText("Pick") { folderLauncher.launch(null) }
                        else Switch(true, "Weekly backup") {
                            haptics.tick()
                            s.autoBackupFolder?.let { f -> runCatching { context.contentResolver.releasePersistableUriPermission(android.net.Uri.parse(f), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } }
                            vm.updateSettings { s -> s.copy(autoBackupFolder = null) }
                        }
                    }
                    PlainRow("Save a copy", "a JSON file of everything but photos, wherever you pick") { LinkText("Save") { saveLauncher.launch("sendline-${LocalDate.now()}.json") } }
                    PlainRow("Export", "the same file, through the share menu") { LinkText("Share") { export(false) } }
                    PlainRow("Export with photos", "a zip, bigger") { LinkText("Share") { export(true) } }
                    PlainRow("Import", "replaces what is on this phone") { LinkText("Pick a file") { confirm = "import" } }
                }
                KindCard("Online features", if (s.onlineFeatures) "on" else "off", sub = "Off, the app never touches the network. Each feature says what it sends and to whom, and nothing about you is ever part of it.", modifier = Modifier.pulseWhen(focus == "online")) {
                    PlainRow("Allow online features", "the master switch") { Switch(s.onlineFeatures, "Allow online features") { haptics.tick(); vm.updateSettings { s -> s.copy(onlineFeatures = !s.onlineFeatures) } } }
                    PlainRow("Recipe import by link", "downloads the one page you paste in New meal, straight from that site") {
                        Switch(s.onlineFeatures && s.recipeImport, "Recipe import") { haptics.tick(); vm.updateSettings { s -> if (s.onlineFeatures && s.recipeImport) s.copy(recipeImport = false) else s.copy(onlineFeatures = true, recipeImport = true) } }
                    }
                    PlainRow("Barcode lookup", "sends the digits you scan in New meal to Open Food Facts, a public food database, and nothing else") {
                        Switch(s.onlineFeatures && s.barcodeLookup, "Barcode lookup") { haptics.tick(); vm.updateSettings { s -> if (s.onlineFeatures && s.barcodeLookup) s.copy(barcodeLookup = false) else s.copy(onlineFeatures = true, barcodeLookup = true) } }
                    }
                    PlainRow("The code that does it", "public, on GitHub") {
                        LinkText("Recipe") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/doughknee/cut-tracker/blob/main/app/src/main/java/com/relentnet/cuttracker/data/RecipeImport.kt"))) }
                        LinkText("Barcode") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/doughknee/cut-tracker/blob/main/app/src/main/java/com/relentnet/cuttracker/data/OpenFoodFacts.kt"))) }
                    }
                }
                KindCard("Danger", "") {
                    PlainRow("Wipe everything", "logs, photos, settings, all of it") {
                        LinkText("Wipe", color = c.accent) { haptics.tap(); confirm = "reset" }
                    }
                }
            }
            else -> {
                val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
                KindCard("Sendline", version) {
                    PlainRow("Privacy policy", "what the app keeps and what it never does") { LinkText("Read") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://doughknee.github.io/cut-tracker/privacy.html"))) } }
                    PlainRow("Everything stays on this phone", if (s.onlineFeatures) "no account, online only when you ask" else "no account, no network") { Icon(CutIcons.Shield, null, Modifier.size(16.dp), tint = c.good) }
                    PlainRow("The code", "public, on GitHub") { LinkText("Read") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/doughknee/cut-tracker"))) } }
                }
                Sub("Sendline is a logbook and a scoreboard, not medical advice. Your targets are yours; talk to a doctor before a cut if you have a condition.", size = 12)
            }
        }
    }
}

/**
 * Scrolls into view and flashes an accent border once, for the row the user was sent to. Lives on the modifier so the
 * card's own layout is untouched.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pulseWhen(on: Boolean): Modifier = composed {
    if (!on) return@composed this
    val c = CutTheme.colors
    val requester = remember { BringIntoViewRequester() }
    var lit by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (lit) 1f else 0f, tween(if (lit) 250 else 900), label = "pulse")
    LaunchedEffect(Unit) { delay(250); requester.bringIntoView(); lit = true; delay(900); lit = false }
    bringIntoViewRequester(requester).border(2.dp, c.accent.copy(alpha = alpha), RoundedCornerShape(14.dp))
}

/** A small accent action at the right of a row. */
@Composable
private fun LinkText(label: String, color: Color = CutTheme.colors.accent, onClick: () -> Unit) {
    Text(
        label.uppercase(), color = color, style = cutStyle(11, 800, 1f, 0.08f),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick, role = Role.Button).padding(horizontal = 8.dp, vertical = 16.dp),
    )
}

/** "last export today", "last export 12 days ago", "never exported". */
private fun exportAge(d: LocalDate?): String {
    if (d == null) return "never exported"
    val n = java.time.temporal.ChronoUnit.DAYS.between(d, LocalDate.now())
    return when { n <= 0 -> "exported today"; n == 1L -> "exported yesterday"; else -> "exported $n days ago" }
}

/** A time the way the phone shows times: 19:00 or 7:00 PM. */
private fun clockText(context: android.content.Context, t: LocalTime): String =
    t.format(if (android.text.format.DateFormat.is24HourFormat(context)) timeFmt else DateTimeFormatter.ofPattern("h:mm a", Locale.US))

@Composable
private fun TimePick(value: LocalTime, onPick: (LocalTime) -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.height(36.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape)
            .clickable(role = Role.Button) { TimePickerDialog(context, { _, h, m -> onPick(LocalTime.of(h, m)) }, value.hour, value.minute, android.text.format.DateFormat.is24HourFormat(context)).show() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(value.format(if (android.text.format.DateFormat.is24HourFormat(context)) timeFmt else DateTimeFormatter.ofPattern("h:mm a", Locale.US)), color = c.text, style = cutStyle(14, 800, 1f, tabular = true)) }
}

/** What the Appearance row says: the mode, and the look the app wears right now. */
private fun appearanceLine(s: com.relentnet.cuttracker.data.Settings): String {
    val mode = when (s.theme) { "dark" -> "Dark"; "light" -> "Light"; "schedule" -> "Dark at night"; else -> "Follows the system" }
    val look = Palettes.of(s, LocalDate.now()).name + if (s.paletteByCalendar) " by the calendar" else ""
    return "$mode, $look" + (if (s.haptics) "" else ", haptics off")
}
