package com.petcare.app

import com.petcare.app.data.PetPhotoProcessor
import com.petcare.app.ui.components.nameInitial
import com.petcare.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProfileHelpersTest {

    @Test
    fun cropSize_keepsTargetAspectRatio() {
        // รูปแนวนอน 4000x3000 → หน้าปก 960x360 (8:3) ตัดบนล่าง
        assertEquals(4000 to 1500, PetPhotoProcessor.cropSize(4000, 3000, 960, 360))
        // รูปแนวตั้ง 3000x4000 → จัตุรัส ตัดบนล่างเหลือ 3000x3000
        assertEquals(3000 to 3000, PetPhotoProcessor.cropSize(3000, 4000, 480, 480))
        // รูปพาโนรามา 6000x1000 → หน้าปก ตัดซ้ายขวา
        assertEquals(2666 to 1000, PetPhotoProcessor.cropSize(6000, 1000, 960, 360))
    }

    @Test
    fun thaiMonthYear_usesBuddhistYear() {
        val millis = LocalDate.of(2026, 10, 13).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("ตุลาคม 2569", DateUtils.thaiMonthYear(millis, ZoneOffset.UTC))
    }

    @Test
    fun nameInitial() {
        assertEquals("ป", nameInitial("  ปาม ทุนพิชัย"))
        assertEquals("T", nameInitial("tester"))
        assertEquals("?", nameInitial(""))
    }
}
