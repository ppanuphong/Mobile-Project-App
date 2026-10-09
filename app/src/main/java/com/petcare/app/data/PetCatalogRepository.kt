package com.petcare.app.data

import com.petcare.app.data.remote.PetApi
import com.petcare.app.data.remote.PetApiClient
import com.petcare.app.data.remote.PetApiParser
import com.petcare.app.model.Breed
import com.petcare.app.model.BreedSource
import com.petcare.app.model.PetCategory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ข้อมูลหมวดหมู่และสายพันธุ์สัตว์เลี้ยงจาก API
 * - หมวดหมู่ทั้งหมด: PetCare Catalog API (Firebase Hosting)
 * - สุนัข: Dog CEO API, แมว: catfact.ninja
 * เก็บผลไว้ในหน่วยความจำ จะได้ไม่ต้องโหลดซ้ำทุกครั้งที่สลับหมวด
 */
class PetCatalogRepository(private val client: PetApiClient) {

    private val mutex = Mutex()
    private var categories: List<PetCategory>? = null
    private val breedsCache = mutableMapOf<String, List<Breed>>()

    suspend fun getCategories(): List<PetCategory> = mutex.withLock {
        categories ?: PetApiParser.parseCatalog(client.get(PetApi.CATALOG_URL)).also { categories = it }
    }

    suspend fun getBreeds(category: PetCategory): List<Breed> {
        mutex.withLock { breedsCache[category.id] }?.let { return it }
        val breeds = when (category.source) {
            BreedSource.DOG_CEO -> PetApiParser.parseDogBreeds(client.get(PetApi.DOG_BREEDS_URL))
            BreedSource.CATFACT -> PetApiParser.parseCatBreeds(client.get(PetApi.CAT_BREEDS_URL))
            BreedSource.CATALOG -> category.breeds
        }
        mutex.withLock { breedsCache[category.id] = breeds }
        return breeds
    }

    /** รูปตัวอย่างของสายพันธุ์ (มีเฉพาะสุนัข) */
    suspend fun getBreedImage(breed: Breed): String? {
        val path = breed.dogCeoPath ?: return null
        return PetApiParser.parseDogImage(client.get(PetApi.dogImageUrl(path)))
    }
}
