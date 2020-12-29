package unibas.dmi.sdatadirect

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.fasterxml.jackson.databind.ObjectMapper
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.net.wifi.p2p.FileTransferService
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.*

class ChatActivity : AppCompatActivity() {
    companion object {
        private val SOCKET_TIMEOUT: Int = 5000
        val TAG: String = "ChatSendingService"
        val ACTION_SEND_CHAT = "unibas.dmi.sdatadirect.SEND_CHAT"
        val EXTRAS_DESTINATION_ADDRESS = "desination_address"
        val EXTRAS_HOST_ADDRESS = "host"
        val EXTRAS_HOST_PORT = "port"
        val socket: Socket = Socket()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        }

    fun sendMessage(view: View) {

        val context: Context = applicationContext
        val destination_address: String? = intent?.extras?.getString(FileTransferService.EXTRAS_DESTINATION_ADDRESS)
        val host: String? = intent?.extras?.getString(FileTransferService.EXTRAS_HOST_ADDRESS)
        val port: Int? = intent?.extras?.getInt(FileTransferService.EXTRAS_HOST_PORT)

        val peerViewModel = EventBus.getDefault().getStickyEvent(PeerViewModel::class.java)

        try {


            Log.d(FileTransferService.TAG, "Opening client socket -")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port!!), ChatActivity.SOCKET_TIMEOUT)

            Log.d(FileTransferService.TAG, "Client socket - ${socket.isConnected}")
            val cr = context.contentResolver

            val peer = peerViewModel.getPeerByWiFiAddress(destination_address!!)

        } catch (e: IOException) {
            Log.e(FileTransferService.TAG, e.message)
        }
        val peer = peerViewModel.getPeerByWiFiAddress(destination_address!!)
        val cryptoHandler = EventBus.getDefault().getStickyEvent(CryptoHandler::class.java)
        val input: TextView = findViewById<EditText>(R.id.msgInput)
        val msg: String = input.text.toString()
        input.text = ""
        val encryptedString = cryptoHandler.encryptAES(msg.toByteArray(Charsets.UTF_8), peer?.shared_key!!)

        val StringStreamEncodedToString = Base64.getEncoder().encodeToString(encryptedString)

        val signature = cryptoHandler.createSignature(encryptedString, peer.private_key)

        val signatureEncodedToString = Base64.getEncoder().encodeToString(signature)


        val json: String =  "{\"string\" : \"$StringStreamEncodedToString\"," +
                "\"type\" : \"String\"," +
                "\"signature\" : \"$signatureEncodedToString\"}"


        val objectMapper = ObjectMapper()
        val node = objectMapper.readTree(json)
        val encodedNode = objectMapper.writeValueAsBytes(node)

        val outStream: OutputStream = socket.getOutputStream()
        //FileUtils.copyFile(encodedNode, outStream)
        outStream.write(encodedNode)
        socket.close()

    }

}