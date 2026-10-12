package com.petcare.app.data

import com.petcare.app.model.Breed
import com.petcare.app.model.BreedGroup
import com.petcare.app.model.buildBreeds
import com.petcare.app.model.dogCeoNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** ผลการโหลดรายชื่อพันธุ์ — fromApi = false แปลว่าใช้รายการในเครื่องแทน (ออฟไลน์ / API ล่ม / ชนิดที่ไม่มี API) */
data class BreedResult(val breeds: List<Breed>, val fromApi: Boolean)

/**
 * ดึงรายชื่อสายพันธุ์จาก API ฟรีที่ไม่ต้องใช้คีย์
 * - สุนัข: dog.ceo
 * - แมว: catfact.ninja
 * ชนิดอื่นใช้รายการในเครื่อง และ cache ผลไว้ในหน่วยความจำตลอดอายุแอป
 */
class BreedRepository {

    private val cache = mutableMapOf<BreedGroup, BreedResult>()
    private val mutex = Mutex()

    suspend fun breedsFor(species: String): BreedResult {
        val group = BreedGroup.fromSpecies(species) ?: return BreedResult(emptyList(), fromApi = false)
        mutex.withLock { cache[group] }?.let { return it }

        val result = try {
            when (group) {
                BreedGroup.DOG -> BreedResult(buildBreeds(fetchDogNames(), group.popular), fromApi = true)
                BreedGroup.CAT -> BreedResult(buildBreeds(fetchCatNames(), group.popular), fromApi = true)
                else -> null
            }
        } catch (e: CancellationException) {
            throw e // ผู้ใช้เปลี่ยนชนิดสัตว์ระหว่างโหลด อย่าคืนรายการสำรองทับผลใหม่
        } catch (e: Exception) {
            null
        }

        if (result != null) mutex.withLock { cache[group] = result }
        // ไม่ cache ตอนล้มเหลว เพื่อให้ลองใหม่ได้เมื่อกลับมาออนไลน์
        return result ?: BreedResult(buildBreeds(emptyList(), group.popular), fromApi = false)
    }

    private suspend fun fetchDogNames(): List<String> {
        val message = JSONObject(get(DOG_CEO_URL)).getJSONObject("message")
        val map = message.keys().asSequence().associateWith { key ->
            val subs = message.getJSONArray(key)
            List(subs.length()) { subs.getString(it) }
        }
        return dogCeoNames(map)
    }

    private suspend fun fetchCatNames(): List<String> {
        val data = JSONObject(get(CAT_FACT_URL)).getJSONArray("data")
        return List(data.length()) { data.getJSONObject(it).getString("breed") }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val DOG_CEO_URL = "https://dog.ceo/api/breeds/list/all"
        const val CAT_FACT_URL = "https://catfact.ninja/breeds?limit=200"
        const val TIMEOUT_MS = 8_000
    }
}
