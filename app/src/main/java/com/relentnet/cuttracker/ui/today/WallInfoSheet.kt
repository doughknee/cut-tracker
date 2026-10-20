package com.relentnet.cuttracker.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** The legend behind the info button: shape is the kind of day, color is how it went. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WallInfoSheet(onDismiss: () -> Unit) {
    val c = CutTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, dragHandle = {
        Box(Modifier.padding(top = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.mutedBorder))
    }) {
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp, 10.dp, 20.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Reading the wall", color = c.text, style = cutStyle(20, 800, 1f, -0.02f, display = true))
            Sub(CutTheme.vocab.wallInfo)
            Text("SHAPE · KIND OF DAY", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
            Grid(
                listOf(
                    { ShapeKey(HoldShape.JUG, c.good, "Off day") }, { ShapeKey(HoldShape.CRIMP, c.good, CutTheme.vocab.trainDay) },
                    { ShapeKey(HoldShape.SLOPER, c.good, "Rest day") }, { ShapeKey(HoldShape.PINCH, c.miss, "No day picked") },
                ),
            )
            Text("COLOR · HOW IT WENT", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
            Grid(
                listOf(
                    { ColorKey(c.good, "${CutTheme.vocab.send}, whole script") }, { ColorKey(c.goodDim, "Min day, chain safe") },
                    { ColorKey(c.freeze, "Freeze or skip") }, { ColorKey(c.miss, "Missed") },
                    { ColorKey(Color.Transparent, "Today, still open", ring = c.accent) },
                ),
            )
            Text("THE THREE RULES", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
            Rule("Min day keeps the chain.", "Weigh in and hit protein. Even if the rest of the day slips, it counts.")
            Rule("Two freezes a month.", "They cover your first two missed days automatically. A third miss pauses the chain.")
            Rule("The chain never resets.", "A miss pauses it; the next good day picks it up at the same number.")
            BigButton("Got it", onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), secondary = true, height = 44.dp, fontSize = 12)
        }
    }
}

@Composable
private fun Rule(title: String, body: String) {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = c.text, style = cutStyle(13, 700, 1.2f))
        Text(body, color = c.text2, style = cutStyle(12, 500, 1.35f))
    }
}

@Composable
private fun Grid(cells: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in cells.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (cell in row) Box(Modifier.weight(1f)) { cell() }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShapeKey(shape: HoldShape, fill: Color, label: String) {
    val c = CutTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Hold(shape, fill, 28.dp)
        Text(label, color = c.text, style = cutStyle(13, 600, 1f))
    }
}

@Composable
private fun ColorKey(fill: Color, label: String, ring: Color? = null) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(5.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(16.dp).clip(shape).background(fill).then(if (ring != null) Modifier.border(2.dp, ring, shape) else Modifier))
        Text(label, color = c.text, style = cutStyle(13, 600, 1f))
    }
}
