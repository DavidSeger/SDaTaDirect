package unibas.dmi.sdatadirect

import androidx.appcompat.app.AppCompatActivity

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri

import android.net.wifi.p2p.*
import android.os.*

import android.util.Log
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.greenrobot.eventbus.EventBus

import unibas.dmi.sdatadirect.bluetooth.BluetoothDriver
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerDao
import unibas.dmi.sdatadirect.net.wifi.p2p.FileTransferService

import unibas.dmi.sdatadirect.net.wifi.p2p.WifiP2pDriver
import unibas.dmi.sdatadirect.peer.PeerActivity
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.QRCode

import java.io.IOException
import java.lang.Exception
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"
    val CHOOSE_FILE_RESULT_CODE: Int = 20

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
    lateinit var chooseFileBtn: FloatingActionButton
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
                qrCode.showQrCode(msg.obj as Bitmap)
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
        chooseFileBtn = findViewById(R.id.chooseFileBtn)
        qrCodeBtn = findViewById(R.id.qrCodeButton)
        scanQrBtn = findViewById(R.id.scanQRBtn)
        peersBtn = findViewById(R.id.peersBtn)
        listenBtn = findViewById(R.id.listenBtn)

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

        peersBtn.setOnClickListener {
            val intent = Intent(this, PeerActivity::class.java)
            startActivity(intent)
        }

        peerViewModel = ViewModelProvider(this).get(PeerViewModel::class.java)

        cryptoHandler = CryptoHandler()
        qrCode = QRCode(this)

        bluetoothDriver = BluetoothDriver(this, handler, qrCode, cryptoHandler, peerViewModel)
        wifiP2pDriver = WifiP2pDriver(this, cryptoHandler, peerViewModel)

        // PHASE 1
        qrCodeBtn.setOnClickListener {
            val keyAES = cryptoHandler.keyAESGenerator()
            val keyRSA = cryptoHandler.keyPairRSAGenerator()
            cryptoHandler.sharedAESKey = keyAES
            cryptoHandler.publicRSAKey = keyRSA.public
            cryptoHandler.privateRSAKey = keyRSA.private

            val encodedKeyAES = cryptoHandler.getSecretKeyEncoded(cryptoHandler.sharedAESKey!!)
            val encodedPublicKeyRSA = cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!)

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

            qrCode.showQrCode(bitmap)
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
                            public_key = cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!),
                            private_key = cryptoHandler.getPrivateKeyEncoded(cryptoHandler.privateRSAKey!!),
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

        chooseFileBtn.setOnClickListener {
            val target = Intent(Intent.ACTION_GET_CONTENT)
            target.type = "*/*"
            startActivityForResult(target, CHOOSE_FILE_RESULT_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                textView.text = "Sending"
                Log.d(TAG, "Intent------------------ $uri")
                val serviceIntent = Intent(this, FileTransferService::class.java).apply {
                    action = FileTransferService.ACTION_SEND_FILE
                    putExtra(FileTransferService.EXTRAS_FILE_PATH, uri?.toString())
                    putExtra(FileTransferService.EXTRAS_DESTINATION_ADDRESS, wifiP2pDriver.deviceWantsToConnectTo)
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        wifiP2pDriver.groupOwnerAddress)
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8888)
                    EventBus.getDefault().postSticky(peerViewModel)
                    EventBus.getDefault().postSticky(cryptoHandler)
                }
                startService(serviceIntent)
            }
        }  else {
            val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            qrCode.scannedContent = result.contents
        }
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
