package com.relentnet.cuttracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/** What a recipe page says about itself: schema.org Recipe from its JSON-LD. Numbers are per serving where the page gives them. */
data class RecipeInfo(val name: String, val protein: Int?, val calories: Int?, val servings: Int?)

/**
 * The app's one network call: GET the page the user pasted, read its JSON-LD Recipe block, nothing else. HTTPS only,
 * 2 MB cap, no cookies, no redirects to another host. The parser is pure so the test can feed it saved pages.
 */
object RecipeImport {
    private const val MAX_BYTES = 2 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isUsable(url: String): Boolean = url.trim().startsWith("https://", ignoreCase = true) && url.trim().length > 12 && ' ' !in url.trim()

    suspend fun fetch(url: String): Result<RecipeInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(isUsable(url)) { "Only https links." }
            val conn = URL(url.trim()).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000
            conn.instanceFollowRedirects = true
            // A browser-shaped agent: several recipe sites answer 403 to anything else, and the page is public either way.
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            try {
                if (conn.responseCode !in 200..299) error("The page answered ${conn.responseCode}.")
                val bytes = conn.inputStream.use { it.readNBytes(MAX_BYTES) }
                val html = String(bytes, Charsets.UTF_8)
                val (info, why) = parseDetailed(html)
                info ?: error(if (why.isBlank() && bytes.size < 8 * 1024) "That site sent a stub instead of the page, which usually means it blocks apps. Open it in a browser and type the numbers." else "No recipe data on that page. Type the numbers.")
            } finally { conn.disconnect() }
        }.recoverCatching { e ->
            // On phones that gate the network per app (GrapheneOS), a denied app sees every host as unknown.
            if (e is java.net.UnknownHostException) throw IllegalStateException("Could not reach that site. Check the connection, and that Sendline is allowed to use the network in the phone's app settings.")
            throw e
        }
    }

    private val scriptRe = Regex("""<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** The first schema.org Recipe in the page's JSON-LD blocks, or null. */
    fun parse(html: String): RecipeInfo? = parseDetailed(html).first

    /** The recipe, or why there is none: the blocks seen and what went wrong with each. */
    fun parseDetailed(html: String): Pair<RecipeInfo?, String> {
        val why = StringBuilder()
        for (m in scriptRe.findAll(html)) {
            val raw = m.groupValues[1].trim()
            val el = runCatching { json.parseToJsonElement(raw) }.getOrElse { why.append("block of ${raw.length} chars did not parse: ${it.message?.take(80)}; "); null } ?: continue
            val r = findRecipe(el)
            if (r == null) { why.append("block of ${raw.length} chars has no Recipe (${types(el).take(6).joinToString()}); "); continue }
            return toInfo(r) to ""
        }
        return null to why.toString()
    }

    private fun types(el: JsonElement): List<String> = when (el) {
        is JsonObject -> listOfNotNull((el["@type"] as? JsonPrimitive)?.content) + ((el["@graph"] as? JsonArray)?.flatMap { types(it) } ?: emptyList())
        is JsonArray -> el.flatMap { types(it) }
        else -> emptyList()
    }

    private fun findRecipe(el: JsonElement): JsonObject? = when (el) {
        is JsonObject -> {
            val type = el["@type"]
            val isRecipe = when (type) {
                is JsonPrimitive -> type.content.equals("Recipe", true)
                is JsonArray -> type.any { it is JsonPrimitive && it.content.equals("Recipe", true) }
                else -> false
            }
            if (isRecipe) el else (el["@graph"] as? JsonArray)?.firstNotNullOfOrNull { findRecipe(it) }
        }
        is JsonArray -> el.firstNotNullOfOrNull { findRecipe(it) }
        else -> null
    }

    private fun toInfo(r: JsonObject): RecipeInfo {
        val name = (r["name"] as? JsonPrimitive)?.content?.trim().orEmpty().ifEmpty { "Recipe" }
        val n = r["nutrition"] as? JsonObject
        val servings = r["recipeYield"]?.let { y ->
            val s = when (y) { is JsonPrimitive -> y.content; is JsonArray -> y.firstOrNull()?.let { (it as? JsonPrimitive)?.content }; else -> null }
            s?.let { num(it)?.toInt() }
        }
        return RecipeInfo(name, n?.get("proteinContent")?.let { num(it.jsonPrimitive.content)?.toInt() }, n?.get("calories")?.let { num(it.jsonPrimitive.content)?.toInt() }, servings)
    }

    private val numRe = Regex("""\d+(?:[.,]\d+)?""")
    private fun num(s: String): Double? = numRe.find(s)?.value?.replace(',', '.')?.toDoubleOrNull()
}
