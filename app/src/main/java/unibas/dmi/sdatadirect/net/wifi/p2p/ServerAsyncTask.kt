package unibas.dmi.sdatadirect.net.wifi.p2p

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.FileHandler
import unibas.dmi.sdatadirect.utils.QRCode
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class ServerAsyncTask(
    val context: Context,
    val activity: MainActivity,
    val cryptoHandler: CryptoHandler,
    val peerViewModel: PeerViewModel
): AsyncTask<Void, Void, String>() {

    private val TAG = "ServerAsyncTask"
    lateinit var serverSocket: ServerSocket
    lateinit var clientSocket: Socket
    val FILE_CHOSEN: Int = 1

    val handler: Handler = Handler(Handler.Callback {msg: Message ->

        when (msg.what) {

            FILE_CHOSEN -> {
                val cr = context.contentResolver
                var inputStream: InputStream? = null
                val fileHandler = FileHandler()
                var fileUri: Uri? = null

                try {
                    fileUri = msg.obj as Uri
                    inputStream = cr.openInputStream(fileUri)
                } catch (e: FileNotFoundException) {
                    Log.d(TAG, e.toString())
                }

                try {
                    val peer = peerViewModel.getPeerByWiFiAddress("c0:10:b1:34:91:52")
                    //val outStream = DataOutputStream(clientSocket.getOutputStream())
                    val outStream = clientSocket.getOutputStream()
                    val sharedKey = cryptoHandler.getSecretKeyDecoded(peer?.shared_key!!)
                    val encryptedMessage = cryptoHandler.encryptAES(inputStream?.readBytes()!!, sharedKey)
                    val signature = cryptoHandler.createSignature(inputStream.readBytes(), peer.private_key)
                    fileHandler.copyFile(inputStream?.readBytes()!!, outStream)
                    /*outStream.writeInt(encryptedMessage.size)
                    outStream.write(encryptedMessage)
                    outStream.writeInt(signature.size)
                    outStream.write(signature)*/
                    /*outStream.writeInt(cr.getType(fileUri!!).toByteArray().size)
                    outStream.write(cr.getType(fileUri!!).toByteArray())

                    Log.d(TAG, "Type: ${cr.getType(fileUri!!)}")*/

                    Log.d(TAG, "Client: Data Written")
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                } finally {
                    clientSocket.takeIf {it.isConnected}?.apply {
                        Log.d(TAG, "Client closed!")
                        clientSocket.close()
                    }
                }
            }
        }

        return@Callback true

    })

    override fun doInBackground(vararg params: Void?): String? {
        /**
         * Create a server socket.
         */
        serverSocket = ServerSocket(8888)
        //var receivedMessages: MutableList<ByteArray> = List<ByteArray>(2)

        return serverSocket.use {
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            clientSocket = Socket()
            clientSocket = serverSocket.accept()
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

            val inputStream = clientSocket.getInputStream()
            val fileHandler = FileHandler()
            fileHandler.copyFile(inputStream.readBytes(), FileOutputStream(f))

            /*try {

                val peer = peerViewModel.getPeerByWiFiAddress(clientSocket.inetAddress.hostAddress)
                val inputstream = clientSocket.getInputStream()

                val input = DataInputStream(inputstream)
                val sizeMessage = input.readInt()
                val messageEncrypted = ByteArray(sizeMessage)
                input.read(messageEncrypted, 0, sizeMessage)

                val sizeSignature = input.readInt()
                val signature = ByteArray(sizeSignature)
                input.read(signature, 0, sizeSignature)

                /*val sizeType = input.readInt()
                val type = ByteArray(sizeType)
                input.read(type, 0, sizeType)*/

                val sharedKey = cryptoHandler.getSecretKeyDecoded(peer?.shared_key!!)

                val verification = cryptoHandler.verifySignature(
                    signature,
                    messageEncrypted,
                    peer.foreign_public_key!!
                )

                if (verification) {
                    Toast.makeText(context, "Verification successful", Toast.LENGTH_SHORT)
                    val decryptedMessage = cryptoHandler.decryptAES(messageEncrypted, sharedKey)
                    val fileHandler = FileHandler()
                    fileHandler.copyFile(decryptedMessage, FileOutputStream(f))
                } else {

                    val message: Message = Message.obtain()
                    message.what = activity.VERIFICATION_FAILED
                    handler.sendMessage(message)
                }
                val inputStream = clientSocket.getInputStream()
                val fileHandler = FileHandler()
                fileHandler.copyFile(inputStream, FileOutputStream(f))

            } catch (e: IOException) {
                Log.e(TAG, e.message)
            } finally {
                serverSocket.close()
            }*/
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
