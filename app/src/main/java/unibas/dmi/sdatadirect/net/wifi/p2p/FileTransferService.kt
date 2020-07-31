package unibas.dmi.sdatadirect.net.wifi.p2p

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.*

/**
 * Service triggered by the main thread after choosing a file. It encrypts and signs the chosen file
 * and sends it to the host.
 */
class FileTransferService: IntentService(FileTransferService::class.simpleName) {

    companion object {
        private val SOCKET_TIMEOUT: Int = 5000
        val TAG: String = "FileTransferService"
        val ACTION_SEND_FILE = "unibas.dmi.sdatadirect.SEND_FILE"
        val EXTRAS_FILE_PATH = "file_url"
        val EXTRAS_DESTINATION_ADDRESS = "desination_address"
        val EXTRAS_HOST_ADDRESS = "host"
        val EXTRAS_HOST_PORT = "port"
    }


    /**
     * Handling intent sent by MainActivity. Opens a socket and tries to establish a connection
     * with the desired host.
     */
    override fun onHandleIntent(intent: Intent?) {
        val context: Context = applicationContext
        val fileUri: String? = intent?.extras?.getString(EXTRAS_FILE_PATH)
        val destination_address: String? = intent?.extras?.getString(EXTRAS_DESTINATION_ADDRESS)
        val host: String? = intent?.extras?.getString(EXTRAS_HOST_ADDRESS)
        val socket = Socket()
        val port: Int? = intent?.extras?.getInt(EXTRAS_HOST_PORT)

        val peerViewModel = EventBus.getDefault().getStickyEvent(PeerViewModel::class.java)
        val cryptoHandler = EventBus.getDefault().getStickyEvent(CryptoHandler::class.java)

        try {


            Log.d(TAG, "Opening client socket -")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port!!), SOCKET_TIMEOUT)

            Log.d(TAG, "Client socket - ${socket.isConnected}")
            val cr = context.contentResolver
            var inputStream: InputStream? = null
            val uriType: String? = cr.getType(Uri.parse(fileUri))

            try {
                inputStream = cr.openInputStream(Uri.parse(fileUri))
            } catch (e: FileNotFoundException) {
                Log.d(TAG, e.toString())
            }

            val peer = peerViewModel.getPeerByWiFiAddress(destination_address!!)

            val objectMapper = ObjectMapper()

            val fileToSend = inputStream?.readBytes()!!

            Log.d(TAG, "Client: Start encryption: ${System.currentTimeMillis()}")

            val encryptedFile = cryptoHandler.encryptAES(fileToSend, peer?.shared_key!!)

            val fileStreamEncodedToString = Base64.getEncoder().encodeToString(encryptedFile)
            val uriTypeEncodedToString = Base64.getEncoder().encodeToString(uriType?.toByteArray(
                Charset.defaultCharset()))

            val signature = cryptoHandler.createSignature(encryptedFile, peer.private_key)

            val signatureEncodedToString = Base64.getEncoder().encodeToString(signature)


            val json: String =  "{\"file\" : \"$fileStreamEncodedToString\"," +
                                "\"type\" : \"$uriTypeEncodedToString\"," +
                                "\"signature\" : \"$signatureEncodedToString\"}"

            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)

            val outStream: OutputStream = socket.getOutputStream()
            //FileUtils.copyFile(encodedNode, outStream)
            Log.d(TAG, "Client: Data Written: ${System.currentTimeMillis()}")
            outStream.write(encodedNode)

            if (socket.isClosed) {
                Log.d(TAG, "Client: Is closed")
            }

        } catch (e: IOException) {
            Log.e(TAG, e.message)
        } finally {
            socket.takeIf { it.isConnected }?.apply {
                Log.d(TAG, "Client closed!")
                close()
            }
        }


    }
}