package unibas.dmi.sdatadirect.net.wifi.p2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
    val deviceNameArray: MutableList<String> = mutableListOf()
    val deviceArray: MutableList<WifiP2pDevice> = mutableListOf()

    val listView: ListView = activity.findViewById(R.id.peerView)

    val adapter: ArrayAdapter<WifiP2pDevice> = ArrayAdapter(
        activity,
        android.R.layout.simple_list_item_1, peers
    )

    var wantsToBeClient = false
    lateinit var deviceWantsToConnectTo: String


    var isServer = false
    var isClient = false

    var groupOwnerAddress: String = ""

    var wifiP2pDeviceListAdapter: WifiP2pDeviceListAdapter =
        WifiP2pDeviceListAdapter(activity, R.layout.device_adapter_view, peers)

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

    fun discoverPeers() {

        checkPermission()

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
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
    fun connect(address: String = "", target: WifiP2pDevice? = null) {

        checkPermission()

        val config = WifiP2pConfig().apply {
            if (address == "")
                deviceAddress = target?.deviceAddress
            else
                deviceAddress = address
        }

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                /*Toast.makeText(
                    activity, "Connected successfully to ${target?.deviceName}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(
                    TAG,
                    "Connection to [${target.deviceName}, ${target.deviceAddress}] was successful"
                )*/
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
            //(activity.listView.adapter as WifiP2pDeviceListAdapter).notifyDataSetChanged()
            //wifiP2pDeviceListAdapter.notifyDataSetChanged()
        }

        if (peers.isEmpty()) {
            Toast.makeText(activity, "No Device Found", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "No devices found")
            activity.listView.adapter = wifiP2pDeviceListAdapter
            //(activity.listView.adapter as WifiP2pDeviceListAdapter).notifyDataSetChanged()
            //wifiP2pDeviceListAdapter.notifyDataSetChanged()
            return@PeerListListener
        }

    }

    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->

        groupOwnerAddress = info.groupOwnerAddress.hostAddress

        if (info.groupFormed && info.isGroupOwner) {
            activity.textView.text = "Host!"
            FileServerAsyncTask(activity).execute()
            isServer = true
        } else if (info.groupFormed) {
            activity.textView.text = "Client!"
            isClient = true
        }
    }

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
