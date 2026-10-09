package com.petcare.app

import com.petcare.app.data.remote.PetApiParser
import com.petcare.app.model.BreedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetApiParserTest {

    @Test
    fun catalog_mapsSourcesAndBreeds() {
        val json = """
            {"version":1,"categories":[
              {"id":"dog","name":"สุนัข","emoji":"🐶","source":"dog.ceo"},
              {"id":"cat","name":"แมว","emoji":"🐱","source":"catfact.ninja"},
              {"id":"rodent","name":"สัตว์ฟันแทะ","emoji":"🐹","breeds":[
                {"name":"หนูตะเภา","nameEn":"Guinea Pig","species":"หนูตะเภา"},
                {"name":"ชินชิล่า","nameEn":"Chinchilla"}
              ]}
            ]}
        """.trimIndent()
        val categories = PetApiParser.parseCatalog(json)

        assertEquals(listOf("dog", "cat", "rodent"), categories.map { it.id })
        assertEquals(BreedSource.DOG_CEO, categories[0].source)
        assertEquals(BreedSource.CATFACT, categories[1].source)
        assertEquals(BreedSource.CATALOG, categories[2].source)
        assertEquals("หนูตะเภา", categories[2].breeds[0].species)
        assertEquals("Guinea Pig", categories[2].breeds[0].subtitle)
        assertNull(categories[2].breeds[1].species) // ไม่ระบุ → ใช้ชื่อหมวดตอนบันทึก
    }

    @Test
    fun dogBreeds_flattenSubBreedsWithReadableNames() {
        val json = """
            {"message":{"beagle":[],"bulldog":["french"],"german":["shepherd"],"shihtzu":[]},"status":"success"}
        """.trimIndent()
        val breeds = PetApiParser.parseDogBreeds(json)

        assertEquals(listOf("Beagle", "French Bulldog", "German Shepherd", "Shih Tzu"), breeds.map { it.name })
        val french = breeds.first { it.name == "French Bulldog" }
        assertEquals("bulldog/french", french.dogCeoPath)
        assertEquals("กลุ่ม Bulldog", french.subtitle)
    }

    @Test
    fun catBreeds_includeCountryAndThaiCoat() {
        val json = """
            {"current_page":1,"data":[
              {"breed":"Siamese","country":"Thailand","origin":"Natural","coat":"Short","pattern":"Colorpoint"},
              {"breed":"Abyssinian","country":"Ethiopia","origin":"Natural","coat":"","pattern":"Ticked"}
            ]}
        """.trimIndent()
        val breeds = PetApiParser.parseCatBreeds(json)

        assertEquals(listOf("Abyssinian", "Siamese"), breeds.map { it.name })
        assertEquals("ถิ่นกำเนิด Thailand · ขนสั้น", breeds[1].subtitle)
        assertEquals("ถิ่นกำเนิด Ethiopia", breeds[0].subtitle)
    }

    @Test
    fun dogImage() {
        assertEquals(
            "https://images.dog.ceo/breeds/shiba/shiba-10.jpg",
            PetApiParser.parseDogImage("""{"message":"https://images.dog.ceo/breeds/shiba/shiba-10.jpg","status":"success"}"""),
        )
        assertTrue(PetApiParser.parseDogImage("""{"message":"Breed not found","status":"error"}""") == null)
    }
}
