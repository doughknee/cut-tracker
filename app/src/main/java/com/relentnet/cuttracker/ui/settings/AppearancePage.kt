package com.relentnet.cuttracker.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import com.relentnet.cuttracker.ui.components.drawWallDecor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import com.relentnet.cuttracker.data.CustomLook
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.palette
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.CheckBox
import com.relentnet.cuttracker.ui.components.Confetti
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.delay
import com.relentnet.cuttracker.ui.components.ChipRow
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.Switch
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.theme.CutColors
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.LocalCutColors
import com.relentnet.cuttracker.ui.theme.LocalDecor
import com.relentnet.cuttracker.ui.theme.Decor
import com.relentnet.cuttracker.ui.theme.Palette
import com.relentnet.cuttracker.ui.theme.Palettes
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.Month

/** The look: dark or light and when, which palette, or let the calendar choose. Everything applies as you tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val today = LocalDate.now()
    val calReason = Palettes.forDate(today).second
    val current = Palettes.of(s, today)
    val dark = c.isDark
    Screen(bottomInset = true) {
        BackHeader("Settings", "Appearance", onBack)
        Sub("The stone, the chalk and the sends change. The type and the layout never do.", size = 12)
        Preview(current, dark, if (s.paletteByCalendar) "$calReason, by the calendar" else current.line)

        KindCard("Dark or light", "") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("system" to "System", "dark" to "Dark", "light" to "Light", "schedule" to "Dark at night"), s.theme, onSelect = { haptics.tick(); vm.saveSettings(s.copy(theme = it)) })
            }
            if (s.theme == "schedule") Sub("Dark from 19:00 to 07:00, light the rest of the day.", Modifier.padding(14.dp, 0.dp, 14.dp, 12.dp), size = 12)
        }

        KindCard("Calendar", if (s.paletteByCalendar) "on" else "off", sub = "The look follows the month, and a holiday takes the wall for its days. A long route gets to change its clothes.") {
            PlainRow("Follow the calendar", if (s.paletteByCalendar) "today: $calReason" else "off, the look below stays") { Switch(s.paletteByCalendar) { haptics.tick(); vm.saveSettings(s.copy(paletteByCalendar = !s.paletteByCalendar)) } }
            Column(Modifier.padding(14.dp, 10.dp, 14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (m in Month.entries) {
                        val p = Palettes.forMonth(m)
                        val f = if (dark) p.dark else p.light
                        val isNow = m == today.month
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp)).background(f.ground).border(if (isNow) 2.dp else 1.dp, if (isNow) c.accent else c.hairline, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(f.accent))
                            }
                            Text(m.name.take(1), color = if (isNow) c.text else c.text2, style = cutStyle(9, 800, 1f))
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (h in Palettes.holidays) {
                        val f = if (dark) h.palette.dark else h.palette.light
                        val on = h.covers(today)
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) f.accent.copy(alpha = 0.18f) else c.ground).border(1.dp, if (on) f.accent else c.hairline, RoundedCornerShape(8.dp)).padding(8.dp, 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(f.accent))
                            Text("${h.name}, ${h.span}", color = c.text, style = cutStyle(11, 700, 1f))
                        }
                    }
                }
            }
        }

        var sheet by remember { mutableStateOf<String?>(null) }
        var code by remember { mutableStateOf("") }
        val looks = s.customLooks
        fun add(look: CustomLook) { haptics.tick(); vm.saveSettings(s.copy(customLooks = looks.filter { it.id != look.id } + look, palette = look.id, paletteByCalendar = false)) }
        KindCard("Looks", "${Palettes.all.size + looks.size}", sub = if (s.paletteByCalendar) "Pick one and the calendar lets go." else null) {
            Column(Modifier.padding(14.dp, 6.dp, 14.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val tiles: List<Any?> = Palettes.all + looks + listOf(null)
                for (row in tiles.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in row) when (t) {
                        is Palette -> PaletteTile(t, dark, selected = !s.paletteByCalendar && t.id == s.palette, Modifier.weight(1f)) {
                            haptics.tick(); vm.saveSettings(s.copy(palette = t.id, paletteByCalendar = false))
                        }
                        is CustomLook -> PaletteTile(
                            t.palette(), dark, selected = !s.paletteByCalendar && t.id == s.palette, Modifier.weight(1f),
                            onEdit = { sheet = t.id },
                            onDelete = { haptics.tick(); vm.saveSettings(s.copy(customLooks = looks.filter { it.id != t.id }, palette = if (s.palette == t.id) "sandstone" else s.palette)) },
                        ) { haptics.tick(); vm.saveSettings(s.copy(palette = t.id, paletteByCalendar = false)) }
                        else -> NewLookTile(Modifier.weight(1f)) { sheet = "" }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
                Field(
                    code, { v -> code = v.take(8); CustomLook.fromCode(v, CustomLook.PREFIX + System.currentTimeMillis().toString(36))?.let { add(it); code = "" } },
                    Modifier.fillMaxWidth(), height = 44.dp, fontSize = 14, weight = 700, keyboard = KeyboardType.Ascii, placeholder = "Paste a code, SL-7K2Q", borderWidth = 1.dp, radius = 10.dp,
                )
                if (code.length >= 7 && CustomLook.decode(code) == null) Sub("Not a Sendline code. Four letters after SL-, and a typo is refused.", size = 11)
            }
        }
        sheet?.let { id ->
            val editing = looks.firstOrNull { it.id == id }
            ChalkSheet(
                editing, dark, onDismiss = { sheet = null },
                onSave = { add(it.copy(id = editing?.id ?: (CustomLook.PREFIX + System.currentTimeMillis().toString(36)))); sheet = null },
                onDelete = if (editing != null) ({ vm.saveSettings(s.copy(customLooks = looks.filter { it.id != editing.id }, palette = if (s.palette == editing.id) "sandstone" else s.palette)); sheet = null }) else null,
            )
        }

        KindCard("Feel", "") {
            PlainRow("Haptics and confetti", "a tick on every check, a burst on a Send") { Switch(s.haptics) { vm.saveSettings(s.copy(haptics = !s.haptics)) } }
        }
        Sub("Applies at once. The widget keeps its own colors.", size = 12)
    }
}

/**
 * A corner of Today drawn in [palette], so a tap on a look shows what it does before it does it everywhere. It plays:
 * tick the row, log the day, and the look's own confetti goes up. Then it resets so you can do it again.
 */
@Composable
private fun Preview(palette: Palette, dark: Boolean, line: String) {
    val colors = palette.colors(dark)
    val haptics = LocalHaptics.current
    val ground by animateColorAsState(colors.ground, tween(360), label = "pg")
    val surface by animateColorAsState(colors.surface, tween(360), label = "ps")
    val wall by animateColorAsState(colors.wall, tween(360), label = "pw")
    val accent by animateColorAsState(colors.accent, tween(360), label = "pa")
    val good by animateColorAsState(colors.good, tween(360), label = "pgood")
    val freeze by animateColorAsState(colors.freeze, tween(360), label = "pf")
    val live: CutColors = colors.copy(ground = ground, surface = surface, wall = wall, accent = accent, good = good, freeze = freeze)
    var checked by remember { mutableStateOf(false) }
    var logged by remember { mutableStateOf(false) }
    val sent = checked && logged
    LaunchedEffect(sent) { if (sent) { haptics.pattern(0, 30, 40, 60); delay(1800); checked = false; logged = false } }
    CompositionLocalProvider(LocalCutColors provides live, LocalDecor provides palette.decor) {
        val c = CutTheme.colors
        Box(Modifier.fillMaxWidth()) {
            Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline) {
                Box(Modifier.fillMaxWidth().height(86.dp).background(c.wall).drawBehind { drawWallDecor(palette.decor, c, 64.dp.toPx()) }) {
                    Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                        Hold(HoldShape.JUG, c.good, 30.dp); Hold(HoldShape.CRIMP, c.goodDim, 26.dp); Hold(HoldShape.SLOPER, c.freeze, 28.dp)
                        val today by animateColorAsState(if (sent) c.good else c.accent, tween(300), label = "ptoday")
                        Hold(HoldShape.PINCH, today, 30.dp)
                    }
                    Text("V1 · WEEK 1", color = c.onAccent, style = cutStyle(9, 800, 1f, 0.1f), modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(4.dp)).background(c.accent).padding(6.dp, 3.dp))
                }
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(palette.name, color = c.text, style = cutStyle(20, 800, 1f, -0.02f, display = true))
                            Text(line, color = c.text2, style = cutStyle(11, 500, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (sent) "5 CHAIN" else "4 CHAIN", color = c.good, style = cutStyle(11, 800, 1f, 0.06f), modifier = Modifier.padding(start = 10.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (checked) c.surface.copy(alpha = 0.7f) else c.surface)
                            .then(if (checked) Modifier else Modifier.border(1.5.dp, c.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp)))
                            .clickable(role = Role.Checkbox) { haptics.tick(); checked = !checked }.padding(12.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CheckBox(checked, size = 20, iconSize = 11)
                        Column(Modifier.weight(1f)) {
                            if (!checked) Text("UP NEXT", color = c.accent, style = cutStyle(9, 800, 1f, 0.1f))
                            Text("Weigh in", color = if (checked) c.text2 else c.text, style = cutStyle(14, 700, 1f), textDecoration = if (checked) TextDecoration.LineThrough else null)
                        }
                        val xp by animateColorAsState(if (checked) c.good else c.text2, tween(200), label = "pxp")
                        Text("+10", color = xp, style = cutStyle(11, 700, 1f, tabular = true))
                    }
                    BigButton(if (logged) "Logged" else "Log it", onClick = { haptics.tick(); logged = true }, modifier = Modifier.fillMaxWidth(), height = 40.dp, fontSize = 12, confirm = true, enabled = !logged)
                }
            }
            if (sent) Confetti(colors = listOf(c.accent, c.good))
        }
    }
}

/** One look in the grid, drawn in its own stone so the grid is the preview. A chalked look carries its pen and its bin. */
@Composable
private fun PaletteTile(p: Palette, dark: Boolean, selected: Boolean, modifier: Modifier, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null, onClick: () -> Unit) {
    val outer = CutTheme.colors
    CompositionLocalProvider(LocalCutColors provides p.colors(dark), LocalDecor provides p.decor) {
        val c = CutTheme.colors
        Column(
            modifier.clip(RoundedCornerShape(12.dp)).background(c.ground).border(if (selected) 2.dp else 1.dp, if (selected) outer.accent else outer.hairline, RoundedCornerShape(12.dp))
                .pressable(onClick = onClick).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.decor != Decor.NONE) Hold(HoldShape.JUG, c.good, 18.dp) else Swatch(c.accent, 16)
                Swatch(c.good, 12); Swatch(c.freeze, 12); Swatch(c.surface, 12, c.mutedBorder)
                Box(Modifier.weight(1f))
                if (selected) Text("ON", color = outer.accent, style = cutStyle(9, 800, 1f, 0.1f))
                if (onEdit != null) Icon(CutIcons.Pen, "Edit", Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onEdit).padding(5.dp), tint = c.text2)
                if (onDelete != null) Icon(CutIcons.Trash, "Delete", Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onDelete).padding(5.dp), tint = c.text2)
            }
            Text(p.name, color = c.text, style = cutStyle(15, 800, 1f, display = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.line, color = c.text2, style = cutStyle(10, 500, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The empty tile at the end of the grid: a dashed frame and a plus. */
@Composable
private fun NewLookTile(modifier: Modifier, onClick: () -> Unit) {
    val c = CutTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).drawBehind {
            drawRoundRect(c.mutedBorder, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))), cornerRadius = CornerRadius(12.dp.toPx()))
        }.pressable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, c.accent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(CutIcons.Plus, null, Modifier.size(10.dp), tint = c.accent) }
        Text("Chalk your own", color = c.text, style = cutStyle(15, 800, 1f, display = true))
        Text("Your hue on your stone, with a code to share.", color = c.text2, style = cutStyle(10, 500, 1.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Swatch(color: Color, size: Int, ring: Color? = null) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 2).dp)).background(color).then(if (ring != null) Modifier.border(1.dp, ring, RoundedCornerShape((size / 2).dp)) else Modifier))
}
