package unibas.dmi.sdatadirect.net.wifi.p2p

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.LinearGradient
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.widget.*
import androidx.core.content.FileProvider
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.ui.BluetoothDeviceListAdapter
import unibas.dmi.sdatadirect.ui.WifiP2pDeviceListAdapter
import unibas.dmi.sdatadirect.utils.FileHandler
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.jar.Manifest

class WifiP2pDriver (val activity: MainActivity) {

    val TAG: String = "WifiP2pDriver"

    val manager: WifiP2pManager = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    var channel: WifiP2pManager.Channel = manager.initialize(activity, activity.mainLooper, null)
    val receiver: BroadcastReceiver = WifiP2pBroadcastReceiver(this, manager, channel, activity)


    /*val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null

    init {
        channel = manager?.initialize(activity, activity.mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { WifiP2pBroadcastReceiver(this, it, channel, activity) }
        }
    }*/

    var isWifiP2pEnabled: Boolean = true

    val peers: MutableList<WifiP2pDevice> = mutableListOf()
    val deviceNameArray: MutableList<String> = mutableListOf()
    val deviceArray: MutableList<WifiP2pDevice> = mutableListOf()

    val listView: ListView = activity.findViewById(R.id.peerView)

    val adapter: ArrayAdapter<WifiP2pDevice> = ArrayAdapter(activity,
        android.R.layout.simple_list_item_1, peers)

    lateinit var server: Server
    lateinit var client: Client
    lateinit var sendReceive: SendReceive

    var fileUri: String = ""

    var isServer = false
    var isClient = false

    lateinit var groupOwnerAddress: InetAddress

    var wifiP2pDeviceListAdapter: WifiP2pDeviceListAdapter = WifiP2pDeviceListAdapter(activity, R.layout.device_adapter_view, peers)



    /**
     * DiscoverPeers detects available peers that are in range. If the discovery was successful,
     * the system broadcasts the WIFI_P2P_PEERS_CHANGED_ACTION intent which will be used to save
     * the list of peers.
     */

    fun checkPermission() {
        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_WIFI_STATE), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.CHANGE_WIFI_STATE), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.CHANGE_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.CHANGE_NETWORK_STATE), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.INTERNET), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_NETWORK_STATE), 1)
        }


    }

    fun discoverPeers(){

        checkPermission()

        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //...
                activity.textView.text = "Discovery Started"
                Log.d(TAG, "Discovery successful")
            }

            override fun onFailure(reasonCode: Int) {
                //...
                activity.textView.text = "Discovery Starting failed"
                Log.d(TAG, "Discovery failed")
            }
        })
    }

    /**
     * Connect method establishes a connection to a desired device. Within the method, a WifiP2pConfig
     * will be specified containing the information of the device to connect to. The WifiP2pManager.ActionListener
     * notifies you of a connection success or failure.
     */
    fun connect(target: WifiP2pDevice) {

        checkPermission()

        val config = WifiP2pConfig().apply {
            deviceAddress = target.deviceAddress
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(activity, "Connected successfully to ${target.deviceName}",
                    Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Connection to [${target.deviceName}, ${target.deviceAddress}] was successful")
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(activity, "Connect failed. Retry.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Connection failed")
            }

        })
    }

    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            activity.listView.adapter = wifiP2pDeviceListAdapter
        }

        if (peers.isEmpty()) {
            Toast.makeText(activity, "No Device Found", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "No devices found")
            activity.listView.adapter = wifiP2pDeviceListAdapter
            return@PeerListListener
        }

    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->

        val groupOwnerAdress: InetAddress = info.groupOwnerAddress
        groupOwnerAddress = info.groupOwnerAddress
        Log.d(TAG, groupOwnerAddress.hostAddress)

        if (info.groupFormed && info.isGroupOwner) {
            activity.textView.text = "Host!"
            //FileServerAsyncTask(activity.applicationContext, activity.textView).execute()
            server = Server(activity.textView)
            server.start()
            isServer = true
        } else if (info.groupFormed) {
            activity.textView.text = "Client!"
            client = Client(groupOwnerAdress, activity.textView)
            client.start()
            isClient = true
        }
    }

    fun createGroup() {
        checkPermission()
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                println("Connected successfully!")
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(activity, "Group creation failed. Retry.", Toast.LENGTH_SHORT).show()
            }

        })
    }

    fun stopWifiP2pDriver() {
        manager.stopPeerDiscovery(channel, object: WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })

        manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })

        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })
    }

    inner class Server(val textView: TextView): Thread() {
        lateinit var socket: Socket
        lateinit var serverSocket: ServerSocket
        var inputStream: InputStream? = null
        //var outputStream: OutputStream? = null

        override fun run() {
            serverSocket = ServerSocket(8888)
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            socket = serverSocket.accept()
            //textView.text = "Connected to Client"
            //Toast.makeText(activity, "Connected to Client", Toast.LENGTH_SHORT)
            inputStream = socket.getInputStream()
            val buf: ByteArray = ByteArray(1024)
            //sendReceive = SendReceive(socket)
            //sendReceive.start()


                try {
                    serverSocket.use {

                        /**
                         * If this code is reached, a client has connected and transferred data
                         * Save the input stream from the client as a JPEG file
                         */
                        while (!socket.isClosed) {
                            val bytes = inputStream?.read(buf)
                            if (bytes != null) {
                                if (bytes > 0) {
                                    println("Absolut path: " + Environment.getDataDirectory().absolutePath)
                                    println("Path" + Environment.getDataDirectory().path)
                                    /*val f = File(
                                        Environment.getExternalStorageDirectory().absolutePath +
                                                "/${activity.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg"
                                    )*/

                                    val f: File = File(activity.getExternalFilesDir("received"),
                                    "wifip2pshared-" + System.currentTimeMillis()
                                    + ".jpg")

                                    val dirs = File(f.parent)

                                    dirs.takeIf { it.doesNotExist() }?.apply {
                                        mkdirs()
                                    }
                                    f.createNewFile()
                                    //val inputstream = client.getInputStream()
                                    val fileHandler = FileHandler()
                                    val tempInputStream = inputStream
                                    val outputStream = FileOutputStream(f)
                                    fileHandler.copyFile(tempInputStream!!, outputStream)
                                    serverSocket.close()
                                    Log.d(TAG, "Server closed!")

                                    //f.absolutePath
                                    //textView.text = "File copied - $f.absolutePath"
                                    //Toast.makeText(activity, "File copied - $f.absolutePath", Toast.LENGTH_SHORT)
                                    Log.d(TAG, "URI PATH IN SERVER: ${f.absolutePath}")
                                    Log.d(TAG, "URI PATH IN SERVER: ${f.path}")
                                    /*val intent = Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse("${f.absolutePath}"), "")
                                    }*/


                                    val recvFile = File(f.absolutePath)
                                    val fileUri: Uri = FileProvider.getUriForFile(
                                        activity,
                                        "unibas.dmi.sdatadirect.fileprovider",
                                        recvFile
                                    )
                                    val intent = Intent()
                                    intent.action = android.content.Intent.ACTION_VIEW
                                    intent.setDataAndType(fileUri, "image/*")
                                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    activity.startActivity(intent)
                                    //activity.startActivity(intent)
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }
            }

        private fun File.doesNotExist(): Boolean = !exists()
    }

    inner class Client(hostAdress: InetAddress, val textView: TextView): Thread() {
        var socket: Socket
        var hostAdd: String
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        val context: Context = activity
        val cr = context.contentResolver

        init {
            socket = Socket()
            this.hostAdd = hostAdress.hostAddress
        }

        override fun run() {
            socket.bind(null)
            socket.connect(InetSocketAddress(hostAdd, 8888))
            //textView.text = "Connected to Server"
            //Toast.makeText(activity, "Connected to server", Toast.LENGTH_SHORT)
            Log.d(TAG, "Client socket - ${socket.isConnected}")
            outputStream = socket.getOutputStream()

            while (!socket.isClosed) {
                try {

                    //sendReceive = SendReceive(socket)
                    //sendReceive.start()

                    if (fileUri != "") {

                        try {
                            val uri: Uri = Uri.parse(fileUri)
                            inputStream = cr.openInputStream(uri)
                            Log.d(TAG, "URI details: ${uri.path}")
                            Log.d(TAG, "URI details: ${uri.authority}")
                            Log.d(TAG, "URI details: ${uri.host}")
                            
                        } catch (e: FileNotFoundException) {
                            Log.d(TAG, "File not found")
                            Log.d(TAG, e.toString())
                        }
                        val fileHandler = FileHandler()
                        val tempOutPutStream = outputStream
                        val tempInputStream = inputStream
                        fileHandler.copyFile(tempInputStream!!, tempOutPutStream)
                        Log.d(TAG, "Client: Data Written")
                        /*socket.takeIf { it.isConnected }?.apply {
                            close()
                            Log.d(TAG, "Client closed!")
                        }*/
                    }

                } catch (e: FileNotFoundException) {
                    Log.d(TAG, "File not found")
                    e.printStackTrace()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class SendReceive(socket: Socket?): Thread() {
        var socket: Socket? = null
        lateinit var inputStream: InputStream
        lateinit var outputStream: OutputStream

        init {
            this.socket = socket

            try {
                inputStream = this.socket?.getInputStream()!!
                outputStream = this.socket?.getOutputStream()!!
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while(this.socket != null) {

                bytes = inputStream.read(buffer)

                if (bytes > 0) {

                    val f = File(
                        Environment.getExternalStorageDirectory().absolutePath +
                                "/${activity.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg"
                    )
                    val dirs = File(f.parent)

                    dirs.takeIf { it.doesNotExist() }?.apply {
                        mkdirs()
                    }
                    f.createNewFile()
                    copyFile(inputStream, FileOutputStream(f))
                    Toast.makeText(activity, "SAVED", Toast.LENGTH_SHORT).show()
                }
            }
        }



        fun copyFile(inputStream: InputStream, outputStream: FileOutputStream) {
            outputStream.use { fileOutputStream ->  inputStream.copyTo(fileOutputStream)}
        }

        fun File.doesNotExist(): Boolean = !exists()

        fun write(uri: Uri) {
            val buffer = ByteArray(1024)
            var len: Int = 0
            try {
                val cr = activity.contentResolver
                val _inputStream: InputStream? = cr.openInputStream(uri)
                while (_inputStream?.read(buffer).also { len = it!! } != -1) {
                    this.outputStream.write(buffer, 0, len)
                }
                Toast.makeText(activity, "WRITTEN", Toast.LENGTH_SHORT).show()
                _inputStream?.close()
                this.outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }


    inner class FileServerAsyncTask(val context: Context, val statusText: TextView): AsyncTask<Void, Void, String>() {


        override fun doInBackground(vararg params: Void?): String? {
            /*try {
                val serverSocket: ServerSocket = ServerSocket(8988)
                Log.d(TAG, "Server: Socket opened")
                val client: Socket = serverSocket.accept()
                Log.d(TAG, "Server: connection done")

                val file = context.getExternalFilesDir("received")
                println("File extension: " + file?.extension)
                println("File name: " + file?.name)
                println("File path: " + file?.path)
                println("File parent: " + file?.parent)
                println("File canonicalPath: " + file?.canonicalPath)

                val f = File(
                    context.getExternalFilesDir("received"), "wifip2pshared-"
                            + System.currentTimeMillis() + ".jpg"
                )

                val dirs = File(f.parent)
                if (!dirs.exists()) {
                    dirs.mkdirs()
                }
                f.createNewFile()

                Log.d(TAG, "Server: copying files ${f.toString()}")
                val inputStream: InputStream = client.getInputStream()
                FileHandler.copyFile(inputStream, FileOutputStream(f))
                serverSocket.close()
                activity.textView.text = "Server closed"
                return f.absolutePath
            } catch (e: IOException) {
                Log.e(TAG, e.message)
                return null
            }*/
            /**
             * Create a server socket.
             */
            val serverSocket = ServerSocket(8888)
            return serverSocket.use {
                /**
                 * Wait for client connections. This call blocks until a
                 * connection is accepted from a client.
                 */
                val client = serverSocket.accept()
                activity.textView.text = "Connected to Client"
                /**
                 * If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */
                println("Absolut path: " + Environment.getDataDirectory().absolutePath)
                println("Path" + Environment.getDataDirectory().path)
                val f = File(Environment.getDataDirectory().absolutePath +
                        "/${context.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg")
                val dirs = File(f.parent)

                dirs.takeIf { it.doesNotExist() }?.apply {
                    mkdirs()
                }
                f.createNewFile()
                val inputstream = client.getInputStream()
                val fileHandler = FileHandler()
                //fileHandler.copyFile(inputstream?.readBytes(), FileOutputStream(f))
                serverSocket.close()
                f.absolutePath
            }
        }

        private fun File.doesNotExist(): Boolean = !exists()

        override fun onPostExecute(result: String?) {
            //super.onPostExecute(result)
            /*if (result != null) {
                statusText.text = "File copied - $result"

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
            }*/

             */

            result?.run {
                statusText.text = "File copied - $result"
                val intent = Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("file://$result"), "image/*")
                }
                context.startActivity(intent)
            }
        }

        override fun onPreExecute() {
            //super.onPreExecute()
            statusText.text = "Opening a server socket"
        }

    }
}
