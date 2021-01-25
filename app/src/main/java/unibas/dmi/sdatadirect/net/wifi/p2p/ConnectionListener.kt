package unibas.dmi.sdatadirect.net.wifi.p2p
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils.MessageHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Asynchronous task to for running server socket. It receives the file sent by some client, verifies,
 * decrypts and saves it on the device
 *
 */
class ConnectionListener(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val cryptoHandler: CryptoHandler,
    val source_device_address: String,
    val msgHandler: MessageHandler = MessageHandler(context, activity, peerViewModel, cryptoHandler, source_device_address)

): AsyncTask<Void, Void, String>() {

    private val TAG = "ConnectionListener"


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
            peerViewModel.insertIp(client.inetAddress.toString().replace("/", ""), source_device_address)
            if (ConnectionManager.getSocket(source_device_address) == null){
                var sock = Socket()
                sock.bind(null)
                sock.connect(InetSocketAddress(client.inetAddress.toString().replace("/", ""), 8888), 5000)
                ConnectionManager.addConnection(source_device_address, sock)
            }

            //val peer = peerViewModel.getPeerByWiFiAddress(source_device_address!!)
            val inputstream = client.getInputStream()
            thread(start = true) {
                val ds = DataInputStream(inputstream)
                while (true){
                    try {
                        var len = ds.readInt()
                        var h = ByteArray(len)
                        if (len > 0) {
                            ds.readFully(h)
                            MessageHandler.msgQueue.add(h)
                        }
                    }catch (e: EOFException){

                    }
                }
            }
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
