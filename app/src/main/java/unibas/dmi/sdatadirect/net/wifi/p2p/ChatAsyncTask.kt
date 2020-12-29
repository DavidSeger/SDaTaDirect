package unibas.dmi.sdatadirect.net.wifi.p2p
import android.content.Context
import android.os.AsyncTask
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.ByteArrayInputStream
import java.io.File
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.*

/**
 * Asynchronous task to for running server socket. It receives the file sent by some client, verifies,
 * decrypts and saves it on the device.
 */
class ChatAsyncTask(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val cryptoHandler: CryptoHandler,
    val source_device_address: String?
): AsyncTask<Void, Void, String>() {

    private val TAG = "ChatAsyncTask"


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
            val peer = peerViewModel.getPeerByWiFiAddress(source_device_address!!)
            val inputstream = client.getInputStream()
            Log.d(TAG, "Server: Data received: ${System.currentTimeMillis()}")

            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             * */
            val objectMapper = ObjectMapper()
            val decodedNode = objectMapper.readTree(ByteArrayInputStream(inputstream.readBytes()))

            val receivedMsg: ByteArray = Base64.getDecoder().decode(decodedNode.get("string").asText())
            val receivedType: ByteArray = Base64.getDecoder().decode(decodedNode.get("type").asText())
            val receivedSignature: ByteArray = Base64.getDecoder().decode(decodedNode.get("signature").asText())

            val verification = cryptoHandler.verifySignature(
                receivedSignature,
                receivedMsg,
                peer?.foreign_public_key!!
            )

            if (verification) {
                val decryptedMsg = cryptoHandler.decryptAES(receivedMsg, peer?.shared_key).toString(Charsets.UTF_8)
                Log.d(TAG, "Server: Msg decrypted: ${System.currentTimeMillis()}")
                Log.d(TAG, "msg: $decryptedMsg")
                activity.runOnUiThread {
                    Toast.makeText(activity, decryptedMsg, Toast.LENGTH_LONG).show()
                }

                Log.d(TAG, "Type: ${String(receivedType, Charset.defaultCharset())}")
            } else {
                val message = Message.obtain()
                message.what = activity.VERIFICATION_FAILED
                activity.handler.sendMessage(message)
            }


            serverSocket.close()
            "what"
        }
    }

    private fun File.doesNotExist(): Boolean = !exists()

    override fun onPostExecute(result: String?) {

    }

    override fun onPreExecute() {
        //super.onPreExecute()
        //statusText.text = "Opening a server socket"
    }
}
