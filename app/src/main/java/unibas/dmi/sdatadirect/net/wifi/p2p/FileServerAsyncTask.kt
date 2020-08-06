package unibas.dmi.sdatadirect.net.wifi.p2p

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Message
import android.util.Log
import androidx.core.content.FileProvider
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.FileUtils
import java.io.*
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.*

/**
 * Asynchronous task to for running server socket. It receives the file sent by some client, verifies,
 * decrypts and saves it on the device.
 */
class FileServerAsyncTask(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val cryptoHandler: CryptoHandler,
    val source_device_address: String?
): AsyncTask<Void, Void, String>() {

    private val TAG = "FileServerAsyncTask"
    private var f: File? = null


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
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             * */

            val peer = peerViewModel.getPeerByWiFiAddress(source_device_address!!)
            val inputstream = client.getInputStream()
            Log.d(TAG, "Server: Data received: ${System.currentTimeMillis()}")

            val objectMapper = ObjectMapper()
            val decodedNode = objectMapper.readTree(ByteArrayInputStream(inputstream.readBytes()))

            val receivedFile: ByteArray = Base64.getDecoder().decode(decodedNode.get("file").asText())
            val receivedType: ByteArray = Base64.getDecoder().decode(decodedNode.get("type").asText())
            val receivedSignature: ByteArray = Base64.getDecoder().decode(decodedNode.get("signature").asText())

            val verification = cryptoHandler.verifySignature(
                receivedSignature,
                receivedFile,
                peer?.foreign_public_key!!
            )

            if (verification) {
                val decryptedFile = cryptoHandler.decryptAES(receivedFile, peer?.shared_key)
                Log.d(TAG, "Server: File decrypted: ${System.currentTimeMillis()}")
                f = File(context.getExternalFilesDir("received"),
                    "wifip2pshared-${System.currentTimeMillis()}.jpg")
                val dirs = File(f?.parent)

                dirs.takeIf { it.doesNotExist() }?.apply {
                    mkdirs()
                }
                f?.createNewFile()


                Log.d(TAG, "Type: ${String(receivedType, Charset.defaultCharset())}")

                FileUtils.copyFile(decryptedFile, FileOutputStream(f))
            } else {
                val message = Message.obtain()
                message.what = activity.VERIFICATION_FAILED
                activity.handler.sendMessage(message)
            }


            serverSocket.close()
            f?.absolutePath
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
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(fileUri, "*/*")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(intent)

        }
    }

    override fun onPreExecute() {
        //super.onPreExecute()
        //statusText.text = "Opening a server socket"
    }
}
