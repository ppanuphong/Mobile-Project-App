package com.petcare.app

import com.petcare.app.data.PetPhotoProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class PetPhotoProcessorTest {

    @Test
    fun sampleSize_keepsShortSideAtLeastTarget() {
        // รูปจากกล้อง 4000x3000 → ย่อ 4 เท่าเหลือ 1000x750 (ยังใหญ่กว่า 480)
        assertEquals(4, PetPhotoProcessor.sampleSize(4000, 3000, 480))
        // รูปเล็กกว่าเป้าหมายอยู่แล้ว ไม่ต้องย่อ
        assertEquals(1, PetPhotoProcessor.sampleSize(640, 480, 480))
        assertEquals(1, PetPhotoProcessor.sampleSize(300, 300, 480))
        // 1920x1080 → ย่อ 2 เท่าเหลือ 960x540
        assertEquals(2, PetPhotoProcessor.sampleSize(1920, 1080, 480))
    }
}
