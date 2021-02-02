package unibas.dmi.sdatadirect

import androidx.appcompat.app.AppCompatActivity

import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap

import android.net.wifi.p2p.*
import android.os.*
import android.provider.Settings
import android.util.EventLog

import android.util.Log
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.greenrobot.eventbus.EventBus

import unibas.dmi.sdatadirect.bluetooth.BluetoothDriver
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.content.PeerInfoViewModel
import unibas.dmi.sdatadirect.content.SelfViewModel
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.Self
import unibas.dmi.sdatadirect.net.wifi.p2p.ConnectionManager

import unibas.dmi.sdatadirect.net.wifi.p2p.WifiP2pDriver
import unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils.SetSynchronization
import unibas.dmi.sdatadirect.peer.PeerActivity
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.QRCode

import java.lang.Exception
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"

    lateinit var intentFilter: IntentFilter
    lateinit var wifiP2pDriver: WifiP2pDriver
    lateinit var bluetoothDriver: BluetoothDriver
    lateinit var device: BluetoothDevice

    // UI components
    lateinit var listView: ListView

    lateinit var discoverableSwitch: Switch
    lateinit var scanBtn: FloatingActionButton
    lateinit var stopConnectivityButton: FloatingActionButton
    lateinit var textView: TextView
    lateinit var viewFeedsBtn: FloatingActionButton
    lateinit var qrCodeBtn: ImageButton
    lateinit var scanQrBtn: ImageButton
    lateinit var peersBtn: FloatingActionButton
    lateinit var listenBtn: FloatingActionButton

    val STATE_LISTENING: Int = 1
    val STATE_CONNECTING: Int = 2
    val STATE_CONNECTED: Int = 3
    val STATE_CONNECTION_FAILED: Int = 4
    val MESSAGE_TOAST: Int = 5
    val QRCODE: Int = 6
    val PEER_SAVED: Int = 7
    val VERIFICATION_SUCCESSFUL: Int = 8
    val VERIFICATION_FAILED: Int = 9
    val CONNECTION_CLOSED: Int = 10

    lateinit var cryptoHandler: CryptoHandler
    lateinit var qrCode: QRCode
    private lateinit var peerViewModel: PeerViewModel
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var feedViewModel: FeedViewModel
    private lateinit var peerInfoViewModel: PeerInfoViewModel
    private lateinit var selfViewModel: SelfViewModel



    var bluetoothActive = false
    var wifiDirectActive = false



    val handler: Handler = Handler(Handler.Callback {msg: Message ->

        when (msg.what) {

            STATE_LISTENING -> { textView.text = "Listening" }
            STATE_CONNECTING -> { textView.text = "Connecting" }
            STATE_CONNECTED -> { textView.text = "Connected" }
            STATE_CONNECTION_FAILED -> { textView.text = "Connection Failed" }
            MESSAGE_TOAST -> {textView.text = "Couldn't send data"}
            QRCODE -> {
                val qrCode = QRCode(this)
                qrCode.showQRCode(msg.obj as Bitmap)
            }

            PEER_SAVED -> { textView.text = "Peer saved"}

            VERIFICATION_SUCCESSFUL -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Verification Successful")
                builder.setMessage(
                    "The verification has been successful, click OK to start the WiFi-Connection"
                )
                builder.setPositiveButton("OK") { dialog, which ->
                    bluetoothActive = false
                    wifiDirectActive = true
                    bluetoothDriver.stop()
                    wifiP2pDriver.discoverPeers()
                    dialog.dismiss()
                }

                val dialog: AlertDialog = builder.create()

                dialog.show()
            }

            VERIFICATION_FAILED -> {

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Verification failed")
                builder.setMessage(
                    "The verification has been failed! Tab on close to close the " +
                            "connection"
                )
                builder.setPositiveButton("Close") { dialog, which ->
                    finish()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

            CONNECTION_CLOSED -> {textView.text = "Connection closed"}
        }

        return@Callback true

    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        textView = findViewById(R.id.textView)
        listView = findViewById(R.id.peerView)
        discoverableSwitch = findViewById(R.id.discoverableSwitch)
        scanBtn = findViewById(R.id.scanBtn)
        stopConnectivityButton = findViewById(R.id.stopConnectivityBtn)
        viewFeedsBtn = findViewById(R.id.viewFeedsBtn)
        qrCodeBtn = findViewById(R.id.qrCodeButton)
        scanQrBtn = findViewById(R.id.scanQRBtn)
        peersBtn = findViewById(R.id.peersBtn)
        listenBtn = findViewById(R.id.listenBtn)

        messageViewModel = ViewModelProvider(this).get(MessageViewModel::class.java)
        feedViewModel = ViewModelProvider(this).get(FeedViewModel::class.java)
        messageViewModel.feeds = feedViewModel
        peerInfoViewModel = ViewModelProvider(this).get(PeerInfoViewModel::class.java)
        selfViewModel = ViewModelProvider(this).get(SelfViewModel::class.java)

        intentFilter = IntentFilter()
        intentFilter.apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            // Indicates a change in the Wi-Fi P2P status.
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            // Indicates a change in the list of available peers.
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            // Indicates the state of Wi-Fi P2P connectivity has changed.
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            // Indicates this device's details have changed.
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        peerViewModel = ViewModelProvider(this).get(PeerViewModel::class.java)
        ConnectionManager.peers = peerViewModel
        SetSynchronization.setup(peerViewModel, peerInfoViewModel, feedViewModel)

        peersBtn.setOnClickListener {
            val intent = Intent(this, PeerActivity::class.java)
            startActivity(intent)
        }

        peerViewModel = ViewModelProvider(this).get(PeerViewModel::class.java)

        cryptoHandler = CryptoHandler()
        ConnectionManager.cryptoHandler = cryptoHandler

        if(selfViewModel.getSelf() == null){
            var kPair = cryptoHandler.keyPairRSAGenerator()
            val self = Self(
                name = "self",
                privKey = cryptoHandler.getPrivateKeyEncoded(kPair.private),
                pubKey = cryptoHandler.getPublicKeyEncoded(kPair.public)
            )
            selfViewModel.insert(self)
        }

        if(feedViewModel.getFeedByOwner(selfViewModel.getSelf().pubKey!!) == null){
            var selfFeed = Feed(
                key = Settings.Secure.getString(contentResolver, "bluetooth_name"),
                type = "priv",
                host = "UNIBAS.SDATA.TEST",
                port = "8888",
                subscribed = true,
                owner = selfViewModel.getSelf().pubKey
            )
            feedViewModel.insert(selfFeed)
        }
        qrCode = QRCode(this)

        bluetoothDriver = BluetoothDriver(this, handler, qrCode, cryptoHandler, peerViewModel)
        wifiP2pDriver = WifiP2pDriver(this, cryptoHandler, peerViewModel)

        // PHASE 1
        qrCodeBtn.setOnClickListener {
            val keyAES = cryptoHandler.keyAESGenerator()
            cryptoHandler.secretAESKey = keyAES
            cryptoHandler.publicRSAKey = cryptoHandler.getPublicKeyDecoded(selfViewModel.getSelf().pubKey!!)
            cryptoHandler.privateRSAKey = cryptoHandler.getPrivateKeyDecoded(selfViewModel.getSelf().privKey!!)

            val encodedKeyAES = cryptoHandler.getSecretKeyEncoded(cryptoHandler.secretAESKey!!)
            val encodedPublicKeyRSA = selfViewModel.getSelf().pubKey

            var wifiMacAddress: String? = null
            try {
                wifiMacAddress = NetworkInterface.getNetworkInterfaces()
                    .toList()
                    .find { networkInterface ->
                        networkInterface.name.equals(
                            "wlan0",
                            ignoreCase = true
                        )
                    }
                    ?.hardwareAddress
                    ?.joinToString(separator = ":") { byte -> "%02X".format(byte) }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }


            val bitmap = qrCode.generateQRCode("$wifiMacAddress#$encodedKeyAES#$encodedPublicKeyRSA")

            qrCode.showQRCode(bitmap)
        }

        scanQrBtn.setOnClickListener {
            qrCode.scanQRCode()
        }

        // PHASE 2

        listenBtn.setOnClickListener {
            bluetoothDriver.startServer()
            val message: Message = Message.obtain()
            message.what = STATE_LISTENING
            handler.sendMessage(message)
        }

        discoverableSwitch.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) {
                buttonView.text = "ON"
                bluetoothDriver.discoverable(true)
                bluetoothActive = true
            } else {
                buttonView.text = "OFF"
                bluetoothDriver.discoverable(false)
                bluetoothActive = false
            }
        }

        scanBtn.setOnClickListener {
            if (!wifiDirectActive && !bluetoothActive) {
                bluetoothDriver.startDiscovery()
                bluetoothActive = true
            } else if (wifiDirectActive && !bluetoothActive) {
                wifiP2pDriver.discoverPeers()
            }
        }


        stopConnectivityButton.setOnClickListener {
            bluetoothDriver.stop()
            wifiP2pDriver.stop()
        }

        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->

            if (bluetoothActive) {
                Log.d(TAG, "You clicked on a device")
                val deviceName: String? = bluetoothDriver.devices[i]?.name
                val deviceAddress: String? = bluetoothDriver.devices[i]?.address
                Log.d(TAG, "You clicked on device: $deviceName, $deviceAddress")
                val device = bluetoothDriver.devices[i]
                val peer = peerViewModel.getPeerByBluetoothAddress(device?.address!!)

                var saved = false

                if (peer == null) {

                    if (qrCode.scannedContent == null) {
                        Toast.makeText(
                            this,
                            "You have to scan the QRCode first!",
                            Toast.LENGTH_SHORT
                        )
                        saved = false
                    } else {
                        val dialog = Dialog(this)
                        dialog.setContentView(R.layout.peeradd_dialog)
                        dialog.setTitle("You want to add this peer to your database?")
                        val linearLayout = dialog.findViewById<LinearLayout>(R.id.linearLayout)
                        val saveBtn: Button = dialog.findViewById(R.id.saveBtn)
                        val cancelBtn: Button = dialog.findViewById(R.id.button3)
                        val newPeer = Peer(
                            name = device.name,
                            bluetooth_mac_address = device.address,
                            wifi_mac_address = qrCode.scannedContent?.split("#")?.get(0),
                            shared_key = qrCode.scannedContent?.split("#")?.get(1),
                            public_key = selfViewModel.getSelf().pubKey,                                               //cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!),
                            private_key = selfViewModel.getSelf().privKey,                                                           //cryptoHandler.getPrivateKeyEncoded(cryptoHandler.privateRSAKey!!),
                            foreign_public_key = qrCode.scannedContent?.split("#")?.get(2)
                        )

                        val name= TextView(dialog.context)
                        val bluetoothAddress = TextView(dialog.context)
                        val wifiAddress = TextView(dialog.context)

                        name.text = "Devince name: ${newPeer.name}"
                        bluetoothAddress.text = "Bluetooth: ${newPeer.bluetooth_mac_address}"
                        wifiAddress.text = "WiFi: ${newPeer.wifi_mac_address}"

                        name.textSize = 18f
                        bluetoothAddress.textSize = 18f
                        wifiAddress.textSize = 18f

                        linearLayout.addView(name)
                        linearLayout.addView(bluetoothAddress)
                        linearLayout.addView(wifiAddress)


                        saveBtn.setOnClickListener {
                            if (newPeer.wifi_mac_address == null || newPeer.shared_key == null || newPeer.public_key == null) {
                                Toast.makeText(
                                    dialog.context,
                                    "You have to scan the QRCode first!",
                                    Toast.LENGTH_SHORT
                                )
                                saved = false
                                dialog.cancel()
                            } else {
                                peerViewModel.insert(newPeer)
                                saved = true

                                bluetoothDriver.connect(bluetoothDriver.devices[i])
                                val message: Message = Message.obtain()
                                message.what = STATE_CONNECTING
                                handler.sendMessage(message)

                                dialog.cancel()
                            }
                        }

                        cancelBtn.setOnClickListener {
                            saved = false

                            val builder = AlertDialog.Builder(this@MainActivity)
                            builder.setTitle("Refused peer to save")
                            builder.setMessage(
                                "Are you sure to not save the device? Otherwise, the connection will be " +
                                        "aborted"
                            )
                            builder.setPositiveButton("YES") { dialog, which ->
                                finish()
                            }

                            val alertDialog: AlertDialog = builder.create()
                            alertDialog.show()

                            dialog.cancel()
                        }

                        dialog.show()
                    }

                } else {
                    bluetoothDriver.connect(device)
                }
            } else if (wifiDirectActive) {
                wifiP2pDriver.connect("", wifiP2pDriver.peers[i])
            }
        }

        viewFeedsBtn.setOnClickListener {
            startChat()
        }
    }

    /**
     * start connections, start chat
     */
    fun startChat(){
      /*  val chatIntent = Intent(this, ChatActivity::class.java).apply {
            action = ChatActivity.ACTION_SEND_CHAT
            putExtra(ChatActivity.EXTRAS_DESTINATION_ADDRESS, wifiP2pDriver.targetDeviceAddress)
            putExtra(ChatActivity.EXTRAS_HOST_ADDRESS,
                wifiP2pDriver.groupOwnerAddress)
            putExtra(ChatActivity.EXTRAS_HOST_PORT, 8888)
            EventBus.getDefault().postSticky(peerViewModel)
            EventBus.getDefault().postSticky(cryptoHandler)
        }*/
       // startActivity(chatIntent)
        val feedIntent = Intent(this, Feed_overview_activity::class.java).apply {
           EventBus.getDefault().postSticky(feedViewModel)
            EventBus.getDefault().postSticky(messageViewModel)
            EventBus.getDefault().postSticky(cryptoHandler)
            EventBus.getDefault().postSticky(selfViewModel)
        }
        startActivity(feedIntent)

    }

    /**
     * currently only catching results of QR code and setting that
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            qrCode.scannedContent = result.contents

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiP2pDriver.receiver, intentFilter)
        registerReceiver(bluetoothDriver.receiver, intentFilter)
        //registerReceiver(bleDriver.receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiP2pDriver.receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothDriver.receiver)
        for (device: BluetoothDevice in bluetoothDriver.bluetoothAdapter?.bondedDevices!!) {
            bluetoothDriver.removePairs(device)
        }
        bluetoothDriver.stop()
        unregisterReceiver(wifiP2pDriver.receiver)
        wifiP2pDriver.stop()

    }
}