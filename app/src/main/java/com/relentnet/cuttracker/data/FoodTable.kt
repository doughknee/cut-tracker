package com.relentnet.cuttracker.data

import android.content.Context
import com.relentnet.cuttracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.GZIPInputStream

/** One food from the bundled USDA table: nutrition per 100 g, and the portions FDC lists for it, when any. */
data class Food(val name: String, val protein100: Double, val kcal100: Double, val portions: List<Portion>) {
    val key: String = name.lowercase()
    /** Grams a tapped food starts at: its first listed portion, else 100 g. */
    val defaultGrams: Double get() = portions.firstOrNull()?.grams ?: 100.0
}

data class Portion(val label: String, val grams: Double)

/**
 * The offline ingredient table: USDA FoodData Central (Foundation + SR Legacy, public domain), bundled as
 * res/raw/fdc_tsv.gz by tools/build_food_table.py. Read once, on first use, off the main thread.
 */
object FoodTable {
    @Volatile private var cache: List<Food>? = null
    private val lock = Mutex()

    suspend fun load(context: Context): List<Food> = cache ?: lock.withLock {
        cache ?: withContext(Dispatchers.IO) { parse(context.resources.openRawResource(R.raw.fdc_tsv)) }.also { cache = it }
    }

    /** The whole table from a gzip stream; pure, so the test can read the bundled file straight from disk. */
    fun parse(gz: InputStream): List<Food> = GZIPInputStream(gz).bufferedReader().useLines { lines -> lines.mapNotNull(::parseLine).toList() }

    fun parseLine(line: String): Food? {
        val f = line.split('\t')
        if (f.size < 3 || f[0].isBlank()) return null
        val portions = f.getOrNull(3).orEmpty().split('|').filter { '=' in it }.mapNotNull { p ->
            val i = p.lastIndexOf('=')
            p.substring(i + 1).toDoubleOrNull()?.let { Portion(p.substring(0, i), it) }
        }
        return Food(f[0], f[1].toDoubleOrNull() ?: 0.0, f[2].toDoubleOrNull() ?: 0.0, portions)
    }

    /** Foods whose name holds every word of [query]; names that start with the first word come first, then shorter names. */
    fun search(foods: List<Food>, query: String, limit: Int = 10): List<Food> {
        val words = query.lowercase().split(' ', ',').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return foods.asSequence().filter { f -> words.all { it in f.key } }
            .sortedWith(compareBy({ !it.key.startsWith(words[0]) }, { it.name.length }))
            .take(limit).toList()
    }
}
