package unibas.dmi.sdatadirect.utils

import android.app.Dialog
import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.Writer
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.R
import java.lang.StringBuilder

class QRCode(val activity: MainActivity) {

    fun generateQRCode(input: String): Bitmap{
        val textToEncode: StringBuilder = StringBuilder()
        textToEncode.append(input)
        val multiFormatWriter = MultiFormatWriter()
        var bitmap: Bitmap? = null

        try {
            val bitMatrix = multiFormatWriter.encode(textToEncode.toString(), BarcodeFormat.QR_CODE,
            600, 600)
            val barcodeEncoder = BarcodeEncoder()
            bitmap = barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return bitmap!!
    }

    fun showQrCode(bitmap: Bitmap) {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.qrcodedialog)

        val doneBtn: Button = dialog.findViewById(R.id.doneBtn)

        doneBtn.setOnClickListener {
            dialog.cancel()
        }

        val imageView: ImageView = dialog.findViewById(R.id.imageView)
        imageView.setImageBitmap(bitmap)
        imageView.visibility = View.VISIBLE

        dialog.show()
    }

    fun scanQRCode() {
        val scanner = IntentIntegrator(activity)
        scanner.initiateScan()
    }
}