package com.relentnet.cuttracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL

/** What Open Food Facts knows about a product: nutrition per 100 g, and the serving in grams when the label gives one. */
data class ProductInfo(val name: String, val protein100: Double?, val kcal100: Double?, val servingGrams: Double?)

/**
 * The barcode lookup: GET one product from Open Food Facts by its digits, nothing else. HTTPS only, 1 MB cap, no cookies.
 * The parser is pure so the test can feed it saved answers.
 */
object OpenFoodFacts {
    private const val MAX_BYTES = 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isBarcode(s: String): Boolean = s.length in 8..14 && s.all { it.isDigit() }

    suspend fun fetch(barcode: String): Result<ProductInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(isBarcode(barcode)) { "That is not a product barcode." }
            val conn = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,nutriments,serving_size,serving_quantity").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "application/json")
            try {
                if (conn.responseCode == 404) error("No product with that barcode in Open Food Facts. Type the numbers.")
                if (conn.responseCode !in 200..299) error("Open Food Facts answered ${conn.responseCode}.")
                val body = String(conn.inputStream.use { it.readNBytes(MAX_BYTES) }, Charsets.UTF_8)
                parse(body) ?: error("No product with that barcode in Open Food Facts. Type the numbers.")
            } finally { conn.disconnect() }
        }.recoverCatching { e ->
            // On phones that gate the network per app (GrapheneOS), a denied app sees every host as unknown.
            if (e is java.net.UnknownHostException) throw IllegalStateException("Could not reach Open Food Facts. Check the connection, and that Sendline is allowed to use the network in the phone's app settings.")
            throw e
        }
    }

    private val gramsRe = Regex("""(\d+(?:[.,]\d+)?)\s*g\b""", RegexOption.IGNORE_CASE)

    /** The product in an api/v2 answer, or null when the answer says there is none. */
    fun parse(body: String): ProductInfo? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val p = root["product"] as? JsonObject ?: return null
        val n = p["nutriments"] as? JsonObject
        fun num(o: JsonObject?, k: String) = (o?.get(k) as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.replace(',', '.').toDoubleOrNull() }
        val name = (p["product_name"] as? JsonPrimitive)?.content?.trim().orEmpty().ifEmpty { "Scanned product" }
        val kcal = num(n, "energy-kcal_100g") ?: num(n, "energy_100g")?.let { it / 4.184 }
        val serving = num(p, "serving_quantity") ?: (p["serving_size"] as? JsonPrimitive)?.content?.let { gramsRe.find(it)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() }
        return ProductInfo(name, num(n, "proteins_100g"), kcal, serving?.takeIf { it > 0 })
    }
}
