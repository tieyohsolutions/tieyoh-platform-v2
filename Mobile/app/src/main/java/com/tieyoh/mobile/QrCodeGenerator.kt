package com.tieyoh.mobile

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

// Turns a Tieyoh ID into a scannable QR code so someone can identify /
// add another user by camera instead of typing their handle. Encodes the
// plain ID string for now (e.g. "@t-hgeorgy") — wiring an in-app scanner
// to look that up is a separate, later piece of work.
object QrCodeGenerator {

    // marginModules is the "quiet zone" ZXing pads around the code, in QR
    // modules (default 4). Kept as a parameter rather than hard-coding a
    // smaller value everywhere -- MyQrCodeScreen's existing look keeps the
    // library default (4) via the default param below, while a caller that
    // wants the pattern to fill its box edge-to-edge (PayWithQrScreen, to
    // match its reference design) passes a smaller value explicitly.
    fun generate(content: String, sizePx: Int = 240, marginModules: Int = 4): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = mapOf(EncodeHintType.MARGIN to marginModules)
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
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
