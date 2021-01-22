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
import unibas.dmi.sdatadirect.utils.PackageFactory
import unibas.dmi.sdatadirect.utils.PackageFactory.METHOD.*
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
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
        val peerViewModel = EventBus.getDefault().getStickyEvent(PeerViewModel::class.java)
        val destination_address: String? =
            intent?.extras?.getString(FileTransferService.EXTRAS_DESTINATION_ADDRESS)
        val peer = peerViewModel.getPeerByWiFiAddress(destination_address!!)
        val cryptoHandler = EventBus.getDefault().getStickyEvent(CryptoHandler::class.java)
        val input: TextView = findViewById<EditText>(R.id.msgInput)
        val msg: String = input.text.toString()
        input.text = ""

        val encryptedPackage = cryptoHandler.encryptAES(PackageFactory.buildPayload(SEND_STRING, msg), peer?.shared_key!!)

        val packageStreamEncodedToString = Base64.getUrlEncoder().encodeToString(encryptedPackage)

        val signature = cryptoHandler.createSignature(encryptedPackage, peer.private_key)

        val signatureEncodedToString = Base64.getEncoder().encodeToString(signature)


        val json: String =  "{\"package\" : \"$packageStreamEncodedToString\"," +
                "\"type\" : \"null\"," +
                "\"signature\" : \"$signatureEncodedToString\"}"

        Log.d(TAG, json)
        val objectMapper = ObjectMapper()
        val node = objectMapper.readTree(json)
        val encodedNode = objectMapper.writeValueAsBytes(node)
        val outStream: OutputStream = socket.getOutputStream()
        val dataWriter = DataOutputStream(outStream)


        //FileUtils.copyFile(encodedNode, outStream)
        dataWriter.writeInt(encodedNode.size)
        dataWriter.write(encodedNode)
    }

}