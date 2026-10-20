package com.relentnet.cuttracker.data

/**
 * Reads protein, calories and the serving size out of the lines a nutrition label scan recognised. Pure, so it is
 * tested on strings. OCR hands back lines in column order as often as row order, so a word with no number on its own
 * line takes the next line when that line starts with the number.
 */
object LabelRead {
    data class Facts(val protein: Int?, val calories: Int?, val serving: String?, val lines: List<String>) {
        val any get() = protein != null || calories != null
    }

    private val num = Regex("""(\d+(?:[.,]\d+)?)""")
    private val grams = Regex("""(\d+(?:[.,]\d+)?)\s*g\b""", RegexOption.IGNORE_CASE)
    private val kcal = Regex("""(\d+(?:[.,]\d+)?)\s*kcal""", RegexOption.IGNORE_CASE)
    private val servingSize = Regex("""serving\s*size\s*[:\-]?\s*(.*)""", RegexOption.IGNORE_CASE)
    private val per = Regex("""\bper\s+(\d+\s*(?:g|ml)\b|serving\b)""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Facts {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val used = LinkedHashSet<String>()

        // The number after [word]: on its line, else at the start of the next one.
        fun after(word: Regex, pattern: Regex): Double? {
            val atStart = Regex("^" + pattern.pattern, RegexOption.IGNORE_CASE)
            for ((i, l) in lines.withIndex()) {
                val w = word.find(l) ?: continue
                val hit = pattern.find(l.substring(w.range.last + 1))?.let { it to l }
                    ?: lines.getOrNull(i + 1)?.let { n -> atStart.find(n)?.let { it to n } } ?: continue
                used += hit.second
                return hit.first.groupValues[1].replace(',', '.').toDoubleOrNull() ?: continue
            }
            return null
        }

        val protein = after(Regex("""protein""", RegexOption.IGNORE_CASE), grams)
        // Energy lines give kJ first and kcal after; "Calories 230" is just the number. kcal anywhere wins over the bare number.
        val calories = lines.firstNotNullOfOrNull { l -> kcal.find(l)?.let { used += l; it.groupValues[1] } }?.replace(',', '.')?.toDoubleOrNull()
            ?: after(Regex("""calories""", RegexOption.IGNORE_CASE), num)
        val serving = lines.withIndex().firstNotNullOfOrNull { (i, l) ->
            servingSize.find(l)?.let { m -> m.groupValues[1].trim().ifEmpty { lines.getOrNull(i + 1) }?.also { used += if (it == m.groupValues[1].trim()) l else it } }
        } ?: lines.firstNotNullOfOrNull { l -> per.find(l)?.let { used += l; it.groupValues[1] } }
        return Facts(protein?.let { Math.round(it).toInt() }, calories?.let { Math.round(it).toInt() }, serving?.trim()?.trimEnd('.', ','), used.toList())
    }
}
