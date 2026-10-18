package com.relentnet.cuttracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.relentnet.cuttracker.App
import com.relentnet.cuttracker.MainActivity
import com.relentnet.cuttracker.R
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.XpKind
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.engine.ScriptItem
import com.relentnet.cuttracker.ui.Haptics
import com.relentnet.cuttracker.ui.theme.DarkCutColors
import com.relentnet.cuttracker.ui.theme.LightCutColors
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Widget tokens: light by day, dark by night, from the same palettes as the app. */
private object W {
    private fun cp(l: Color, d: Color) = ColorProvider(day = l, night = d)
    val ground = cp(LightCutColors.ground, DarkCutColors.ground)
    val surface = cp(LightCutColors.surface, DarkCutColors.surface)
    val hairline = cp(LightCutColors.hairline, DarkCutColors.hairline)
    val mutedBorder = cp(LightCutColors.mutedBorder, DarkCutColors.mutedBorder)
    val text = cp(LightCutColors.text, DarkCutColors.text)
    val text2 = cp(LightCutColors.text2, DarkCutColors.text2)
    val dim = cp(LightCutColors.dim, DarkCutColors.dim)
    val accent = cp(LightCutColors.accent, DarkCutColors.accent)
    val onAccent = cp(LightCutColors.onAccent, DarkCutColors.onAccent)
    val good = cp(LightCutColors.good, DarkCutColors.good)
    val goodDim = cp(LightCutColors.goodDim, DarkCutColors.goodDim)
    val freeze = cp(LightCutColors.freeze, DarkCutColors.freeze)
    val miss = cp(LightCutColors.miss, DarkCutColors.miss)
    val future = cp(LightCutColors.future, DarkCutColors.future)
}

private val ItemKey = ActionParameters.Key<String>("item")
private val ModeKey = ActionParameters.Key<String>("mode")
private val DeltaKey = ActionParameters.Key<Double>("delta")
/** The stepper's unlogged weight, kept per widget instance. */
private val DraftKey = doublePreferencesKey("draft")
private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

private fun eyebrow(color: ColorProvider = W.text2, size: Int = 9) = TextStyle(color = color, fontSize = size.sp, fontWeight = FontWeight.Bold)
private fun body(color: ColorProvider = W.text, size: Int = 12, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

/** 4x2: morning stepper, then the day pick, then the dashboard with the checklist and the day's numbers. */
class CutWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.of(context).repo
        val initial = repo.state.first { it.loaded }
        // Collected inside the composition: Glance keeps this session alive and re-renders on every state change.
        provideContent {
            val st by repo.state.collectAsState(initial)
            val prefs = currentState<Preferences>()
            Wide(st, prefs[DraftKey])
        }
    }
}

/** 2x1: chain number and the Send bar only. */
class CutSmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.of(context).repo
        val initial = repo.state.first { it.loaded }
        provideContent { val st by repo.state.collectAsState(initial); Small(st) }
    }
}

class CutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CutWidget()
}

class CutSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CutSmallWidget()
}

/** Checks or unchecks an item straight from the widget, with the same haptic, then re-renders. */
class CheckItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemKey] ?: return
        val app = App.of(context)
        val st = app.repo.state.first { it.loaded }
        if (id !in st.done) Haptics(context) { st.settings.haptics }.tap()
        app.repo.toggleItem(id, dinnerDefault = true)
        updateWidgets(context)
    }
}

/** Steps the unlogged weight by [DeltaKey]. */
class StepWeightAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DeltaKey] ?: return
        val app = App.of(context)
        val st = app.repo.state.first { it.loaded }
        Haptics(context) { st.settings.haptics }.tick()
        updateAppWidgetState(context, glanceId) { p ->
            val cur = p[DraftKey] ?: draftDefault(st)
            p[DraftKey] = ((cur + delta) * 10).roundToInt() / 10.0
        }
        CutWidget().update(context, glanceId)
    }
}

/** Logs the stepper's weight as today's weigh-in. */
class LogWeightAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = App.of(context)
        val st = app.repo.state.first { it.loaded }
        var draft = draftDefault(st)
        updateAppWidgetState(context, glanceId) { p -> draft = p[DraftKey] ?: draft; p.remove(DraftKey) }
        Haptics(context) { st.settings.haptics }.tap()
        app.repo.logWeight(draft)
        updateWidgets(context)
    }
}

/** Picks today's mode from the widget. */
class PickModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[ModeKey]?.let { runCatching { DayMode.valueOf(it) }.getOrNull() } ?: return
        val app = App.of(context)
        val st = app.repo.state.first { it.loaded }
        Haptics(context) { st.settings.haptics }.tap()
        app.repo.setMode(mode)
        updateWidgets(context)
    }
}

suspend fun updateWidgets(context: Context) {
    runCatching { CutWidget().updateAll(context) }
    runCatching { CutSmallWidget().updateAll(context) }
}

private fun draftDefault(st: AppState): Double = st.weighInBefore(st.today)?.lb ?: st.lastWeighIn?.lb ?: st.settings.startWeight

private fun fmt1(v: Double) = ProgramEngine.fmt1(v)

@Composable
private fun Wide(st: AppState, draft: Double?) {
    val weighed = ProgramSpec.ITEM_WEIGH in st.done || st.weighIns.any { it.date == st.today }
    Box(GlanceModifier.fillMaxSize().background(W.ground).cornerRadius(16.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
        when {
            !weighed -> WeighStage(st, draft ?: draftDefault(st))
            st.mode == null -> PickStage(st)
            else -> Dashboard(st)
        }
    }
}

@Composable
private fun ChainBadge(st: AppState, safe: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (safe) {
            Image(ImageProvider(R.drawable.ic_w_shield), null, GlanceModifier.size(10.dp), colorFilter = ColorFilter.tint(W.good))
            Spacer(GlanceModifier.width(4.dp))
            Text("CHAIN SAFE", style = eyebrow(W.good))
        } else {
            Image(ImageProvider(R.drawable.ic_notification), null, GlanceModifier.size(11.dp), colorFilter = ColorFilter.tint(W.good))
            Spacer(GlanceModifier.width(4.dp))
            Text("CHAIN", style = eyebrow())
        }
        Spacer(GlanceModifier.width(4.dp))
        Text("${st.chain.count}", style = body(size = 13, bold = true))
    }
}

@Composable
private fun WeighStage(st: AppState, draft: Double) {
    val yesterday = st.weighInBefore(st.today)?.lb
    val delta = if (yesterday == null) null else ((draft - yesterday) * 10).roundToInt() / 10.0
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${st.today.format(dateFmt).uppercase()} · DAY ${st.dayN}", style = eyebrow(), modifier = GlanceModifier.defaultWeight())
            ChainBadge(st, safe = false)
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", -0.2)
            Spacer(GlanceModifier.width(8.dp))
            Column(
                GlanceModifier.defaultWeight().fillMaxHeight().background(W.surface).cornerRadius(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("WEIGH IN", style = eyebrow(W.accent))
                Text(fmt1(draft), style = TextStyle(color = W.text, fontSize = 28.sp, fontWeight = FontWeight.Bold))
                Text(
                    when {
                        delta == null -> "first reading"
                        delta == 0.0 -> "same as yesterday"
                        else -> (if (delta > 0) "+" else "") + fmt1(delta) + " vs yesterday"
                    },
                    style = eyebrow(size = 9).copy(fontWeight = FontWeight.Medium),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            StepButton("+", 0.2)
        }
        Spacer(GlanceModifier.height(6.dp))
        Box(
            GlanceModifier.fillMaxWidth().height(30.dp).background(W.accent).cornerRadius(9.dp).clickable(actionRunCallback<LogWeightAction>()),
            contentAlignment = Alignment.Center,
        ) { Text("LOG · +${ProgramSpec.XP_WEIGH} XP", style = eyebrow(W.onAccent, 11)) }
    }
}

@Composable
private fun StepButton(label: String, delta: Double) {
    Box(
        GlanceModifier.width(46.dp).fillMaxHeight().background(W.surface).cornerRadius(10.dp)
            .clickable(actionRunCallback<StepWeightAction>(actionParametersOf(DeltaKey to delta))),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = TextStyle(color = W.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)) }
}

@Composable
private fun PickStage(st: AppState) {
    val lb = st.weighIns.firstOrNull { it.date == st.today }?.lb
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text("DAY ${st.dayN} · ", style = eyebrow())
                Image(ImageProvider(R.drawable.ic_w_check), null, GlanceModifier.size(10.dp), colorFilter = ColorFilter.tint(W.good))
                Spacer(GlanceModifier.width(3.dp))
                Text((if (lb != null) fmt1(lb) + " " else "") + "LOGGED · +${ProgramSpec.XP_WEIGH}", style = eyebrow(W.text))
            }
            Text("PICK A DAY", style = eyebrow(W.accent))
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            val climb = ProgramEngine.script(DayMode.CLIMB, st.settings, null)
            val off = ProgramEngine.script(DayMode.OFF, st.settings, null)
            PickCard(GlanceModifier.defaultWeight(), DayMode.CLIMB, R.drawable.ic_w_climb, "Climb day", "${climb.size} items · ${ProgramEngine.totalXp(climb)} xp")
            Spacer(GlanceModifier.width(8.dp))
            PickCard(GlanceModifier.defaultWeight(), DayMode.OFF, R.drawable.ic_w_off, "Off day", "${off.size} items · ${ProgramEngine.totalXp(off)} xp")
            Spacer(GlanceModifier.width(8.dp))
            val rest = ProgramEngine.script(DayMode.REST, st.settings, null)
            PickCard(GlanceModifier.defaultWeight(), DayMode.REST, R.drawable.ic_w_rest, "Rest day", "${rest.size} items · ${ProgramEngine.totalXp(rest)} xp")
        }
    }
}

@Composable
private fun PickCard(modifier: GlanceModifier, mode: DayMode, icon: Int, title: String, sub: String) {
    Column(
        modifier.fillMaxHeight().background(W.surface).cornerRadius(10.dp).padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionRunCallback<PickModeAction>(actionParametersOf(ModeKey to mode.name))),
    ) {
        Image(ImageProvider(icon), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(W.accent))
        Spacer(GlanceModifier.defaultWeight())
        Text(title, style = body(size = 15, bold = true))
        Text(sub, style = eyebrow().copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

private data class WRow(val item: ScriptItem, val done: Boolean, val next: Boolean)

/** Up to [n] rows: the next items to do, then the most recently completed (tap to undo); older ones fold into the summary line. */
private fun rowsFor(st: AppState, n: Int = 4): List<WRow> {
    val items = st.items
    val remaining = items.filter { it.id !in st.done }
    val lastAt = st.xpEvents.filter { it.date == st.today && it.kind == XpKind.ITEM }.associate { it.ref to it.at }
    val recent = items.filter { it.id in st.done && it.id != ProgramSpec.ITEM_WEIGH }.sortedByDescending { lastAt[it.id]?.toEpochMilli() ?: 0L }
    val todo = remaining.take(n - minOf(1, recent.size))
    val done = recent.take(n - todo.size)
    return todo.mapIndexed { i, it -> WRow(it, false, i == 0) } + done.map { WRow(it, true, false) }
}

@Composable
private fun Dashboard(st: AppState) {
    val safe = st.isMin || st.isSend
    val sent = st.isSend
    val target = st.settings.protein
    val protein = st.totals.protein
    val lb = st.weighIns.firstOrNull { it.date == st.today }?.lb
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WeekHolds(st)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                when (st.mode) { DayMode.CLIMB -> "CLIMB DAY"; DayMode.REST -> "REST DAY"; else -> "OFF DAY" } + " · DAY ${st.dayN}",
                style = eyebrow(), modifier = GlanceModifier.defaultWeight(),
            )
            ChainBadge(st, safe)
        }
        Spacer(GlanceModifier.height(6.dp))
        Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (sent) {
                Column(GlanceModifier.fillMaxWidth().defaultWeight().background(W.surface).cornerRadius(8.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sent", style = TextStyle(color = W.good, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Text("Every item checked. Hold went green.", style = eyebrow(size = 10).copy(fontWeight = FontWeight.Medium), maxLines = 1)
                }
            } else {
                for (r in rowsFor(st)) { ItemRow(r); Spacer(GlanceModifier.height(3.dp)) }
                Spacer(GlanceModifier.defaultWeight())
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_w_check), null, GlanceModifier.size(10.dp), colorFilter = ColorFilter.tint(W.good))
            Spacer(GlanceModifier.width(5.dp))
            Text("${st.totals.done} of ${st.totals.total} done", style = eyebrow(W.dim, 10).copy(fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
            Text("$protein / ${target}g", style = eyebrow(if (protein >= target) W.good else W.text, 10))
            Text("  ·  ", style = eyebrow(W.dim, 10))
            Text(if (lb != null) fmt1(lb) + "  ·  " else "", style = eyebrow(W.text2, 10))
            Text(if (safe) "Safe day" else "${(target - protein).coerceAtLeast(0)}g to safe", style = eyebrow(if (safe) W.good else W.text2, 10))
        }
        Spacer(GlanceModifier.height(5.dp))
        LinearProgressIndicator(
            progress = if (st.totals.total == 0) 0f else st.totals.done.toFloat() / st.totals.total,
            modifier = GlanceModifier.fillMaxWidth().height(3.dp),
            color = if (sent) W.good else W.accent, backgroundColor = W.hairline,
        )
    }
}

@Composable
private fun ItemRow(r: WRow) {
    val action = if (r.item.id == ProgramSpec.ITEM_WEIGH) actionStartActivity<MainActivity>()
    else actionRunCallback<CheckItemAction>(actionParametersOf(ItemKey to r.item.id))
    Row(
        GlanceModifier.fillMaxWidth().height(24.dp).cornerRadius(7.dp).background(if (r.done) W.ground else W.surface).padding(horizontal = 7.dp).clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(13.dp).cornerRadius(4.dp).background(if (r.done) W.accent else W.mutedBorder), contentAlignment = Alignment.Center) {
            if (r.done) Image(ImageProvider(R.drawable.ic_w_check), null, GlanceModifier.size(8.dp), colorFilter = ColorFilter.tint(W.onAccent))
            else Box(GlanceModifier.size(9.dp).cornerRadius(2.dp).background(W.surface)) {}
        }
        Spacer(GlanceModifier.width(7.dp))
        Text(
            r.item.label, maxLines = 1, modifier = GlanceModifier.defaultWeight(),
            style = body(if (r.done) W.text2 else W.text).copy(textDecoration = if (r.done) TextDecoration.LineThrough else TextDecoration.None),
        )
        Text(
            if (r.next || r.done) "+${r.item.xp}" else r.item.time,
            style = eyebrow(if (r.done) W.good else if (r.next) W.accent else W.text2),
        )
    }
}

/** This week's seven holds, tiny. */
@Composable
private fun WeekHolds(st: AppState) {
    val ws = ProgramEngine.weekStart(st.today)
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 7) {
            val d = ws.plusDays(i.toLong())
            val r = st.results[d] ?: DayResult.FUTURE
            val color = when (r) {
                DayResult.SEND -> W.good
                DayResult.MIN -> W.goodDim
                DayResult.FREEZE -> W.freeze
                DayResult.MISSED -> W.miss
                DayResult.TODAY -> W.accent
                else -> W.future
            }
            // Padding, not spacers: a Glance Row holds at most 10 children.
            Box(GlanceModifier.padding(end = if (i < 6) 2.dp else 0.dp)) {
                Box(GlanceModifier.width(11.dp).height(9.dp).cornerRadius(2.dp).background(color).padding(1.dp), contentAlignment = Alignment.Center) {
                    if (r == DayResult.TODAY) Box(GlanceModifier.fillMaxSize().cornerRadius(1.dp).background(W.ground)) {}
                }
            }
        }
    }
}

@Composable
private fun Small(st: AppState) {
    Box(GlanceModifier.fillMaxSize().background(W.ground).cornerRadius(16.dp).padding(12.dp).clickable(actionStartActivity<MainActivity>())) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_notification), null, GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(W.good))
            Spacer(GlanceModifier.width(4.dp))
            Text("${st.chain.count}", style = TextStyle(color = W.text, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(10.dp))
            LinearProgressIndicator(
                progress = if (st.items.isEmpty()) 0f else st.totals.done.toFloat() / st.items.size,
                modifier = GlanceModifier.defaultWeight().height(6.dp),
                color = if (st.isSend) W.good else W.accent, backgroundColor = W.hairline,
            )
        }
    }
}

@Suppress("unused")
private val keepColorProviderImport = ColorProvider(Color.Black)
