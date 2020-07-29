package unibas.dmi.sdatadirect.net.wifi.p2p

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.renderscript.ScriptGroup
import android.util.Log
import unibas.dmi.sdatadirect.utils.FileHandler
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class FileTransferService: IntentService(FileTransferService::class.simpleName) {
    override fun onHandleIntent(p0: Intent?) {
        TODO("Not yet implemented")
    }

    /*companion object {
        private val SOCKET_TIMEOUT: Int = 5000
        val ACTION_SEND_FILE = "unibas.dmi.sdatadirect.SEND_FILE"
        val EXTRAS_FILE_PATH = "file_url"
        val EXTRAS_GROUP_OWNDER_ADDRESS = "go_host"
        val EXTRAS_GROUP_OWNER_PORT = "go_port"
        val EXTRAS_SOCKET = "socket"
        val TAG: String = "FileTransferService"
    }


    override fun onHandleIntent(intent: Intent?) {
        val context: Context = applicationContext
        val fileUri: String? = intent?.extras?.getString(EXTRAS_FILE_PATH)
        val host: String? = intent?.extras?.getString(EXTRAS_GROUP_OWNDER_ADDRESS)
        val socket = intent?.extras?.get(EXTRAS_SOCKET)
        val port: Int? = intent?.extras?.getInt(EXTRAS_GROUP_OWNER_PORT)
        val fileHandler = FileHandler()

        try {
            Log.d(TAG, "Opening client socket -")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port!!), SOCKET_TIMEOUT)

            Log.d(TAG, "Client socket - ${socket.isConnected}")
            val cr = context.contentResolver
            var inputStream: InputStream? = null


            try {
                inputStream = cr.openInputStream(Uri.parse(fileUri))
            } catch (e: FileNotFoundException) {
                Log.d(TAG, e.toString())
            }

            val outStream = socket.getOutputStream()
            fileHandler.copyFile(inputStream?.readBytes()!!, outStream)

            Log.d(TAG, "Client: Data Written")

        } catch (e: IOException) {
            Log.e(TAG, e.message)
        } finally {
            socket.takeIf { it.isConnected }?.apply {
                Log.d(TAG, "Client closed!")
                close()
            }
        }

    }*/
}
