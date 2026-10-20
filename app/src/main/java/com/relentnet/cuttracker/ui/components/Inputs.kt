package com.relentnet.cuttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** Bordered numeric or text field in the prototype's style. Next moves to the next field; Done fires [onDone] and closes the keyboard. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    fontSize: Int = 20,
    weight: Int = 800,
    keyboard: KeyboardType = KeyboardType.Decimal,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
    placeholder: String? = null,
    center: Boolean = false,
    border: Color = CutTheme.colors.mutedBorder,
    borderWidth: Dp = 2.dp,
    background: Color = CutTheme.colors.ground,
    focusRequester: FocusRequester? = null,
    radius: Dp = 9.dp,
    padding: Dp = 12.dp,
    singleLine: Boolean = true,
) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(radius)
    var focused by remember { mutableStateOf(false) }
    // A focused field scrolls itself above the keyboard once the keyboard has opened, wherever it sits on the page.
    val bring = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    androidx.compose.runtime.LaunchedEffect(focused, imeBottom) { if (focused && imeBottom > 0) bring.bringIntoView() }
    val borderColor by animateColorAsState(if (focused) c.accent else border, tween(140), label = "fieldBorder")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background, shape)
            .border(borderWidth, borderColor, shape)
            .bringIntoViewRequester(bring)
            .onFocusChanged { focused = it.isFocused }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        textStyle = cutStyle(fontSize, weight, 1f, tabular = true).copy(color = c.text, textAlign = if (center) TextAlign.Center else TextAlign.Start),
        singleLine = singleLine,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone(); defaultKeyboardAction(ImeAction.Done) }),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(horizontal = padding, vertical = if (singleLine) 0.dp else 10.dp), contentAlignment = if (center) Alignment.Center else if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = c.dim, style = cutStyle(fontSize, weight, 1f, tabular = true), textAlign = if (center) TextAlign.Center else TextAlign.Start)
                }
                inner()
            }
        },
    )
}

/** Toggle switch, 46x26 drawn in a 48dp target, accent when on. TalkBack reads [label] and on or off. */
@Composable
fun Switch(on: Boolean, label: String? = null, onToggle: () -> Unit) {
    val c = CutTheme.colors
    val track by animateColorAsState(if (on) c.accent else c.mutedBorder, tween(160), label = "switchTrack")
    val bias by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else -1f, tween(160), label = "switchThumb")
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.height(26.dp).width(46.dp).clip(RoundedCornerShape(13.dp)).background(track).padding(3.dp),
            contentAlignment = BiasAlignment(bias, 0f),
        ) {
            Box(Modifier.height(20.dp).width(20.dp).clip(RoundedCornerShape(10.dp)).background(if (c.isDark) c.text else c.surface))
        }
    }
}
