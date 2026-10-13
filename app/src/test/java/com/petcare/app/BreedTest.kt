package com.petcare.app

import com.petcare.app.model.Breed
import com.petcare.app.model.BreedGroup
import com.petcare.app.model.SPECIES_OPTIONS
import com.petcare.app.model.buildBreeds
import com.petcare.app.model.cleanBreedName
import com.petcare.app.model.dogCeoNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreedTest {

    @Test
    fun dogCeoFlattening() {
        val names = dogCeoNames(
            mapOf(
                "australian" to listOf("kelpie", "shepherd"),
                "pug" to emptyList(),
                "shihtzu" to emptyList(),
                "bulldog" to listOf("french"),
                "german" to listOf("shepherd"),
            )
        )
        assertEquals(
            listOf("Australian Kelpie", "Australian Shepherd", "Pug", "Shih Tzu", "French Bulldog", "German Shepherd"),
            names,
        )
        val dogs = buildBreeds(names, BreedGroup.DOG.popular)
        assertEquals("เยอรมันเชพเพิร์ด", dogs.first { it.en == "German Shepherd" }.th)
    }

    @Test
    fun cleanCatNames() {
        assertEquals("Foldex", cleanBreedName("Foldex[4]"))
        assertEquals("PerFoldæ (Experimental Breed - WCF)", cleanBreedName("PerFoldæ(Experimental Breed - WCF)"))
        assertEquals("Persian (Modern Persian Cat)", cleanBreedName("  Persian  (Modern Persian Cat) "))
    }

    @Test
    fun thaiNamesFromPopularList() {
        val dogs = buildBreeds(dogCeoNames(mapOf("pug" to emptyList(), "shihtzu" to emptyList())), BreedGroup.DOG.popular)
        assertEquals("ปั๊ก", dogs.first { it.en == "Pug" }.th)
        assertEquals("ชิสุห์", dogs.first { it.en == "Shih Tzu" }.th)

        val cats = buildBreeds(listOf("Persian (Modern Persian Cat)", "Lykoi"), BreedGroup.CAT.popular)
        assertEquals("เปอร์เซีย", cats.first { it.en.startsWith("Persian") }.th)
        assertNull(cats.first { it.en == "Lykoi" }.th)
    }

    @Test
    fun popularBreedsMissingFromApiAreAdded() {
        val dogs = buildBreeds(listOf("Pug"), BreedGroup.DOG.popular)
        assertTrue(dogs.any { it.th == "ไทยหลังอาน" })
        assertTrue(dogs.any { it.th == "บางแก้ว" })
        // ไม่ซ้ำกับที่ API มี
        assertEquals(1, dogs.count { it.en == "Pug" })
    }

    @Test
    fun thaiNamedBreedsSortFirst() {
        val cats = buildBreeds(listOf("Abyssinian", "Lykoi", "Persian"), BreedGroup.CAT.popular)
        val firstEnglishOnly = cats.indexOfFirst { it.th == null }
        assertTrue(cats.drop(firstEnglishOnly).all { it.th == null })
    }

    @Test
    fun matchesThaiAndEnglish() {
        val pug = Breed("Pug", "ปั๊ก")
        assertEquals("ปั๊ก (Pug)", pug.label)
        assertTrue(pug.matches("pug"))
        assertTrue(pug.matches("ปั๊ก"))
        assertTrue(pug.matches(""))
        assertFalse(pug.matches("persian"))
    }

    @Test
    fun speciesToGroup() {
        assertEquals(BreedGroup.DOG, BreedGroup.fromSpecies("สุนัข"))
        assertEquals(BreedGroup.CAT, BreedGroup.fromSpecies("แมว"))
        assertEquals(BreedGroup.RODENT, BreedGroup.fromSpecies("หนูแฮมสเตอร์"))
        // ชนิดสัตว์ใน SPECIES_OPTIONS ครบทุกหมวดของหน้าสมัคร
        assertEquals(BreedGroup.RODENT, BreedGroup.fromSpecies("สัตว์ฟันแทะ"))
        assertEquals(BreedGroup.REPTILE, BreedGroup.fromSpecies("สัตว์เลื้อยคลาน"))
        assertEquals(BreedGroup.REPTILE, BreedGroup.fromSpecies("อิกัวน่า"))
        assertEquals(BreedGroup.EXOTIC, BreedGroup.fromSpecies("สัตว์เลี้ยงพิเศษ"))
        assertNull(BreedGroup.fromSpecies("แมงมุม"))
    }

    @Test
    fun everySpeciesOptionHasBreedGroup() {
        SPECIES_OPTIONS.forEach { assertNotNull("ไม่มีกลุ่มพันธุ์สำหรับ $it", BreedGroup.fromSpecies(it)) }
    }
}
