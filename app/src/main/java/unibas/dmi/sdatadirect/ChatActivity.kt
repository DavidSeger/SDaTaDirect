package unibas.dmi.sdatadirect

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.android.synthetic.main.activity_chat.*
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.net.wifi.p2p.FileTransferService
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
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

        val context: Context = applicationContext
        val destination_address: String? = intent?.extras?.getString(FileTransferService.EXTRAS_DESTINATION_ADDRESS)
        val host: String? = intent?.extras?.getString(FileTransferService.EXTRAS_HOST_ADDRESS)
        val port: Int? = intent?.extras?.getInt(FileTransferService.EXTRAS_HOST_PORT)

        val peerViewModel = EventBus.getDefault().getStickyEvent(PeerViewModel::class.java)
        val cryptoHandler = EventBus.getDefault().getStickyEvent(CryptoHandler::class.java)

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
        }

    fun sendMessage(view: View) {
        val msg: String = textView2.text.toString()
        textView2.text = ""
        val outStream: OutputStream = socket.getOutputStream()
        //FileUtils.copyFile(encodedNode, outStream)
        outStream.write(msg.toByteArray())


    }

}