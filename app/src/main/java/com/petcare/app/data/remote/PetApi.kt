package com.petcare.app.data.remote

import com.petcare.app.model.CatalogBreed
import com.petcare.app.model.PetCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** ที่อยู่ของ PetCare Catalog API (ไฟล์ hosting/api/pet-catalog.json บน Firebase Hosting) */
object PetApi {
    const val CATALOG_URL = "https://petcare-5x7gf-a3068.web.app/api/pet-catalog.json"
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

/** แปลง JSON ของ Catalog API เป็น model ของแอป */
object PetApiParser {

    fun parseCatalog(json: String): List<PetCategory> {
        val categories = JSONObject(json).getJSONArray("categories")
        return (0 until categories.length()).map { i ->
            val c = categories.getJSONObject(i)
            val breeds = c.optJSONArray("breeds")?.let { arr ->
                (0 until arr.length()).map { j ->
                    val b = arr.getJSONObject(j)
                    CatalogBreed(name = b.getString("name"), nameEn = b.optString("nameEn"))
                }
            }.orEmpty()
            PetCategory(
                id = c.getString("id"),
                name = c.getString("name"),
                emoji = c.optString("emoji", "🐾"),
                breeds = breeds,
            )
        }
    }
}
