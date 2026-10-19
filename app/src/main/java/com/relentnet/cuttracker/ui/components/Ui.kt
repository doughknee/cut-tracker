package com.relentnet.cuttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** One scrolling screen column with the prototype's 28/20/12 padding and 14dp gaps. */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    bottomInset: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(CutTheme.colors.ground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
            .then(if (bottomInset) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = CutTheme.colors.text2, size: Int = 12) {
    Text(text.uppercase(), modifier, color = color, style = cutStyle(size, 600, 1f, 0.12f, tabular = true))
}

@Composable
fun H1(text: String, modifier: Modifier = Modifier, color: Color = CutTheme.colors.text) {
    Text(text, modifier.padding(top = 6.dp), color = color, style = cutStyle(44, 800, 1f, -0.02f, display = true))
}

/** Eyebrow + H1 header block. */
@Composable
fun Header(eyebrow: String, title: String, titleColor: Color = CutTheme.colors.text, right: (@Composable () -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow)
            right?.invoke()
        }
        H1(title, color = titleColor)
    }
}

@Composable
fun BackHeader(backLabel: String, title: String, onBack: () -> Unit) {
    Column {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onBack, role = Role.Button).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(CutIcons.Back, null, Modifier.size(14.dp), tint = CutTheme.colors.text2)
            Eyebrow(backLabel)
        }
        H1(title)
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    radius: Dp = 12.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    background: Color = CutTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(background, shape)
            .then(if (border != null) Modifier.border(borderWidth, border, shape) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 56dp primary (accent) or secondary (surface + muted border) button, uppercase label. */
@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    height: Dp = 56.dp,
    fontSize: Int = 15,
    enabled: Boolean = true,
    /** Show a green check for a beat before [onClick] runs; for Save and Log, where the page is about to leave. */
    confirm: Boolean = false,
) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(12.dp)
    var confirming by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bg by animateColorAsState(if (confirming) c.good else if (secondary) c.surface else c.accent, tween(160), label = "btn")
    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(bg, shape)
            .then(if (secondary && !confirming) Modifier.border(1.dp, c.mutedBorder, shape) else Modifier)
            .pressable(enabled = enabled && !confirming, haptic = false) {
                if (confirm) { confirming = true; scope.launch { delay(420); onClick(); delay(200); confirming = false } } else onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (confirming) Icon(CutIcons.Check, null, Modifier.size((fontSize + 6).dp), tint = c.ground)
        else Text(
            label.uppercase(), color = if (secondary) c.text else c.onAccent,
            style = cutStyle(fontSize, 800, 1f, 0.06f), textAlign = TextAlign.Center,
        )
    }
}

/** 36dp pill, used for Theme / Phase / Start selectors. */
@Composable
fun Pill(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 36.dp) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(if (on) c.accent else Color.Transparent, shape)
            .border(1.dp, if (on) c.accent else c.mutedBorder, shape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), color = if (on) c.onAccent else c.text, style = cutStyle(12, 800, 1f, 0.06f))
    }
}

/** Settings row: label on the left, control on the right, hairline below. */
@Composable
fun SettingsRow(label: String, last: Boolean = false, onClick: (() -> Unit)? = null, labelColor: Color = CutTheme.colors.text, control: @Composable RowScope.() -> Unit) {
    val c = CutTheme.colors
    Column(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = labelColor, style = cutStyle(15, 600), modifier = Modifier.weight(1f, fill = false))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = control)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, color: Color = CutTheme.colors.text2, size: Int = 13) {
    Text(text, modifier, color = color, style = cutStyle(size, 500, 1.4f))
}

/** Label-in-a-tile, 11sp uppercase. */
@Composable
fun TileLabel(text: String, color: Color = CutTheme.colors.text2) {
    Text(text.uppercase(), color = color, style = cutStyle(11, 600, 1f, 0.1f, tabular = true))
}

@Composable
fun SmallIcon(icon: ImageVector, tint: Color, size: Dp = 14.dp) {
    Icon(icon, null, Modifier.size(size), tint = tint)
}
