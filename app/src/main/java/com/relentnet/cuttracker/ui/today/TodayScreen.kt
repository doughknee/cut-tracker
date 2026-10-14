package com.relentnet.cuttracker.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.engine.ScriptItem
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.CheckRow
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Header
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.TileLabel
import com.relentnet.cuttracker.ui.components.tickedFloat
import com.relentnet.cuttracker.ui.components.tickedInt
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

@Composable
fun TodayScreen(vm: AppViewModel, onSettings: () -> Unit, onOpenLift: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val now by vm.nowTime.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    var weighOpen by rememberSaveable { mutableStateOf(false) }
    var pickOpen by rememberSaveable { mutableStateOf(false) }
    var doneOpen by rememberSaveable { mutableStateOf(false) }

    val items = st.items
    val done = st.done
    val all = st.isSend
    val remaining = items.filter { it.id !in done }
    val finished = items.filter { it.id in done }
    val next = remaining.firstOrNull()
    val late = st.mode == null && !now.isBefore(st.settings.pickTime)
    val welcomeBack = st.missedRun >= 2 && st.settings.welcomeBackSeen != st.today

    Screen {
        Header(
            eyebrow = "${st.today.format(dayFmt)} · Day ${st.dayN} of ${st.settings.totalDays}",
            title = when {
                all -> "Sent"
                st.mode == DayMode.CLIMB -> "Climb day"
                st.mode == DayMode.OFF -> "Off day"
                else -> "Pick a day"
            },
            titleColor = if (all) c.good else c.text,
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSettings, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Eyebrow(if (st.settings.phase == 2) "Phase 2 Chest" else "Phase 1 Cut", color = c.accent)
                Icon(CutIcons.Gear, contentDescription = "Open settings", Modifier.size(16.dp), tint = c.accent)
            }
        }

        if (welcomeBack) WelcomeBack(st.chain.count, onOk = { vm.dismissWelcomeBack() })

        XpStrip(st)

        when {
            late -> LateCard(
                onOff = { vm.setMode(DayMode.OFF) },
                onClimb = { vm.setMode(DayMode.CLIMB, preCheck = setOf(ProgramSpec.ITEM_CLIMB)) },
            )
            st.mode == null -> PickCards(st, onPick = { haptics.tap(); vm.setMode(it) })
            else -> ModeChipRow(st, onChange = { weighOpen = false; pickOpen = false; vm.setMode(null) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow("Script")
                Eyebrow("${tickedInt(finished.size)} / ${items.size}")
            }
            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).dashedBorder(c.mutedBorder).padding(18.dp, 14.dp),
                ) {
                    Sub("Pick a day and the script shows up. Minimum day is weigh in + protein. That keeps the chain.")
                }
            }
            for (it in remaining) {
                val isNext = it === next
                CheckRow(
                    label = it.label,
                    sub = if (it.id == ProgramSpec.ITEM_WEIGH && st.lastWeighIn != null) "Yesterday ${ProgramEngine.fmt1(st.lastWeighIn!!.lb)}. Tap to log." else it.sub,
                    time = it.time, xp = it.xp, done = false, next = isNext,
                    onClick = {
                        when {
                            it.id == ProgramSpec.ITEM_WEIGH -> { weighOpen = !weighOpen; pickOpen = false }
                            it.pick && st.log.dinnerIndex == null -> { pickOpen = !pickOpen; weighOpen = false }
                            else -> { haptics.tap(); vm.toggleItem(it.id) }
                        }
                    },
                    onSwipeCheck = { haptics.tap(); vm.toggleItem(it.id, dinnerDefault = true) },
                )
                if (it.id == ProgramSpec.ITEM_WEIGH && weighOpen) {
                    WeighInline(
                        prefill = st.lastWeighIn?.lb ?: st.settings.startWeight,
                        onLog = { lb -> weighOpen = false; haptics.tap(); vm.logWeight(lb) },
                    )
                }
                if (it.pick && pickOpen) {
                    DinnerOptions(st.log.dinnerIndex) { i -> pickOpen = false; haptics.tap(); vm.pickDinner(it.id, i) }
                }
            }
            if (finished.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { doneOpen = !doneOpen }.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(CutIcons.Forward, null, Modifier.size(12.dp).rotate(if (doneOpen) 90f else 0f), tint = c.text2)
                    Text("DONE · ${finished.size}", color = c.text2, style = cutStyle(11, 700, 1f, 0.1f))
                    Box(Modifier.weight(1f).height(1.dp).background(c.hairline))
                }
                if (doneOpen) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (it in finished) CheckRow(
                        label = it.label, sub = null, time = null, xp = it.xp, done = true, compact = true,
                        onClick = { vm.toggleItem(it.id) },
                    )
                }
            }
        }

        Tiles(st)
    }
}

@Composable
private fun WelcomeBack(chain: Int, onOk: () -> Unit) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), border = c.freeze, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Welcome back.", color = c.text, style = cutStyle(18, 800, 1.1f))
            Sub("Chain paused at $chain. Nothing reset. Today counts, and a Min day (weigh in + protein) picks the chain right back up.")
            BigButton("Got it", onClick = onOk, modifier = Modifier.fillMaxWidth(), secondary = true, height = 44.dp, fontSize = 13)
        }
    }
}

@Composable
fun XpStrip(st: AppState) {
    val c = CutTheme.colors
    val level = st.level
    val next = ProgramEngine.nextLevelXp(level)
    val xp = tickedInt(st.xp)
    val progress by animateFloatAsState(ProgramEngine.levelProgress(st.xp), tween(450), label = "xpbar")
    val chainN = tickedInt(st.chain.count)
    Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp, 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Text("V$level", color = c.onAccent, style = cutStyle(18, 900, 1f, -0.02f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("$xp XP", color = c.text, style = cutStyle(12, 700, 1f, 0.08f, tabular = true))
                    Text(if (next != null) "${next - st.xp} to V${level + 1}" else "maxed", color = c.text2, style = cutStyle(11, 600, 1f, tabular = true))
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.hairline)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp)).background(c.accent))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(CutIcons.Link, null, Modifier.size(14.dp), tint = c.good)
                    Text("$chainN", color = c.text, style = cutStyle(18, 800, 1f, tabular = true))
                }
                val f = st.chain.freezesLeft
                val fz = "$f freeze" + if (f == 1) "" else "s"
                Text(
                    when {
                        st.chain.securedToday -> "secured today"
                        st.chain.paused -> "paused · $fz"
                        else -> fz
                    }.uppercase(),
                    color = c.text2, style = cutStyle(10, 600, 1f, 0.1f),
                )
            }
        }
    }
}

@Composable
private fun LateCard(onOff: () -> Unit, onClimb: () -> Unit) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), border = c.accent, borderWidth = 2.dp, radius = 14.dp, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("It's 7pm and no day is picked.", color = c.text, style = cutStyle(20, 800, 1.1f, -0.02f, display = true))
            Sub("Guessing Off day. Weigh in and hit protein and the chain is safe. Nothing else required tonight.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Yep, Off day", onClick = onOff, modifier = Modifier.weight(1f), height = 48.dp, fontSize = 13)
                BigButton("I climbed", onClick = onClimb, modifier = Modifier.weight(1f), secondary = true, height = 48.dp, fontSize = 13)
            }
        }
    }
}

@Composable
private fun PickCards(st: AppState, onPick: (DayMode) -> Unit) {
    val climb = ProgramEngine.script(DayMode.CLIMB, st.settings, null)
    val off = ProgramEngine.script(DayMode.OFF, st.settings, null)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PickCard(Modifier.weight(1f), CutIcons.Climb, "Climb day", "Climb, then 15 min push at the gym. ${climb.size} items, ${ProgramEngine.totalXp(climb)} xp.") { onPick(DayMode.CLIMB) }
        PickCard(Modifier.weight(1f), CutIcons.Off, "Off day", "15 min core + push at home. ${off.size} items, ${ProgramEngine.totalXp(off)} xp.") { onPick(DayMode.OFF) }
    }
}

@Composable
private fun PickCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(c.surface, shape)
            .border(2.dp, c.mutedBorder, shape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(14.dp, 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.ground), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.accent)
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = c.text, style = cutStyle(22, 800, 1f, -0.02f, display = true))
            Text(sub, color = c.text2, style = cutStyle(12, 500, 1.35f))
        }
    }
}

@Composable
private fun ModeChipRow(st: AppState, onChange: () -> Unit) {
    val c = CutTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface)
                .border(1.dp, c.hairline, RoundedCornerShape(999.dp))
                .clickable(onClick = onChange, role = Role.Button)
                .padding(start = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(if (st.mode == DayMode.CLIMB) CutIcons.Climb else CutIcons.Off, null, Modifier.size(13.dp), tint = c.onAccent)
            }
            Text(if (st.mode == DayMode.CLIMB) "Climb day" else "Off day", color = c.text2, style = cutStyle(12, 600, 1f, 0.04f))
            Text("Change", color = c.accent, style = cutStyle(12, 800, 1f, 0.06f))
        }
        if (st.isMin || st.isSend) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(CutIcons.Shield, null, Modifier.size(12.dp), tint = c.good)
            Text("CHAIN SAFE", color = c.good, style = cutStyle(10, 800, 1f, 0.1f))
        }
    }
}

@Composable
private fun WeighInline(prefill: Double, onLog: (Double) -> Unit) {
    val c = CutTheme.colors
    var text by rememberSaveable { mutableStateOf(ProgramEngine.fmt1(prefill)) }
    val focus = remember { FocusRequester() }
    val haptics = LocalHaptics.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun adjust(d: Double) { text = ProgramEngine.fmt1((text.toDoubleOrNull() ?: prefill) + d); haptics.tick() }
    fun go() { text.toDoubleOrNull()?.takeIf { it > 0 }?.let(onLog) }
    Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("-.2") { adjust(-0.2) }
        Field(text, { text = it }, Modifier.weight(1f), onDone = ::go, focusRequester = focus)
        Chip("+.2") { adjust(0.2) }
        Box(
            Modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(c.accent).clickable(onClick = ::go, role = Role.Button).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("LOG", color = c.onAccent, style = cutStyle(12, 800, 1f, 0.06f)) }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    Box(
        Modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(c.surface).border(1.dp, c.mutedBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick, role = Role.Button).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, style = cutStyle(12, 800, 1f, 0.06f, tabular = true)) }
}

@Composable
private fun DinnerOptions(selected: Int?, onPick: (Int) -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProgramSpec.dinners.forEachIndexed { i, d ->
            val shape = RoundedCornerShape(9.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(c.ground).border(1.dp, if (selected == i) c.accent else c.hairline, shape)
                    .clickable(onClick = { onPick(i) }, role = Role.Button).padding(12.dp, 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(d.name, color = c.text, style = cutStyle(14, 600, 1f))
                Text("${d.protein}g · ${d.calories} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
            }
        }
    }
}

@Composable
private fun Tiles(st: AppState) {
    val c = CutTheme.colors
    val pro = tickedInt(st.totals.protein)
    val cal = tickedInt(st.totals.calories)
    val target = st.settings.protein
    val hit = st.totals.protein >= target
    val bar by animateFloatAsState((st.totals.protein.toFloat() / target).coerceIn(0f, 1f), tween(350), label = "pro")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.weight(1.3f), padding = PaddingValues(14.dp, 12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TileLabel("Protein")
                    TileLabel("${st.totals.protein} / ${target}g")
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$pro", color = c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Text("g", color = c.text2, style = cutStyle(13, 600, 1f))
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.hairline)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(bar).clip(RoundedCornerShape(3.dp)).background(if (hit) c.good else c.accent))
                }
            }
        }
        if (!st.freeToday) Card(Modifier.weight(1f), padding = PaddingValues(14.dp, 12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TileLabel("Calories")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$cal", color = c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Text(" / ${st.settings.calories}", color = c.text2, style = cutStyle(13, 600, 1f, tabular = true))
                }
                Text("${(st.settings.calories - st.totals.calories).coerceAtLeast(0)} left", color = c.text2, style = cutStyle(11, 500, 1f, tabular = true))
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color) = drawBehind {
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    drawRoundRect(color, style = stroke, cornerRadius = CornerRadius(10.dp.toPx()))
}
