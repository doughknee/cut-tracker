package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.w
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** The home-screen widget: add it, and what it shows. */
@Composable
fun WidgetPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val context = LocalContext.current
    val last = st.weighIns.maxByOrNull { it.date }?.lb ?: s.startWeight
    Screen(bottomInset = true) {
        BackHeader("You", "Widget", onBack)
        Sub("Weigh in, pick the day and tick the next item without opening the app.", size = 12)

        // The widget's weigh-in face, as these settings draw it: its own light or dark, over a hint of wallpaper when see-through.
        val dark = when (p.widgetTheme) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }
        val wc = if (dark) com.relentnet.cuttracker.ui.theme.DarkCutColors else com.relentnet.cuttracker.ui.theme.LightCutColors
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(if (p.widgetSeeThrough) androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.accent, c.freeze)) else androidx.compose.ui.graphics.SolidColor(c.ground))
                .padding(if (p.widgetSeeThrough) 10.dp else 0.dp),
        ) {
        Card(Modifier.fillMaxWidth(), radius = 20.dp, background = wc.ground.copy(alpha = if (p.widgetSeeThrough) 0.72f else 1f), border = wc.hairline) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${st.today.dayOfWeek.name.take(3)} · DAY ${st.dayN}", color = wc.text2, style = cutStyle(10, 800, 1f, 0.08f))
                Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val step = ProgramEngine.fmt1(p.weighStep).removePrefix("0")
                    for ((label, mid) in listOf("−$step" to false, "" to true, "+$step" to false)) {
                        Box(
                            (if (mid) Modifier.weight(1f) else Modifier.width(54.dp)).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(wc.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!mid) Text(label, color = wc.text, style = cutStyle(15, 800, 1f, tabular = true))
                            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WEIGH IN", color = wc.accent, style = cutStyle(10, 800, 1f, 0.08f))
                                Text(if (p.widgetHideWeight) "• • •" else s.w(last), color = wc.text, style = cutStyle(28, 800, 1f, -0.02f, tabular = true))
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(9.dp)).background(wc.accent), contentAlignment = Alignment.Center) {
                    Text(if (p.widgetXp) "LOG · +${com.relentnet.cuttracker.data.ProgramSpec.XP_WEIGH} xp" else "LOG", color = wc.onAccent, style = cutStyle(11, 800, 1f, 0.08f))
                }
            }
        }
        }

        val widgets = context.getSystemService(android.appwidget.AppWidgetManager::class.java)
        KindCard("On the home screen", "") {
            if (widgets?.isRequestPinAppWidgetSupported == true) PlainRow("Add the widget", "your launcher asks where it goes") {
                LinkText("Add") { widgets.requestPinAppWidget(android.content.ComponentName(context, com.relentnet.cuttracker.widget.CutWidgetReceiver::class.java), null, null) }
            } else PlainRow("Add the widget", "hold an empty spot on the home screen, then Widgets, then Sendline") {}
            SwitchRow("Hide your weight", "dots instead of the number, for a phone others see. The step and the change from yesterday still show", p.widgetHideWeight) {
                vm.updatePrefs { it.copy(widgetHideWeight = !it.widgetHideWeight) }
            }
        }
        KindCard("The look", "") {
            ChoiceRow("Light or dark", "the widget on its own, whatever the app wears", listOf("phone" to "Follow the phone", "dark" to "Dark", "light" to "Light"), p.widgetTheme) { k ->
                vm.updatePrefs { it.copy(widgetTheme = k) }
            }
            SwitchRow("See-through", "the wallpaper shows faintly behind it", p.widgetSeeThrough) { vm.updatePrefs { it.copy(widgetSeeThrough = !it.widgetSeeThrough) } }
            SwitchRow("XP", "the +10 on the log button, the day cards and the rows", p.widgetXp) { vm.updatePrefs { it.copy(widgetXp = !it.widgetXp) } }
        }
        Sub("The minus and plus follow Weigh-in steps on the Today page.", size = 12)
    }
}
