package com.relentnet.cuttracker.ui.beta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.relentnet.cuttracker.data.Food
import com.relentnet.cuttracker.data.FoodTable
import com.relentnet.cuttracker.data.Ingredient
import com.relentnet.cuttracker.data.LabelRead
import com.relentnet.cuttracker.data.OpenFoodFacts
import com.relentnet.cuttracker.data.RecipeImport
import com.relentnet.cuttracker.data.kcalSum
import com.relentnet.cuttracker.data.proteinSum
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.LibMeal
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel

/** Meals: everything you can put on a day, grouped by kind. A kind is a slot on the script (Breakfast, Lunch, ...). */
@Composable
fun MealsPage(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    var sheet by rememberSaveable { mutableStateOf("") }   // "", "new", "meal:<id>", "kind:<slot id>"
    // Something shared in from another app opens New meal with it; a second share while the sheet is up rebuilds the sheet.
    val shared = vm.pendingShare
    LaunchedEffect(shared) { if (shared != null) sheet = "new" }
    val kinds = s.meals.map { LibKind(it.id, it.name) }
    val ws = ProgramEngine.weekStart(st.today)
    val uses = remember(st.logs) {
        st.logs.filter { !it.date.isBefore(ws) }.flatMap { it.picks.values }.filter { it.startsWith("lib:") }.groupingBy { it.removePrefix("lib:") }.eachCount()
    }
    val rows = s.library.sortedByDescending { it.protein }.map { m ->
        val n = uses[m.id] ?: 0
        LibRow(m.id, if (s.meals.any { it.id == m.kind }) m.kind else "", m.protein.toString(), "g", m.name, "${m.calories} cal" + (if (n > 0) ", $n× this week" else ""))
    }
    LibraryScreen(
        "Meals", "${s.library.size} meals you can put on a day. The number is protein.", onBack,
        kinds, rows, searchHint = "Search meals",
        onRow = { sheet = "meal:" + it.key }, onAdd = { sheet = "new" }, onEditKind = { sheet = "kind:" + it.key },
        onNewKind = { name -> vm.saveSettings(addMealKind(s, name)) },
        empty = "No meals yet. Tap + to add one.",
    )
    when {
        sheet == "new" -> key(shared) { MealSheet(null, kinds, kinds.firstOrNull()?.key ?: "", onDismiss = { sheet = ""; vm.pendingShare = null },
            onSave = { m -> vm.saveSettings(s.copy(library = s.library + m)); sheet = ""; vm.pendingShare = null }, onDelete = null, shared = shared,
            importOn = s.recipeImportOn, onAllowImport = { vm.saveSettings(s.copy(onlineFeatures = true, recipeImport = true)) },
            barcodeOn = s.barcodeLookupOn, onAllowBarcode = { vm.saveSettings(s.copy(onlineFeatures = true, barcodeLookup = true)) }) }
        sheet.startsWith("meal:") -> s.library.firstOrNull { it.id == sheet.removePrefix("meal:") }?.let { m ->
            MealSheet(m, kinds, m.kind, onDismiss = { sheet = "" },
                onSave = { nm -> vm.saveSettings(s.copy(library = s.library.map { if (it.id == m.id) nm else it })); sheet = "" },
                onDelete = {
                    haptics.tap()
                    vm.saveSettings(s.copy(library = s.library.filter { it.id != m.id }, meals = s.meals.map { if (it.defaultLib == m.id) it.copy(defaultLib = null) else it }))
                    sheet = ""
                })
        } ?: run { sheet = "" }
        sheet.startsWith("kind:") -> s.meals.firstOrNull { it.id == sheet.removePrefix("kind:") }?.let { slot ->
            KindSheet(slot.name, "meals", onDismiss = { sheet = "" },
                onSave = { name -> vm.saveSettings(s.copy(meals = s.meals.map { if (it.id == slot.id) it.copy(name = name) else it })); sheet = "" },
                onRemove = {
                    haptics.tap()
                    vm.saveSettings(s.copy(
                        meals = s.meals.filter { it.id != slot.id },
                        library = s.library.map { if (it.kind == slot.id) it.copy(kind = "") else it },
                        plans = s.plans.mapValues { (_, p) -> p.copy(slots = p.slots - slot.id) },
                    ))
                    sheet = ""
                })
        } ?: run { sheet = "" }
    }
}

/** A new kind is a new slot on the script, on for every kind of day until a day page says otherwise. */
fun addMealKind(s: Settings, name: String): Settings {
    val id = "m" + System.currentTimeMillis()
    return s.copy(meals = s.meals + Meal(id, name, "", "", 0, 0, 10, true, null), plans = s.plans.mapValues { (_, p) -> p.copy(slots = p.slots + id) })
}

@Composable
private fun MealSheet(
    m: LibMeal?, kinds: List<LibKind>, kind0: String, onDismiss: () -> Unit, onSave: (LibMeal) -> Unit, onDelete: (() -> Unit)?,
    importOn: Boolean = false, onAllowImport: () -> Unit = {}, barcodeOn: Boolean = false, onAllowBarcode: () -> Unit = {},
    shared: String? = null,
) {
    val c = CutTheme.colors
    val context = LocalContext.current
    // A shared link goes in the link field; whatever text came with it (or plain text alone) is the name until the page says better.
    val sharedLink = shared?.let { Regex("""https?://\S+""").find(it)?.value }
    val sharedName = shared?.replace(sharedLink ?: "", "")?.trim()?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: ""
    var name by remember(m) { mutableStateOf(m?.name ?: sharedName) }
    var kind by remember(m) { mutableStateOf(kind0) }
    var pro by remember(m) { mutableStateOf(m?.protein?.toString() ?: "") }
    var cal by remember(m) { mutableStateOf(m?.calories?.toString() ?: "") }
    var recipe by remember(m) { mutableStateOf(m?.recipe ?: "") }
    var lines by remember(m) { mutableStateOf(m?.ingredients ?: emptyList()) }
    var link by remember(m) { mutableStateOf(sharedLink ?: "") }
    var fetching by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Pair<String, Boolean>?>(null) }   // text, ok
    var scanning by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var askBarcode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    // The lines own the numbers while there are any; the fields stay editable for a meal typed by hand.
    fun setLines(l: List<Ingredient>) { lines = l; if (l.isNotEmpty()) { pro = l.proteinSum().toString(); cal = l.kcalSum().toString() } }
    // The link is the consent: once online features are on, a usable link fetches half a second after the typing stops.
    // Lives in the effect so a changed link cancels the fetch in flight instead of racing it.
    LaunchedEffect(link, importOn) {
        if (!importOn || !RecipeImport.isUsable(link)) return@LaunchedEffect
        delay(500)
        fetching = true; note = "Reading the page" to true
        RecipeImport.fetch(link).onSuccess { r ->
            name = r.name; pro = r.protein?.toString() ?: ""; cal = r.calories?.toString() ?: ""
            recipe = link.trim() + (r.servings?.let { ", 1 of $it servings." } ?: "")
            val missing = listOfNotNull("protein".takeIf { r.protein == null }, "calories".takeIf { r.calories == null })
            note = (if (missing.isEmpty()) "Found it. Check the numbers before you add it." else "Found the name. That page lists no ${missing.joinToString(" or ")}, type it.") to true
        }.onFailure { note = (it.message ?: "Could not read that page.") to false }
        fetching = false
    }
    // A scanned code becomes one line of the meal: one serving when the label gives one, else 100 g. The grams stay editable.
    LaunchedEffect(code) {
        if (code.isBlank()) return@LaunchedEffect
        fetching = true; note = "Asking Open Food Facts" to true
        OpenFoodFacts.fetch(code).onSuccess { r ->
            if (name.isBlank()) name = r.name
            setLines(lines + Ingredient(r.name, r.servingGrams ?: 100.0, r.protein100 ?: 0.0, r.kcal100 ?: 0.0))
            val missing = listOfNotNull("protein".takeIf { r.protein100 == null }, "calories".takeIf { r.kcal100 == null })
            note = (if (missing.isEmpty()) (if (r.servingGrams != null) "Found it, one serving. Change the grams if you had more." else "Found it. The label has no serving size, so this is 100 g. Change the grams.")
                else "Found the name. Open Food Facts lists no ${missing.joinToString(" or ")} for it, type it.") to true
        }.onFailure { note = (it.message ?: "Could not look that up.") to false }
        fetching = false; code = ""
    }
    if (scanning) BarcodeScanner(onScan = { scanning = false; code = it }, onDismiss = { scanning = false })
    // A label fills the number fields straight from the camera, on the phone. The note says which lines it trusted.
    if (reading) LabelScanner(onDismiss = { reading = false }, onRead = { text ->
        reading = false
        val f = LabelRead.parse(text)
        f.protein?.let { pro = it.toString() }; f.calories?.let { cal = it.toString() }
        if (f.serving != null && recipe.isBlank()) recipe = "One serving, ${f.serving}, from the label."
        val missing = listOfNotNull("protein".takeIf { f.protein == null }, "calories".takeIf { f.calories == null })
        val next = if (missing.isNotEmpty()) "No ${missing.joinToString(" or ")} found, type it." else if (name.isBlank()) "Name it and add it." else "Check the numbers."
        note = ("Read " + f.lines.joinToString(", ") { "“$it”" } + ". " + next) to true
    })
    Sheet(if (m == null) "New meal" else "Edit meal", onDismiss) {
        if (m == null) {
            SheetField("Recipe link", link, { link = it; note = null }, text = true, placeholder = "Paste one, or share a page here")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Scan a barcode", onClick = { note = null; if (barcodeOn) scanning = true else askBarcode = true }, modifier = Modifier.weight(1f), secondary = true, height = 48.dp, fontSize = 12)
                BigButton("Read a label", onClick = { note = null; reading = true }, modifier = Modifier.weight(1f), secondary = true, height = 48.dp, fontSize = 12)
            }
            if (!importOn && RecipeImport.isUsable(link)) AllowCard(
                "This downloads the page you pasted, from that site, and reads the recipe out of it. Nothing else leaves the phone. You can switch it off under Settings, Data.",
                "Allow and read it", onAllow = onAllowImport, onNotNow = { link = "" },
            )
            if (askBarcode && !barcodeOn) AllowCard(
                "This sends the digits of the barcode you scan, and nothing else, to Open Food Facts, a public food database, and reads the product's name and nutrition from the answer. You can switch it off under Settings, Data.",
                "Allow and scan", onAllow = { onAllowBarcode(); askBarcode = false; scanning = true }, onNotNow = { askBarcode = false },
            )
            note?.let { (text, ok) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fetching) CircularProgressIndicator(Modifier.size(12.dp), color = c.accent, strokeWidth = 2.dp)
                Text(text, color = if (!ok) c.accent else if (fetching) c.text2 else c.good, style = cutStyle(12, 600, 1.3f))
            } }
        }
        SheetField("Name", name, { name = it }, text = true, big = true, placeholder = "What is it")
        SheetLabel("Kind")
        KindPicker(kinds, kind) { kind = it }
        IngredientsSection(lines, ::setLines) { q -> withContext(Dispatchers.Default) { FoodTable.search(FoodTable.load(context), q) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetField("Protein", pro, { pro = it }, Modifier.weight(1f), unit = "g")
            SheetField("Calories", cal, { cal = it }, Modifier.weight(1f))
        }
        SheetField("Recipe or note", recipe, { recipe = it }, text = true, last = true, placeholder = "Optional")
        SheetButtons(
            if (m == null) "Add it" else "Done", enabled = name.isNotBlank(),
            onPrimary = { onSave(LibMeal(m?.id ?: "lib_${System.currentTimeMillis()}", name.trim(), pro.toIntOrNull() ?: 0, cal.toIntOrNull() ?: 0, recipe.trim(), kind, lines)) },
            secondary = if (m == null) "Cancel" else "Delete", onSecondary = onDelete ?: onDismiss,
        )
    }
}

/** The in-context consent for an online feature: what it sends, to whom, and a way to say no. */
@Composable
private fun AllowCard(what: String, allow: String, onAllow: () -> Unit, onNotNow: () -> Unit) {
    val c = CutTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.ground).border(1.dp, c.accent, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetNote(what)
        SheetButtons(allow, onPrimary = onAllow, secondary = "Not now", onSecondary = onNotNow)
    }
}

/**
 * The lines a meal is built from, and a search over the bundled USDA table to add one. The table loads the first time
 * something is typed; [search] runs off the main thread.
 */
@Composable
private fun IngredientsSection(lines: List<Ingredient>, onLines: (List<Ingredient>) -> Unit, search: suspend (String) -> List<Food>) {
    val c = CutTheme.colors
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Food>>(emptyList()) }
    LaunchedEffect(q) { hits = if (q.isBlank()) emptyList() else search(q) }
    // With no lines yet the search field's own label is the section label; two eyebrows in a row read as a mistake.
    if (lines.isNotEmpty()) SheetLabel("Ingredients")
    lines.forEachIndexed { i, ing ->
        key(i, ing.name) {
            var g by remember { mutableStateOf(fmtGrams(ing.grams)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ing.name, color = c.text, style = cutStyle(13, 700, 1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${fmtGrams(ing.protein)} g protein, ${Math.round(ing.kcal)} cal", color = c.text2, style = cutStyle(11, 600, 1.2f))
                }
                Field(g, { g = it; onLines(lines.toMutableList().also { l -> l[i] = ing.copy(grams = it.replace(',', '.').toDoubleOrNull() ?: 0.0) }) },
                    Modifier.width(64.dp), height = 40.dp, fontSize = 14, weight = 700, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 10.dp, padding = 8.dp)
                Text("g", color = c.text2, style = cutStyle(12, 700, 1f))
                Icon(CutIcons.Close, "Remove", Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onLines(lines.filterIndexed { j, _ -> j != i }) }.padding(5.dp), tint = c.text2)
            }
        }
    }
    SheetField(if (lines.isEmpty()) "Ingredients" else "Add another", q, { q = it }, text = true, placeholder = "Search foods, like chicken breast")
    hits.forEach { f ->
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.ground).clickable(role = Role.Button) { onLines(lines + Ingredient(f.name, f.defaultGrams, f.protein100, f.kcal100)); q = "" }.padding(10.dp, 8.dp),
        ) {
            Text(f.name, color = c.text, style = cutStyle(13, 700, 1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${fmtGrams(f.protein100)} g protein, ${Math.round(f.kcal100)} cal per 100 g" + (f.portions.firstOrNull()?.let { ", ${it.label} is ${fmtGrams(it.grams)} g" } ?: ""), color = c.text2, style = cutStyle(11, 600, 1.2f))
        }
    }
    if (q.isNotBlank() && hits.isEmpty()) SheetNote("Nothing in the USDA table matches. Try fewer words, or type the numbers.")
}

private fun fmtGrams(g: Double): String = if (g == Math.rint(g)) g.toLong().toString() else "%.1f".format(g)

/** Rename or remove a kind. Removing keeps what was in it; those rows move to Other. */
@Composable
fun KindSheet(name0: String, noun: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var name by remember(name0) { mutableStateOf(name0) }
    Sheet("Edit kind", onDismiss) {
        SheetField("Kind", name, { name = it }, text = true, big = true, last = true)
        SheetNote("Removing a kind keeps its $noun. They move to Other.")
        SheetButtons("Done", onPrimary = { onSave(name.trim().ifBlank { name0 }) }, secondary = "Remove kind", onSecondary = onRemove, enabled = name.isNotBlank())
    }
}
