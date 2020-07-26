package unibas.dmi.sdatadirect.net.wifi.p2p

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.utils.FileHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.ServerSocket

class FileServerAsyncTask(val context: Context): AsyncTask<Void, Void, String>() {

    private val TAG = "FileServerAsyncTask"

    override fun doInBackground(vararg params: Void?): String? {
        /**
         * Create a server socket.
         */
        val serverSocket = ServerSocket(8888)
        //var receivedMessages: MutableList<ByteArray> = List<ByteArray>(2)

        return serverSocket.use {
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            val client = serverSocket.accept()
            //statusText.text = "Connected to Client"
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */

            val f = File(context.getExternalFilesDir("received"),
                        "wifip2pshared-${System.currentTimeMillis()}.jpg")
            val dirs = File(f.parent)

            dirs.takeIf { it.doesNotExist() }?.apply {
                mkdirs()
            }
            f.createNewFile()
            val inputstream = client.getInputStream()
            val fileHandler = FileHandler()
            fileHandler.copyFile(inputstream.readBytes(), FileOutputStream(f))

            serverSocket.close()
            f.absolutePath
        }
    }



    private fun File.doesNotExist(): Boolean = !exists()

    override fun onPostExecute(result: String?) {
        //super.onPostExecute(result)

        result?.run {
            val recvFile = File(result)
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "unibas.dmi.sdatadirect.fileprovider",
                recvFile
            )
            val intent = Intent()
            intent.action = android.content.Intent.ACTION_VIEW
            intent.setDataAndType(fileUri, "image/*")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(intent)
        }
    }

    override fun onPreExecute() {
        //super.onPreExecute()
        //statusText.text = "Opening a server socket"
    }
}
