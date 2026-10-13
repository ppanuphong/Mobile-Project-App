package com.petcare.app

import com.petcare.app.model.BreedTrait
import com.petcare.app.model.CareTipEngine
import com.petcare.app.model.Pet
import com.petcare.app.model.ThaiSeason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CareTipEngineTest {
    private val april = LocalDate.of(2026, 4, 15)
    private val august = LocalDate.of(2026, 8, 15)
    private val december = LocalDate.of(2026, 12, 15)

    private val husky = Pet(id = "h", name = "โอม", species = "สุนัข", breed = "ไซบีเรียน ฮัสกี้ (Siberian Husky)")
    private val pug = Pet(id = "p", name = "ถั่วแดง", species = "สุนัข", breed = "ปั๊ก (Pug)")
    private val cat = Pet(id = "c", name = "เต้าหู้", species = "แมว", breed = "Scottish Fold")

    @Test
    fun seasons() {
        assertEquals(ThaiSeason.HOT, ThaiSeason.of(april))
        assertEquals(ThaiSeason.RAINY, ThaiSeason.of(august))
        assertEquals(ThaiSeason.COOL, ThaiSeason.of(december))
    }

    @Test
    fun traits_matchThaiAndEnglishNames() {
        assertTrue(BreedTrait.THICK_COAT in CareTipEngine.traitsOf("Siberian Husky"))
        assertTrue(BreedTrait.FLAT_FACE in CareTipEngine.traitsOf("ปั๊ก"))
        assertTrue(BreedTrait.LONG_BACK in CareTipEngine.traitsOf("ดัชชุนด์ (Dachshund)"))
        assertTrue(CareTipEngine.traitsOf("").isEmpty())
    }

    @Test
    fun hotSeason_huskyGetsHeatstrokeTipFirst_withPetName() {
        val tips = CareTipEngine.tipsFor(listOf(husky), april)
        assertEquals("อากาศร้อน ระวังโอมเป็นฮีตสโตรก", tips.first().title)
        assertTrue(tips.first().tag.contains("ไซบีเรียน ฮัสกี้"))
        assertFalse(tips.first().tag.contains("(")) // แสดงชื่อพันธุ์แบบสั้น
    }

    @Test
    fun pugGetsBreathingTipInAnySeason() {
        assertTrue(CareTipEngine.tipsFor(listOf(pug), december).any { it.title == "ปั๊กควรระวังเรื่องการหายใจ" })
    }

    @Test
    fun rainySeason_tickTipMergesAllDogsAndCats() {
        val tick = CareTipEngine.tipsFor(listOf(husky, cat), august).single { it.id == "rain-ticks" }
        assertEquals("หน้าฝนเห็บหมัดเยอะ", tick.title)
        assertTrue(tick.message.contains("โอมและเต้าหู้"))
        assertEquals(listOf("h", "c"), tick.petIds)
    }

    @Test
    fun noPets_showsGeneralSeasonTip() {
        val tips = CareTipEngine.tipsFor(emptyList(), august)
        assertEquals(listOf("general-rainy"), tips.map { it.id })
    }

    @Test
    fun joinNames() {
        assertEquals("โมจิ", CareTipEngine.joinNames(listOf("โมจิ")))
        assertEquals("โมจิและเต้าหู้", CareTipEngine.joinNames(listOf("โมจิ", "เต้าหู้")))
        assertEquals("โมจิ เต้าหู้ และอีก 2 ตัว", CareTipEngine.joinNames(listOf("โมจิ", "เต้าหู้", "a", "b")))
    }
}
