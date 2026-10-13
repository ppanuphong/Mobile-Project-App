package com.petcare.app

import com.petcare.app.data.remote.PetApiParser
import com.petcare.app.model.Breed
import com.petcare.app.model.toBreed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetApiParserTest {

    @Test
    fun catalog_parsesCategoriesAndBreeds() {
        val json = """
            {"version":1,"categories":[
              {"id":"dog","name":"สุนัข","emoji":"🐶","source":"dog.ceo"},
              {"id":"rodent","name":"สัตว์ฟันแทะ","emoji":"🐹","breeds":[
                {"name":"หนูตะเภา / แกสบี้","nameEn":"Guinea Pig","species":"หนูตะเภา"},
                {"name":"ชินชิล่า"}
              ]}
            ]}
        """.trimIndent()
        val categories = PetApiParser.parseCatalog(json)

        assertEquals(listOf("dog", "rodent"), categories.map { it.id })
        assertTrue(categories[0].breeds.isEmpty()) // สุนัขดึงจาก dog.ceo แทน
        assertEquals("Guinea Pig", categories[1].breeds[0].nameEn)
    }

    @Test
    fun catalogBreed_toBreedLabel() {
        val rodent = PetApiParser.parseCatalog(
            """{"categories":[{"id":"rodent","name":"x","breeds":[{"name":"ชินชิล่า","nameEn":"Chinchilla"},{"name":"เดกู"}]}]}""",
        ).single()
        assertEquals("ชินชิล่า (Chinchilla)", rodent.breeds[0].toBreed().label)
        assertEquals(Breed("เดกู"), rodent.breeds[1].toBreed()) // ไม่มีชื่ออังกฤษ ใช้ชื่อไทยเป็นชื่อหลัก
    }
}
