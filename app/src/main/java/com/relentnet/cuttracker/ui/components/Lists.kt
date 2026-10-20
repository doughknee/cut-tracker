package com.relentnet.cuttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

// The list vocabulary every Beta page shares: a card per group with hairline rows and a number column, a switch where
// a row is on or off, a bottom sheet to edit, a FAB to add.

/** A group card: title, count pill, optional fold chevron and pen. Rows go in [content]. */
@Composable
fun KindCard(
    title: String, count: String, modifier: Modifier = Modifier,
    folded: Boolean = false, onFold: (() -> Unit)? = null, onEdit: (() -> Unit)? = null, editing: Boolean = false,
    sub: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CutTheme.colors
    Card(modifier.fillMaxWidth(), radius = 14.dp) {
        Row(
            Modifier.fillMaxWidth().then(if (onFold != null) Modifier.clickable(onClick = onFold, role = Role.Button) else Modifier).padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = c.text, style = cutStyle(18, 800, 1f, -0.01f, display = true), modifier = Modifier.padding(vertical = 10.dp))
            if (count.isNotEmpty()) Text(count, color = c.text2, style = cutStyle(11, 800, 1f, tabular = true), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.ground).padding(6.dp, 3.dp))
            Box(Modifier.weight(1f))
            if (onFold != null) {
                val rot by animateFloatAsState(if (folded) -90f else 0f, tween(200), label = "fold")
                Icon(CutIcons.Forward, null, Modifier.size(14.dp).rotate(90f + rot), tint = c.text2)
            }
            if (onEdit != null) Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (editing) c.ground else Color.Transparent).clickable(onClick = onEdit, role = Role.Button),
                contentAlignment = Alignment.Center,
            ) { Icon(CutIcons.Gear, null, Modifier.size(14.dp), tint = if (editing) c.accent else c.text2) }
        }
        if (sub != null) Text(sub, color = c.text2, style = cutStyle(12, 500, 1.4f), modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp))
        AnimatedVisibility(!folded, enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 420f)) + fadeIn(), exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 420f)) + fadeOut()) {
            Column { content() }
        }
    }
}

/** A hairline row with a number column: tap opens its editor, or flips its switch when [onSwitch] is set. */
@Composable
fun NumRow(
    num: String, title: String, modifier: Modifier = Modifier,
    unit: String? = null, sub: String? = null, right: String? = null, rightColor: Color = CutTheme.colors.text2,
    on: Boolean? = null, onSwitch: (() -> Unit)? = null, chevron: Boolean = on == null,
    lead: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null,
) {
    val c = CutTheme.colors
    val dimmed = on == false
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Row(
            // A row with a switch and nothing else to open toggles from anywhere on the row.
            Modifier.fillMaxWidth().then((onClick ?: onSwitch)?.let { Modifier.pressable(haptic = false, onClick = it) } ?: Modifier).padding(14.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (lead != null) lead()
            Column(Modifier.width(48.dp)) {
                Text(num, color = if (dimmed) c.text2 else c.text, style = cutStyle(17, 800, 1f, -0.02f, display = true, tabular = true), maxLines = 1, overflow = TextOverflow.Clip)
                if (unit != null) Text(unit, color = c.text2, style = cutStyle(9, 700, 1f, 0.04f), modifier = Modifier.padding(top = 2.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = if (dimmed) c.text2 else c.text, style = cutStyle(15, 700, 1.1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!sub.isNullOrBlank()) Text(sub, color = c.text2, style = cutStyle(11, 500, 1.2f, tabular = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (right != null) Text(right, color = rightColor, style = cutStyle(11, 700, 1f, tabular = true))
            if (on != null && onSwitch != null) Switch(on, title, onSwitch)
            if (chevron) Icon(CutIcons.Forward, null, Modifier.size(12.dp), tint = c.text2)
        }
    }
}

/** The "+ Add" line at the foot of a card. */
@Composable
fun AddRow(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Box(Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button).padding(13.dp), contentAlignment = Alignment.Center) {
            Text("+ ${label.uppercase()}", color = c.accent, style = cutStyle(12, 800, 1f, 0.06f))
        }
    }
}

/** The 56dp orange add button; the caller puts it in a Box at BottomEnd. */
@Composable
fun Fab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(end = 20.dp, bottom = 24.dp).size(56.dp).shadow(14.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape).background(c.accent).pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(CutIcons.Plus, "Add", Modifier.size(26.dp), tint = c.onAccent) }
}

/** Filter chips with one orange pill that slides to the chosen one. [extra] renders after the chips (the + Kind chip). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, extra: (@Composable () -> Unit)? = null) {
    val c = CutTheme.colors
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    val density = LocalDensity.current
    val target = bounds[selected]
    val x by animateDpAsState(with(density) { (target?.left ?: 0f).toDp() }, spring(dampingRatio = 0.9f, stiffness = 380f), label = "px")
    val y by animateDpAsState(with(density) { (target?.top ?: 0f).toDp() }, spring(dampingRatio = 0.9f, stiffness = 380f), label = "py")
    val w by animateDpAsState(with(density) { (target?.width ?: 0f).toDp() }, spring(dampingRatio = 0.9f, stiffness = 380f), label = "pw")
    val h by animateDpAsState(with(density) { (target?.height ?: 0f).toDp() }, spring(dampingRatio = 0.9f, stiffness = 380f), label = "ph")
    Box(Modifier.fillMaxWidth()) {
        if (target != null) Box(Modifier.offset(x, y).size(w, h).clip(RoundedCornerShape(8.dp)).background(c.accent))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((key, label) in options) {
                val on = key == selected
                Chip(label, on = on, transparentWhenOn = true, modifier = Modifier.onGloballyPositioned { bounds[key] = it.boundsInParent() }) { onSelect(key) }
            }
            extra?.invoke()
        }
    }
}

/** 32dp chip. Chips in a [ChipRow] go transparent when on so the sliding pill shows through. */
@Composable
fun Chip(label: String, on: Boolean, modifier: Modifier = Modifier, transparentWhenOn: Boolean = false, dashed: Boolean = false, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.height(32.dp).clip(shape)
            .background(if (on && !transparentWhenOn) c.accent else Color.Transparent, shape)
            .border(1.dp, when { on -> if (transparentWhenOn) Color.Transparent else c.accent; dashed -> c.mutedBorder; else -> c.mutedBorder }, shape)
            .clickable(onClick = onClick, role = Role.Button).padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (on) c.onAccent else if (dashed) c.text2 else c.text, style = cutStyle(12, 700, 1f)) }
}

/**
 * A compact [−][value][+] with a unit after it. The buttons repeat while held; with [onType] the number itself opens
 * the keyboard, so a far-off value is typed rather than tapped.
 */
@Composable
fun MiniStepper(
    value: String, onMinus: () -> Unit, onPlus: () -> Unit, unit: String? = null, color: Color = CutTheme.colors.text,
    onType: ((Double) -> Unit)? = null,
) {
    val c = CutTheme.colors
    var typing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        StepBtn("−", "Less", onMinus)
        if (typing && onType != null) {
            var text by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(value.replace(",", "")) }
            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
            var had by remember { androidx.compose.runtime.mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
            fun commit() { if (typing) { text.num()?.let(onType); typing = false } }
            Field(
                text, { text = it },
                Modifier.width(66.dp).onFocusChanged { f -> if (f.isFocused) had = true else if (had) commit() },
                height = 36.dp, fontSize = 14, center = true, focusRequester = focus, onDone = ::commit, padding = 4.dp, radius = 8.dp, borderWidth = 1.dp,
            )
        } else Box(
            Modifier.width(66.dp).height(36.dp).clip(RoundedCornerShape(8.dp)).background(c.ground).border(1.dp, c.mutedBorder, RoundedCornerShape(8.dp))
                .then(if (onType != null) Modifier.clickable(role = Role.Button, onClickLabel = "Type a number") { typing = true } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, color = color, style = cutStyle(14, 800, 1f, tabular = true))
        }
        StepBtn("+", "More", onPlus)
        if (unit != null) Text(unit, color = c.text2, style = cutStyle(11, 700, 1f))
    }
}

@Composable
private fun StepBtn(label: String, description: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    Box(
        Modifier.size(44.dp).padding(4.dp).clip(RoundedCornerShape(8.dp)).background(c.hairline).repeatingPress(description, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.text, style = cutStyle(18, 700, 1f))
    }
}

/** A labelled field for sheets: 10sp label over a 48dp field, unit after it. */
@Composable
fun SheetField(
    label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    unit: String? = null, text: Boolean = false, placeholder: String? = null, big: Boolean = false, last: Boolean = false,
) {
    val c = CutTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Field(
                value, onChange, Modifier.weight(1f), height = 48.dp, fontSize = if (big) 19 else 16, weight = if (big) 800 else 700,
                keyboard = if (text) KeyboardType.Text else KeyboardType.Decimal, imeAction = if (last) ImeAction.Done else ImeAction.Next,
                borderWidth = 1.dp, radius = 12.dp, padding = 14.dp, placeholder = placeholder,
            )
            if (unit != null) Text(unit, color = c.text2, style = cutStyle(12, 700, 1f))
        }
    }
}

@Composable
fun SheetLabel(text: String) = Text(text.uppercase(), color = CutTheme.colors.text2, style = cutStyle(10, 800, 1f, 0.1f))

/** Done (confirms green) and an optional secondary action. */
@Composable
fun SheetButtons(primary: String, onPrimary: () -> Unit, secondary: String? = null, onSecondary: (() -> Unit)? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigButton(primary, onClick = onPrimary, modifier = Modifier.weight(1f), height = 48.dp, fontSize = 13, confirm = true, enabled = enabled)
        if (secondary != null && onSecondary != null) BigButton(secondary, onClick = onSecondary, modifier = Modifier.width(132.dp), secondary = true, height = 48.dp, fontSize = 12)
    }
}

/** The bottom sheet every editor lives in. [content] scrolls when tall; the keyboard pushes it up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = CutTheme.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = c.surface,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.mutedBorder)) },
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp, 6.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = c.text, style = cutStyle(20, 800, 1f, -0.01f, display = true))
            content()
        }
    }
}

/** Hold-free hairline rows for a card without numbers: label left, control right. */
@Composable
fun PlainRow(title: String, sub: String? = null, last: Boolean = false, control: @Composable RowScope.() -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Row(Modifier.fillMaxWidth().padding(14.dp, 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = c.text, style = cutStyle(15, 700, 1.1f))
                if (!sub.isNullOrBlank()) Text(sub, color = c.text2, style = cutStyle(11, 500, 1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            control()
        }
    }
}

/** 6dp space between stacked cards. */
val cardGap: Dp = 8.dp
