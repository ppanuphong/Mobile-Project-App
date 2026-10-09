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
 * แปลงรูปที่ผู้ใช้เลือก/ถ่าย ให้เป็นรูปโปรไฟล์ขนาดเล็กสำหรับเก็บใน Firestore
 * ครอปเป็นสี่เหลี่ยมจัตุรัส → ย่อเหลือ [SIZE]x[SIZE] → JPEG → Base64
 */
class PetPhotoProcessor(private val context: Context) {

    suspend fun encode(uri: Uri): String = withContext(Dispatchers.IO) {
        val decoded = decode(uri) ?: throw IOException("อ่านรูปไม่ได้")
        val square = cropSquare(decoded, SIZE)
        val bytes = ByteArrayOutputStream().use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
        if (bytes.size > MAX_BYTES) throw IOException("รูปใหญ่เกินไป")
        Base64.getEncoder().encodeToString(bytes)
    }

    private fun decode(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder หมุนรูปตาม EXIF ให้เอง
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, SIZE))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        // Android 8.x: อ่านขนาดก่อนเพื่อย่อตอน decode แล้วหมุนตาม EXIF เอง
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, SIZE)
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

    private fun cropSquare(bitmap: Bitmap, size: Int): Bitmap {
        val side = minOf(bitmap.width, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        return cropped.scale(size, size)
    }

    companion object {
        const val SIZE = 480
        private const val QUALITY = 80
        /** กันไม่ให้เอกสาร Firestore ใกล้ขีดจำกัด 1 MB */
        private const val MAX_BYTES = 300_000

        /** ย่อตอน decode ทีละ 2 เท่า ให้ด้านที่สั้นกว่ายังใหญ่กว่า [target] */
        fun sampleSize(width: Int, height: Int, target: Int): Int {
            var sample = 1
            while (minOf(width, height) / (sample * 2) >= target) sample *= 2
            return sample
        }
    }
}
