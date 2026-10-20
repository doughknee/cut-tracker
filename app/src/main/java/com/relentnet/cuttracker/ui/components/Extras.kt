package com.relentnet.cuttracker.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * A number as people type it: "181.4", "181,4", "2,250" or " 165 g". Thousands commas are dropped, a lone comma is a
 * decimal point, anything else that is not a digit or a point is ignored. Null when nothing numeric is left.
 */
fun String.num(): Double? {
    val t = trim()
    val cleaned = if (Regex("""^\d{1,3}(,\d{3})+(\.\d+)?$""").matches(t)) t.replace(",", "") else t.replace(',', '.')
    return cleaned.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
}

fun String.int(): Int? = num()?.roundToInt()

/**
 * Click that repeats while held: once on press, again after 400ms, then faster and faster. For steppers, so a goal
 * 10 lb away is one hold rather than 20 taps.
 */
fun Modifier.repeatingPress(label: String, onClick: () -> Unit): Modifier = composed {
    val cb by rememberUpdatedState(onClick)
    pointerInput(Unit) {
        coroutineScope {
            detectTapGestures(onPress = {
                cb()
                val job = launch {
                    delay(400)
                    var gap = 120L
                    while (isActive) { cb(); delay(gap); gap = (gap * 0.85).toLong().coerceAtLeast(40) }
                }
                tryAwaitRelease()
                job.cancel()
            })
        }
    }.semantics { role = Role.Button; contentDescription = label; onClick { cb(); true } }
}

/** A photo decoded off the main thread at 1/[sample] size; null until it is ready or if the file is gone. */
@Composable
fun rememberPhoto(path: String, sample: Int): ImageBitmap? = produceState<ImageBitmap?>(null, path, sample) {
    value = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap() }.getOrNull()
    }
}.value

/** A plain dialog in the app's own look for the few actions that cannot be undone. [extra] is an optional middle choice. */
@Composable
fun ConfirmDialog(
    title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    extra: Pair<String, () -> Unit>? = null,
) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, color = c.text, style = cutStyle(21, 800, 1.15f, -0.01f, display = true))
            Sub(body)
            Spacer(Modifier.height(6.dp))
            BigButton(confirm, onClick = { onDismiss(); onConfirm() }, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13)
            if (extra != null) BigButton(extra.first, onClick = { onDismiss(); extra.second() }, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13, secondary = true)
            BigButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13, secondary = true)
        }
    }
}
