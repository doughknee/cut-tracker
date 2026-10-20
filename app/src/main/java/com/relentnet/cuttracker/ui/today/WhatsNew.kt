package com.relentnet.cuttracker.ui.today

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/**
 * What changed, in a few lines, the first time an updated app opens. Only versions listed here get a note; the full
 * list lives in CHANGELOG.md and on the site.
 */
val whatsNew: Map<String, List<String>> = mapOf(
    "1.3.0" to listOf(
        "Past days keep the result they were logged with. Changing your plan or protein never rewrites history.",
        "Back closes what is open first, and every delete has Undo for six seconds.",
        "Hold a stepper to repeat, or tap the number to type it.",
        "A done weigh-in opens to fix a typo, and lift numbers stay put when you close the panel.",
        "Each reminder has its own switch, and the pick-a-day one answers from the notification.",
        "Back up every week to a folder you pick (Settings, Data), and restore it on a new phone from the first screen.",
    ),
)

@Composable
fun WhatsNewSheet(version: String, lines: List<String>, onDone: () -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    Sheet("New in $version", onDone) {
        for (line in lines) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 6.dp).size(7.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
            Text(line, color = c.text, style = cutStyle(14, 500, 1.4f))
        }
        SheetButtons(
            "Got it", onPrimary = onDone,
            secondary = "Everything", onSecondary = {
                onDone()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://doughknee.github.io/cut-tracker/#new")))
            },
        )
    }
}
