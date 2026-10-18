package com.relentnet.cuttracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.relentnet.cuttracker.App
import com.relentnet.cuttracker.MainActivity
import com.relentnet.cuttracker.R
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.ui.Haptics
import com.relentnet.cuttracker.ui.theme.DarkCutColors
import com.relentnet.cuttracker.ui.theme.LightCutColors
import kotlinx.coroutines.flow.first

/** Widget tokens: light by day, dark by night, from the same palettes as the app. */
private object W {
    private fun cp(l: Color, d: Color) = ColorProvider(day = l, night = d)
    val ground = cp(LightCutColors.ground, DarkCutColors.ground)
    val surface = cp(LightCutColors.surface, DarkCutColors.surface)
    val hairline = cp(LightCutColors.hairline, DarkCutColors.hairline)
    val text = cp(LightCutColors.text, DarkCutColors.text)
    val text2 = cp(LightCutColors.text2, DarkCutColors.text2)
    val accent = cp(LightCutColors.accent, DarkCutColors.accent)
    val good = cp(LightCutColors.good, DarkCutColors.good)
}

private val ItemKey = ActionParameters.Key<String>("item")

/** 4x2: mode + up next on the left, 7-segment Send bar in the middle, chain on the right. */
class CutWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = App.of(context).repo
        val initial = repo.state.first { it.loaded }
        // Collected inside the composition: Glance keeps this session alive and re-renders on every state change.
        provideContent { val st by repo.state.collectAsState(initial); Wide(st) }
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

/** Checks the "Up next" item straight from the widget, with the same haptic, then re-renders. */
class CheckItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemKey] ?: return
        val app = App.of(context)
        val st = app.repo.state.first { it.loaded }
        if (id in st.done) return
        Haptics(context) { st.settings.haptics }.tap()
        app.repo.toggleItem(id, dinnerDefault = true)
        updateWidgets(context)
    }
}

suspend fun updateWidgets(context: Context) {
    runCatching { CutWidget().updateAll(context) }
    runCatching { CutSmallWidget().updateAll(context) }
}

private fun modeLabel(st: AppState) = when (st.mode) {
    DayMode.CLIMB -> if (st.isSend) "Sent" else "Climb day"
    DayMode.OFF -> if (st.isSend) "Sent" else "Off day"
    null -> "Pick a day"
}

@Composable
private fun Wide(st: AppState) {
    val next = st.items.firstOrNull { it.id !in st.done }
    Box(GlanceModifier.fillMaxSize().background(W.ground).cornerRadius(16.dp).padding(14.dp)) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight().clickable(actionStartActivity<MainActivity>()), verticalAlignment = Alignment.CenterVertically) {
                Text(modeLabel(st), style = TextStyle(color = if (st.isSend) W.good else W.text, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                if (next != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    Column(
                        GlanceModifier.background(W.surface).cornerRadius(8.dp).padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable(
                                // Weigh-in needs a number, so it opens the app; everything else checks in place.
                                if (next.id == ProgramSpec.ITEM_WEIGH) actionStartActivity<MainActivity>()
                                else actionRunCallback<CheckItemAction>(actionParametersOf(ItemKey to next.id)),
                            ),
                    ) {
                        Text("UP NEXT", style = TextStyle(color = W.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                        Text(next.label, style = TextStyle(color = W.text, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    }
                } else if (st.mode != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text("Every item checked.", style = TextStyle(color = W.text2, fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.width(96.dp), verticalAlignment = Alignment.CenterVertically) { Segments(st) }
            Spacer(GlanceModifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.ic_notification), null, GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(W.good))
                    Spacer(GlanceModifier.width(4.dp))
                    Text("${st.chain.count}", style = TextStyle(color = W.text, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                }
                if (st.isMin || st.isSend) Text("CHAIN SAFE", style = TextStyle(color = W.good, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                else Text("CHAIN", style = TextStyle(color = W.text2, fontSize = 9.sp, fontWeight = FontWeight.Bold))
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
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) { Segments(st) }
        }
    }
}

/** One segment per script item; filled accent as items are checked, all green on a Send. */
@Composable
private fun Segments(st: AppState) {
    val total = st.items.size.coerceAtLeast(1)
    val done = st.totals.done
    // Glance allows at most 10 children per Row, so the gaps are padding, not spacers.
    Row(GlanceModifier.fillMaxWidth()) {
        for (i in 0 until if (st.items.isEmpty()) 7 else total) {
            val on = i < done
            Box(GlanceModifier.defaultWeight().height(6.dp).padding(horizontal = 1.5.dp)) {
                Box(GlanceModifier.fillMaxSize().cornerRadius(3.dp).background(if (!on) W.hairline else if (st.isSend) W.good else W.accent)) {}
            }
        }
    }
}

@Suppress("unused")
private val keepColorProviderImport = ColorProvider(Color.Black)
