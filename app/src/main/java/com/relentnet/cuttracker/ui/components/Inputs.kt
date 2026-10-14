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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** Bordered numeric or text field in the prototype's style. Enter fires [onDone]. */
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
) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(radius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background, shape)
            .border(borderWidth, border, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        textStyle = cutStyle(fontSize, weight, 1f, tabular = true).copy(color = c.text, textAlign = if (center) TextAlign.Center else TextAlign.Start),
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { onDone() }),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(horizontal = padding), contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = c.dim, style = cutStyle(fontSize, weight, 1f, tabular = true), textAlign = if (center) TextAlign.Center else TextAlign.Start)
                }
                inner()
            }
        },
    )
}

/** Toggle switch, 46x26, accent when on. */
@Composable
fun Switch(on: Boolean, onToggle: () -> Unit) {
    val c = CutTheme.colors
    Box(
        Modifier
            .height(26.dp)
            .width(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (on) c.accent else c.mutedBorder)
            .clickable(onClick = onToggle, role = Role.Switch)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.height(20.dp).width(20.dp).clip(RoundedCornerShape(10.dp)).background(Color.White))
    }
}
