package com.relentnet.cuttracker.ui.beta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Chip
import com.relentnet.cuttracker.ui.components.ChipRow
import com.relentnet.cuttracker.ui.components.Fab
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.NumRow
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** One library row, whatever the library holds. */
data class LibRow(val key: String, val kind: String, val num: String, val unit: String?, val title: String, val sub: String)

data class LibKind(val key: String, val label: String)

/**
 * A library grouped by kind: search, a chip per kind with a sliding pill, a way to add a kind, one card per kind with
 * number-column rows, and a FAB. The page that owns the data renders its own sheets on top.
 */
@Composable
fun LibraryScreen(
    title: String, sub: String, onBack: () -> Unit,
    kinds: List<LibKind>, rows: List<LibRow>, searchHint: String,
    onRow: (LibRow) -> Unit, onAdd: () -> Unit, onEditKind: (LibKind) -> Unit, onNewKind: (String) -> Unit,
    empty: String,
) {
    val c = CutTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    var folded by rememberSaveable { mutableStateOf(setOf<String>()) }
    var kindForm by rememberSaveable { mutableStateOf(false) }
    var newKind by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = rows.filter { (filter.isEmpty() || it.kind == filter) && (q.isEmpty() || it.title.lowercase().contains(q)) }
    val other = LibKind("", "Other")
    val cards = (kinds + other).filter { k -> filter.isEmpty() || k.key == filter }.map { k -> k to shown.filter { it.kind == k.key } }
        .filter { (k, r) -> r.isNotEmpty() || (q.isEmpty() && k.key.isNotEmpty() && (filter.isEmpty() || filter == k.key)) }

    Box(Modifier.fillMaxSize().background(c.ground)) {
        LazyColumn(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item("head") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackHeader("Beta", title, onBack)
                    Sub(sub, size = 12)
                    Field(query, { query = it }, Modifier.fillMaxWidth().padding(top = 4.dp), height = 44.dp, fontSize = 15, weight = 600, keyboard = KeyboardType.Text, imeAction = ImeAction.Search, borderWidth = 1.dp, radius = 10.dp, padding = 12.dp, placeholder = searchHint)
                    ChipRow(listOf("" to "All") + kinds.map { it.key to it.label }, filter, onSelect = { filter = it }) {
                        Chip(if (kindForm) "×" else "+ Kind", on = false, dashed = !kindForm, onClick = { kindForm = !kindForm; if (!kindForm) newKind = "" })
                    }
                    AnimatedVisibility(kindForm, enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 420f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        val focus = FocusRequester()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBtn(CutIcons.Close, accent = false) { kindForm = false; newKind = "" }
                            Field(
                                newKind, { newKind = it }, Modifier.weight(1f), height = 40.dp, fontSize = 14, weight = 700, keyboard = KeyboardType.Text, imeAction = ImeAction.Done,
                                onDone = { if (newKind.isNotBlank()) { onNewKind(newKind.trim()); newKind = ""; kindForm = false } },
                                borderWidth = 1.dp, border = c.accent, radius = 10.dp, padding = 10.dp, placeholder = "New kind, like Pre-climb", focusRequester = focus,
                            )
                            IconBtn(CutIcons.Check, accent = true) { if (newKind.isNotBlank()) { onNewKind(newKind.trim()); newKind = ""; kindForm = false } }
                        }
                    }
                }
            }
            if (cards.isEmpty()) item("empty") { Sub(if (q.isNotEmpty()) "Nothing matches \"$query\"." else empty, size = 12) }
            for ((k, r) in cards) item(k.key.ifEmpty { "__other" }) {
                KindCard(
                    k.label, r.size.toString(), Modifier.animateItem(),
                    folded = k.key in folded, onFold = { folded = if (k.key in folded) folded - k.key else folded + k.key },
                    onEdit = if (k.key.isNotEmpty()) ({ onEditKind(k) }) else null,
                ) {
                    if (r.isEmpty()) Sub("Nothing here yet. Tap + and pick ${k.label} as its kind.", Modifier.padding(14.dp, 4.dp, 14.dp, 14.dp), size = 12)
                    for (row in r) NumRow(row.num, row.title, unit = row.unit, sub = row.sub, onClick = { onRow(row) })
                }
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd), onClick = onAdd)
    }
}

@Composable
private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Boolean, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(40.dp).clip(shape).background(if (accent) c.accent else c.ground).border(1.dp, if (accent) c.accent else c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(16.dp), tint = if (accent) c.onAccent else c.text2) }
}

/** Kind pills inside a sheet: plain chips, the chosen one orange. */
@Composable
fun KindPicker(kinds: List<LibKind>, selected: String, onSelect: (String) -> Unit) {
    ChipRow(kinds.map { it.key to it.label }, selected, onSelect)
}

@Composable
fun SheetNote(text: String) = Text(text, color = CutTheme.colors.text2, style = cutStyle(12, 500, 1.4f))
