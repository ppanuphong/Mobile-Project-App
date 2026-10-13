package com.petcare.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64

/**
 * แปลงรูปที่ผู้ใช้เลือก/ถ่าย ให้เป็นรูปขนาดเล็กสำหรับเก็บใน Firestore
 * ครอปตรงกลางตามสัดส่วนที่ต้องการ → ย่อ → JPEG → Base64
 * ใช้ทั้งรูปสัตว์เลี้ยง/รูปโปรไฟล์ (จัตุรัส [SIZE]) และรูปหน้าปกผู้ใช้ ([COVER_WIDTH]x[COVER_HEIGHT])
 */
class PetPhotoProcessor(private val context: Context) {

    suspend fun encode(uri: Uri, width: Int = SIZE, height: Int = SIZE): String = withContext(Dispatchers.IO) {
        val decoded = decode(uri, maxOf(width, height)) ?: throw IOException("อ่านรูปไม่ได้")
        val cropped = cropCenter(decoded, width, height)
        val bytes = ByteArrayOutputStream().use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
        if (bytes.size > MAX_BYTES) throw IOException("รูปใหญ่เกินไป")
        Base64.getEncoder().encodeToString(bytes)
    }

    private fun decode(uri: Uri, target: Int): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder หมุนรูปตาม EXIF ให้เอง
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, target))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        // Android 8.x: อ่านขนาดก่อนเพื่อย่อตอน decode แล้วหมุนตาม EXIF เอง
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
    }

    /** ครอปตรงกลางให้ได้สัดส่วน width:height แล้วย่อเป็นขนาดนั้นพอดี */
    private fun cropCenter(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val (cropW, cropH) = cropSize(bitmap.width, bitmap.height, width, height)
        val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - cropW) / 2, (bitmap.height - cropH) / 2, cropW, cropH)
        return cropped.scale(width, height)
    }

    companion object {
        const val SIZE = 480
        const val COVER_WIDTH = 960
        const val COVER_HEIGHT = 360
        private const val QUALITY = 80
        /** กันไม่ให้เอกสาร Firestore ใกล้ขีดจำกัด 1 MB */
        private const val MAX_BYTES = 300_000

        /** ขนาดพื้นที่ครอปที่ใหญ่ที่สุดในรูป srcW x srcH ที่มีสัดส่วน targetW:targetH */
        fun cropSize(srcW: Int, srcH: Int, targetW: Int, targetH: Int): Pair<Int, Int> =
            if (srcW.toLong() * targetH > srcH.toLong() * targetW) {
                (srcH.toLong() * targetW / targetH).toInt() to srcH // รูปกว้างกว่า → ตัดซ้ายขวา
            } else {
                srcW to (srcW.toLong() * targetH / targetW).toInt() // รูปสูงกว่า → ตัดบนล่าง
            }

        /** ย่อตอน decode ทีละ 2 เท่า ให้ด้านที่สั้นกว่ายังใหญ่กว่า [target] */
        fun sampleSize(width: Int, height: Int, target: Int): Int {
            var sample = 1
            while (minOf(width, height) / (sample * 2) >= target) sample *= 2
            return sample
        }
    }
}
