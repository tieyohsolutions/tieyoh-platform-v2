package com.tieyoh.mobile

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

// Turns a Tieyoh ID into a scannable QR code so someone can identify /
// add another user by camera instead of typing their handle. Encodes the
// plain ID string for now (e.g. "@t-hgeorgy") — wiring an in-app scanner
// to look that up is a separate, later piece of work.
object QrCodeGenerator {

    fun generate(content: String, sizePx: Int = 240): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
