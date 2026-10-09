package com.petcare.app.data.remote

import com.petcare.app.model.Breed
import com.petcare.app.model.BreedSource
import com.petcare.app.model.PetCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** ที่อยู่ของ API ข้อมูลสัตว์เลี้ยง */
object PetApi {
    /** หมวดหมู่ทั้งหมด + สายพันธุ์ของหมวดอื่น ๆ (ไฟล์ hosting/api/pet-catalog.json บน Firebase Hosting) */
    const val CATALOG_URL = "https://petcare-5x7gf-a3068.web.app/api/pet-catalog.json"

    /** สายพันธุ์สุนัขทั้งหมด (ฟรี ไม่ต้องใช้ API key) */
    const val DOG_BREEDS_URL = "https://dog.ceo/api/breeds/list/all"

    /** สายพันธุ์แมวทั้งหมด (ฟรี ไม่ต้องใช้ API key) */
    const val CAT_BREEDS_URL = "https://catfact.ninja/breeds?limit=200"

    /** รูปสุ่มของสุนัขสายพันธุ์ที่เลือก */
    fun dogImageUrl(dogCeoPath: String) = "https://dog.ceo/api/breed/$dogCeoPath/images/random"
}

/** HTTP GET แบบง่าย คืนค่า body เป็น String */
class PetApiClient {
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code จาก $url")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

/** แปลง JSON จากแต่ละ API ให้เป็น model ของแอป */
object PetApiParser {

    fun parseCatalog(json: String): List<PetCategory> {
        val categories = JSONObject(json).getJSONArray("categories")
        return (0 until categories.length()).map { i ->
            val c = categories.getJSONObject(i)
            val source = when (c.optString("source")) {
                "dog.ceo" -> BreedSource.DOG_CEO
                "catfact.ninja" -> BreedSource.CATFACT
                else -> BreedSource.CATALOG
            }
            val breeds = c.optJSONArray("breeds")?.let { arr ->
                (0 until arr.length()).map { j ->
                    val b = arr.getJSONObject(j)
                    Breed(
                        id = "${c.getString("id")}/$j",
                        name = b.getString("name"),
                        subtitle = b.optString("nameEn"),
                        species = b.optString("species").ifBlank { null },
                    )
                }
            }.orEmpty()
            PetCategory(
                id = c.getString("id"),
                name = c.getString("name"),
                emoji = c.optString("emoji", "🐾"),
                source = source,
                breeds = breeds,
            )
        }
    }

    /** Dog CEO ส่งมาเป็น { "bulldog": ["boston", "french"], "beagle": [] } → แตกเป็นรายการเดียว */
    fun parseDogBreeds(json: String): List<Breed> {
        val message = JSONObject(json).getJSONObject("message")
        val result = mutableListOf<Breed>()
        message.keys().forEach { main ->
            val subs = message.getJSONArray(main)
            if (subs.length() == 0) {
                result += Breed(id = main, name = dogBreedName(main, null), dogCeoPath = main)
            } else {
                for (i in 0 until subs.length()) {
                    val sub = subs.getString(i)
                    result += Breed(
                        id = "$main/$sub",
                        name = dogBreedName(main, sub),
                        subtitle = "กลุ่ม ${prettyWord(main)}",
                        dogCeoPath = "$main/$sub",
                    )
                }
            }
        }
        return result.sortedBy { it.name }
    }

    fun parseCatBreeds(json: String): List<Breed> {
        val data = JSONObject(json).getJSONArray("data")
        return (0 until data.length()).map { i ->
            val b = data.getJSONObject(i)
            val name = b.getString("breed")
            val details = listOf(
                b.optString("country").takeIf { it.isNotBlank() }?.let { "ถิ่นกำเนิด $it" },
                coatThai(b.optString("coat")),
            ).filterNotNull().joinToString(" · ")
            Breed(id = name, name = name, subtitle = details)
        }.sortedBy { it.name }
    }

    fun parseDogImage(json: String): String? =
        JSONObject(json).optString("message").takeIf { it.startsWith("http") }

    // Dog CEO ใช้คำติดกันบางคำ แก้ให้อ่านง่าย
    private val WORD_FIXES = mapOf(
        "bullterrier" to "Bull Terrier",
        "cattledog" to "Cattle Dog",
        "cotondetulear" to "Coton de Tulear",
        "danishswedish" to "Danish-Swedish",
        "mexicanhairless" to "Mexican Hairless",
        "shihtzu" to "Shih Tzu",
        "stbernard" to "St. Bernard",
        "sharpei" to "Shar Pei",
        "kerryblue" to "Kerry Blue",
        "westhighland" to "West Highland",
        "germanlonghair" to "German Longhair",
        "flatcoated" to "Flat-Coated",
        "waterdog" to "Water Dog",
        "mix" to "Mixed Breed",
    )

    // กลุ่มที่ชื่อหลักเป็นคำขยาย เช่น german/shepherd → "German Shepherd" (ไม่ใช่ "Shepherd German")
    private val ADJECTIVE_MAINS = setOf("african", "australian", "finnish", "german", "danishswedish", "rough")

    internal fun dogBreedName(main: String, sub: String?): String {
        if (sub == null) return prettyWord(main)
        return if (main in ADJECTIVE_MAINS) "${prettyWord(main)} ${prettyWord(sub)}"
        else "${prettyWord(sub)} ${prettyWord(main)}"
    }

    private fun prettyWord(word: String) = WORD_FIXES[word] ?: word.replaceFirstChar { it.uppercase() }

    private fun coatThai(coat: String): String? = when (coat.lowercase()) {
        "" -> null
        "short" -> "ขนสั้น"
        "long" -> "ขนยาว"
        "medium" -> "ขนปานกลาง"
        "semi-long", "semi long" -> "ขนกึ่งยาว"
        "short/long", "long/short" -> "ขนสั้น/ยาว"
        "hairless", "partly hairless", "hairless/furry down" -> "ไม่มีขน"
        "rex", "rex (short/long)" -> "ขนหยิก"
        "all" -> "ขนได้หลายแบบ"
        else -> coat
    }
}
