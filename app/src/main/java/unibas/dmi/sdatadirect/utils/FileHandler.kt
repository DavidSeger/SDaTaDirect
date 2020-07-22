package unibas.dmi.sdatadirect.utils

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FileHandler {

    private val TAG: String = "FileHandler"

    fun copyFile(inputStream: InputStream, outputStream: OutputStream?): Boolean {
        val buf: ByteArray = ByteArray(1024)
        var len: Int = 0

        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                Log.d(TAG, "While in")
                outputStream?.write(buf, 0, len)
                Log.d(TAG, "While out")
            }
            Log.d(TAG, "WRITEEN")
            outputStream?.close()
            inputStream.close()
            //outputStream?.write(inputStream)
            //outputStream?.close()
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
            return false
        }

        return true
    }

}
