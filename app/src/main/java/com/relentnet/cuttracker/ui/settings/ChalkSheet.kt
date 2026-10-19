package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.data.CustomLook
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.Decor
import com.relentnet.cuttracker.ui.theme.LocalCutColors
import com.relentnet.cuttracker.ui.theme.LocalDecor
import com.relentnet.cuttracker.ui.theme.cutStyle
import com.relentnet.cuttracker.ui.theme.palette
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Chalk a look: drag the ring for the hue, pick a stone, name it. The sends and the freeze follow the hue. */
@Composable
fun ChalkSheet(look: CustomLook?, dark: Boolean, onDismiss: () -> Unit, onSave: (CustomLook) -> Unit, onDelete: (() -> Unit)?) {
    val c = CutTheme.colors
    val haptics = LocalHaptics.current
    val clipboard = LocalClipboardManager.current
    var hue by remember { mutableIntStateOf(look?.hue ?: 24) }
    var stone by remember { mutableIntStateOf(look?.stone ?: 0) }
    var name by remember { mutableStateOf(look?.name ?: "") }
    val draft = CustomLook(look?.id ?: "", name, hue, stone)
    val p = draft.palette()
    val live = p.colors(dark)
    Sheet(if (look == null) "Chalk your own" else "Edit look", onDismiss) {
        Text("One hue on one stone. The sends and the freeze pick themselves so nothing clashes.", color = c.text2, style = cutStyle(12, 500, 1.4f))
        CompositionLocalProvider(LocalCutColors provides live, LocalDecor provides Decor.NONE) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(live.wall).border(1.dp, live.hairline, RoundedCornerShape(12.dp))) {
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Hold(HoldShape.JUG, live.good, 28.dp); Hold(HoldShape.CRIMP, live.goodDim, 24.dp); Hold(HoldShape.SLOPER, live.freeze, 26.dp); Hold(HoldShape.PINCH, live.accent, 28.dp)
                }
                Text(name.ifBlank { draft.autoName }, color = live.text, style = cutStyle(12, 800, 1f, display = true), modifier = Modifier.align(Alignment.BottomStart).padding(10.dp, 6.dp))
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HueRing(hue, live.accent, onHue = { if (it != hue) { hue = it } })
        }
        SheetLabel("Stone")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, st) in CustomLook.STONES.withIndex()) {
                val f = CustomLook(draft.id, "", hue, i).face(dark)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(f.ground)).border(if (i == stone) 2.dp else 1.dp, if (i == stone) c.accent else Color(f.muted), CircleShape)
                            .clickable(role = Role.RadioButton) { haptics.tick(); stone = i },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(14.dp).clip(CircleShape).background(Color(f.surface)).border(1.dp, Color(f.hairline), CircleShape)) }
                    Text(st.name, color = if (i == stone) c.text else c.text2, style = cutStyle(9, 800, 1f, 0.06f))
                }
            }
        }
        SheetField("Name", name, { name = it.take(24) }, text = true, placeholder = draft.autoName, last = true)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.ground).clickable { clipboard.setText(AnnotatedString(draft.code)); haptics.tick() }.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SHARE CODE", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
                Text(draft.code, color = c.text, style = cutStyle(18, 800, 1f, 0.08f, tabular = true))
            }
            Text("tap to copy", color = c.text2, style = cutStyle(11, 600, 1f))
        }
        SheetButtons(
            if (look == null) "Chalk it" else "Save", onPrimary = { onSave(draft.copy(name = name.trim().ifBlank { draft.autoName })) },
            secondary = if (onDelete != null) "Delete" else null, onSecondary = onDelete,
        )
    }
}

/** The whole wheel in a ring; drag or tap anywhere on it. Hue 0 sits at three o'clock, as the sweep gradient draws it. */
@Composable
private fun HueRing(hue: Int, accent: Color, onHue: (Int) -> Unit) {
    val c = CutTheme.colors
    val haptics = LocalHaptics.current
    val wheel = remember { List(13) { Color.hsv((it * 30 % 360).toFloat(), 1f, 1f) } }
    fun pick(p: Offset, w: Float, h: Float) {
        val deg = Math.toDegrees(atan2((p.y - h / 2).toDouble(), (p.x - w / 2).toDouble())).toInt()
        onHue(((deg % 360) + 360) % 360)
    }
    Canvas(
        Modifier.size(196.dp)
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()); haptics.tick() } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) }) { change, _ -> change.consume(); pick(change.position, size.width.toFloat(), size.height.toFloat()) }
            },
    ) {
        val band = 30.dp.toPx()
        val r = size.minDimension / 2 - band / 2 - 6.dp.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(Brush.sweepGradient(wheel, center), r, center, style = Stroke(band))
        drawCircle(c.ground.copy(alpha = 0.35f), r - band / 2, center)
        val a = Math.toRadians(hue.toDouble())
        val knob = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
        drawCircle(c.surface, band / 2 + 4.dp.toPx(), knob)
        drawCircle(accent, band / 2 - 1.dp.toPx(), knob)
        drawCircle(c.text, band / 2 + 4.dp.toPx(), knob, style = Stroke(1.5.dp.toPx()))
        drawCircle(accent, r - band / 2 - 10.dp.toPx(), center)
    }
}
