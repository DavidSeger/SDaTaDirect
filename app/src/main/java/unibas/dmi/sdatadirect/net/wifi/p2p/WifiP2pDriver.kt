package unibas.dmi.sdatadirect.net.wifi.p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.view.View
import android.widget.*
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.ui.WifiP2pDeviceListAdapter
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Driver class for handling all relevant functionalities to enable WiFi-Direct traffic.
 */
class WifiP2pDriver (
    val activity: MainActivity,
    val cryptoHandler: CryptoHandler,
    val peerViewModel: PeerViewModel) {

    val TAG: String = "WifiP2pDriver"

    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null

    init {
        channel = manager?.initialize(activity, activity.mainLooper, null)
        channel?.also { channel ->
            receiver = manager?.let { WifiP2pBroadcastReceiver(this, it, channel, activity) }
        }
    }

    var isWifiP2pEnabled: Boolean = true

    val peers: MutableList<WifiP2pDevice> = mutableListOf()

    var wantsToBeClient = false
    lateinit var targetDeviceAddress: String
    var clientAddress: String = ""

    var isServer = false
    var isClient = false

    var groupOwnerAddress: String = ""

    /**
     * DiscoverPeers detects available peers that are in range. If the discovery was successful,
     * the system broadcasts the WIFI_P2P_PEERS_CHANGED_ACTION intent which will be used to save
     * the list of peers.
     */

    fun checkPermission() {
        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_WIFI_STATE), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.CHANGE_WIFI_STATE), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.CHANGE_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.CHANGE_NETWORK_STATE),
                1
            )
        }

        if (activity.checkSelfPermission(android.Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(android.Manifest.permission.INTERNET), 1)
        }

        if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_NETWORK_STATE),
                1
            )
        }


    }

    /**
     * Discovers available devices in the surrounding with WiFi-Direct
     */
    fun discoverPeers() {

        checkPermission()

        activity.listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            connect("", peers[i])
        }

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //...
                activity.textView.text = "Discovery Started"
                Log.d(TAG, "Discovery started")
            }

            override fun onFailure(reasonCode: Int) {
                //...
                activity.textView.text = "Discovery Starting failed"
                Log.d(TAG, "Discovery starting failed")
            }
        })

    }

    /**
     * Connect method establishes a connection to a desired device. Within the method, a WifiP2pConfig
     * will be specified containing the information of the device to connect to. The WifiP2pManager.ActionListener
     * notifies you of a connection success or failure.
     */
    fun connect(address: String, target: WifiP2pDevice?) {

        checkPermission()

        val config = WifiP2pConfig().apply {
            if (address == "")
                deviceAddress = target?.deviceAddress
            else
                deviceAddress = address
        }

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(
                    activity, "Connected successfully to ${target?.deviceName}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(
                    TAG,
                    "Connection to [${target?.deviceName}, ${target?.deviceAddress}] was successful"
                )
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(activity, "Connect failed. Retry.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Connection failed")
            }

        })
    }


    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        if (peerList.deviceList != peers) {
            peers.clear()
            peers.addAll(peerList.deviceList)

            if (peerList.deviceList.isEmpty()) {
                Toast.makeText(activity, "No Device Found", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "No devices found")
            }
        }

        val wifiP2pDeviceListAdapter = WifiP2pDeviceListAdapter(
            activity, R.layout.device_adapter_view, peers)

        activity.listView.adapter = wifiP2pDeviceListAdapter
    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->

        groupOwnerAddress = info.groupOwnerAddress.hostAddress

        if (info.groupFormed && info.isGroupOwner) {
            activity.textView.text = "Host!"
            ConnectionListener(activity, activity, peerViewModel, cryptoHandler, clientAddress).execute()
            isServer = true
        } else if (info.groupFormed) {
            activity.textView.text = "Client!"
            isClient = true
            establishConnection(clientAddress)
            ConnectionListener(activity, activity, peerViewModel, cryptoHandler, clientAddress).execute()
        }
    }

    private fun establishConnection(clientAddress: String) {
        val host: String? = groupOwnerAddress
        val port: Int? = 8888
        val sock: Socket = Socket()
        try {
            Log.d(TAG, "Opening client socket -")
            sock.bind(null)
            sock.connect(InetSocketAddress(host, port!!), 5000)
            if(ConnectionManager.getSocket(clientAddress) == null){
                    ConnectionManager.addConnection(clientAddress, sock)
                }
            Log.d(FileTransferService.TAG, "Client socket - ${ConnectionManager.getSocket(clientAddress)!!.isConnected}")
        } catch (e: IOException) {
            Log.e(FileTransferService.TAG, e.message)
        }
    }


    // TODO: Create a group to integrate also devices without any WiFi support
    fun createGroup() {
        checkPermission()
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                println("Connected successfully!")
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(activity, "Group creation failed. Retry.", Toast.LENGTH_SHORT).show()
            }

        })
    }

    // TODO
    fun stop() {
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })

        manager?.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })

        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(p0: Int) {
                TODO("Not yet implemented")
            }

        })
    }
}
