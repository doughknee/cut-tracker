package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.AutoBackup
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.notify.Reminders
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.io.File
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// Each Settings page opens on a picture of what it changes, the way Theme opens on its playable corner of Today.

@Composable
private fun HeroEyebrow(text: String) = Text(text.uppercase(), color = CutTheme.colors.text2, style = cutStyle(10, 800, 1f, 0.1f))

/** Nudges: the reminders that are on, as they land in the shade, in time order, with their buttons. */
@Composable
internal fun Shade(s: Settings, notifOn: Boolean) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val v = CutTheme.vocab
    val on = listOfNotNull(
        if (s.weighReminder) Reminders.KIND_WEIGH to s.weighInTime else null,
        if (s.pickReminder) Reminders.KIND_PICK to s.pickTime else null,
    ).sortedBy { it.second }
    Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline, padding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroEyebrow("What lands in the shade")
            if (on.isEmpty()) Sub("Both reminders are off. Nothing will buzz.", size = 12)
            for ((kind, at) in on) {
                Column(
                    Modifier.fillMaxWidth().alpha(if (notifOn) 1f else 0.45f).clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Hold(HoldShape.JUG, c.accent, 14.dp)
                        Text("SENDLINE · ${clockText(context, at)}", color = c.text2, style = cutStyle(10, 700, 1f, 0.06f, tabular = true))
                    }
                    Text(Reminders.title(kind), color = c.text, style = cutStyle(14, 800, 1.1f))
                    Text(Reminders.text(kind), color = c.text2, style = cutStyle(12, 500, 1.3f))
                    if (kind == Reminders.KIND_PICK) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        for (a in listOf("Yep, Rest day", v.trained)) Text(a.uppercase(), color = c.accent, style = cutStyle(11, 800, 1f, 0.06f))
                    }
                }
            }
            if (!notifOn && on.isNotEmpty()) Sub("Notifications are off for Sendline, so none of this shows yet. Allow them below.", size = 12)
        }
    }
}

/** Seven day chips under a reminder: lit rings that day, tap one to keep it quiet. */
@Composable
internal fun QuietDays(quiet: Set<DayOfWeek>, what: String, onToggle: (DayOfWeek) -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.padding(14.dp, 0.dp, 14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HeroEyebrow(if (quiet.isEmpty()) "Rings every day. Tap a day to keep it quiet" else "Rings on the lit days")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in DayOfWeek.entries) {
                val rings = d !in quiet
                val shape = RoundedCornerShape(8.dp)
                Box(
                    Modifier.weight(1f).height(36.dp).clip(shape)
                        .background(if (rings) c.accent.copy(alpha = 0.14f) else c.ground).border(1.dp, if (rings) c.accent else c.hairline, shape)
                        .toggleable(value = rings, role = Role.Switch) { onToggle(d) }
                        .semantics { contentDescription = "${d.getDisplayName(TextStyle.FULL, Locale.US)} $what reminder" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        d.getDisplayName(TextStyle.SHORT, Locale.US), color = if (rings) c.text else c.dim, style = cutStyle(11, 800, 1f),
                        textDecoration = if (rings) null else TextDecoration.LineThrough,
                    )
                }
            }
        }
    }
}

/** Data: what is on this phone right now, and how safe it is. */
@Composable
internal fun OnThisPhone(st: AppState) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val s = st.settings
    val photoBytes = remember(st.photos) { st.photos.sumOf { File(it.path).length() } }
    val stale = s.autoBackupFolder == null && (s.lastExport == null || java.time.temporal.ChronoUnit.DAYS.between(s.lastExport, java.time.LocalDate.now()) > 14)
    Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline, padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroEyebrow("On this phone")
            Row(Modifier.fillMaxWidth()) {
                for ((n, label) in listOf(
                    st.logs.count { it.mode != null } to "days logged", st.weighIns.size to "weigh-ins",
                    st.liftSets.size to "lift sets", st.photos.size to "photos",
                )) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$n", color = c.text, style = cutStyle(24, 800, 1f, display = true, tabular = true))
                    Text(label.uppercase(), color = c.text2, style = cutStyle(9, 800, 1.1f, 0.06f))
                }
            }
            val photos = if (st.photos.isEmpty()) "No photos yet." else "Photos take ${android.text.format.Formatter.formatShortFileSize(context, photoBytes)}; only the zip export carries them."
            val backup = s.autoBackupFolder?.let { "A copy lands in ${AutoBackup.folderName(it)} every week." } ?: (exportAge(s.lastExport).replaceFirstChar { it.uppercase() } + if (stale) ". Export one below." else ".")
            Sub(photos, size = 12)
            Text(backup, color = if (stale) c.accent else c.good, style = cutStyle(12, 700, 1.3f))
        }
    }
}

/** About: the jug, the name and the version, and the one thing the app is. */
@Composable
internal fun AboutHero(version: String) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.wall, border = c.hairline, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Hold(HoldShape.JUG, c.accent, 48.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sendline", color = c.text, style = cutStyle(26, 800, 1f, -0.02f, display = true))
                Text(
                    (if (version.isBlank()) "" else "Version $version. ") + "A logbook for one cut: the day, the plan and the route, on this phone and nowhere else.",
                    color = c.text2, style = cutStyle(12, 500, 1.3f),
                )
            }
        }
    }
}
