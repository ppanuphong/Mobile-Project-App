package com.petcare.app.data

import com.petcare.app.data.remote.PetApi
import com.petcare.app.data.remote.PetApiClient
import com.petcare.app.data.remote.PetApiParser
import com.petcare.app.model.PetCategory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * หมวดหมู่และสายพันธุ์จาก PetCare Catalog API (Firebase Hosting)
 * โหลดครั้งเดียวแล้วเก็บไว้ในหน่วยความจำ
 */
class PetCatalogRepository(private val client: PetApiClient) {

    private val mutex = Mutex()
    private var categories: List<PetCategory>? = null

    suspend fun getCategories(): List<PetCategory> = mutex.withLock {
        categories ?: PetApiParser.parseCatalog(client.get(PetApi.CATALOG_URL)).also { categories = it }
    }

    suspend fun category(id: String): PetCategory? = getCategories().firstOrNull { it.id == id }
}
