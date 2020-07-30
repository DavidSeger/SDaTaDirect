package unibas.dmi.sdatadirect.utils

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtils {

    private val TAG: String = "FileHandler"

    fun copyFile(inputStream: InputStream?, outputStream: OutputStream){
        val buf: ByteArray = ByteArray(1024)
        var len: Int? = null

        try {
            while (inputStream?.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len!!)
            }
            //outputStream.close()
            inputStream?.close()
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
        }
    }

    fun copyFile(inputStreamByteArray: ByteArray, outputStream: OutputStream): Boolean {

        try {
            outputStream.write(inputStreamByteArray)
            outputStream.close()
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
            return false
        }

        return true
    }

}
