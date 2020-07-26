package unibas.dmi.sdatadirect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*

import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

import kotlinx.android.synthetic.main.activity_main.*
import kotlin.jvm.javaClass
import unibas.dmi.sdatadirect.bluetooth.BluetoothDriver
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerDao
import unibas.dmi.sdatadirect.net.wifi.p2p.FileTransferService

import unibas.dmi.sdatadirect.net.wifi.p2p.WifiP2pDriver
import unibas.dmi.sdatadirect.peer.PeerActivity
import unibas.dmi.sdatadirect.utils.QRCode

import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.NetworkInterface
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"
    private val charset = Charsets.UTF_8
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
    lateinit var qrCodeBtn: Button
    lateinit var scanQrBtn: Button
    lateinit var peersBtn: Button

    val STATE_LISTENING: Int = 1
    val STATE_CONNECTING: Int = 2
    val STATE_CONNECTED: Int = 3
    val STATE_CONNECTION_FAILED: Int = 4
    val STATE_MESSAGE_RECEIVED: Int = 5
    val MESSAGE_TOAST: Int = 6
    val MESSAGE_WRITE: Int = 7
    val QRCODE: Int = 8

    lateinit var db: AppDatabase
    lateinit var peerDao: PeerDao
    lateinit var cryptoHandler: CryptoHandler



    val handler: Handler = Handler(Handler.Callback {msg: Message ->

        when (msg.what) {

            STATE_LISTENING -> { textView.text = "Listening" }
            STATE_CONNECTING -> { textView.text = "Connecting" }
            STATE_CONNECTED -> { textView.text = "Connected" }
            STATE_CONNECTION_FAILED -> { textView.text = "Connection Failed" }
            STATE_MESSAGE_RECEIVED -> {
                val readBuff: ByteArray = msg.obj as ByteArray
                /*val tempMsg = String(readBuff, 0, msg.arg1)
                println("MESSAGE: $tempMsg")*/

                val verification = cryptoHandler.verifySignature(readBuff)

                if (verification) {
                    bluetoothDriver.stop()
                    wifiP2pDriver.discoverPeers()
                }
            }
            MESSAGE_TOAST -> {textView.text = "Couldn't send data"}
            QRCODE -> {
                val qrCode = QRCode(this)
                qrCode.showQrCode(msg.obj as Bitmap)
            }
        }

        return@Callback true

    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        listView = findViewById(R.id.peerView)
        discoverableSwitch = findViewById(R.id.discoverableSwitch)
        scanBtn = findViewById(R.id.scanBtn)
        stopConnectivityButton = findViewById(R.id.stopConnectivityBtn)
        chooseFileBtn = findViewById(R.id.chooseFileBtn)
        qrCodeBtn = findViewById(R.id.qrCodeButton)
        scanQrBtn = findViewById(R.id.scanQRBtn)
        peersBtn = findViewById(R.id.peersBtn)

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

        peersBtn.setOnClickListener {
            val intent = Intent(this, PeerActivity::class.java)
            startActivity(intent)
        }

        db = Room.databaseBuilder(
            this, AppDatabase::class.java, "Peers.db"
        ).build()
        peerDao = db.peersDao()

        cryptoHandler = CryptoHandler()

        bluetoothDriver = BluetoothDriver(this, handler)
        wifiP2pDriver = WifiP2pDriver(this)
        /*scanBtn.setOnClickListener {
            wifiP2pDriver.discoverPeers()
            qrCode.showQrCode(bitmap)
        }*/

        qrCodeBtn.setOnClickListener {
            val keyAES = cryptoHandler.keyAESGenerator()
            val keyRSA = cryptoHandler.keyPairRSAGenerator()
            cryptoHandler.sharedAESKey = keyAES
            cryptoHandler.publicRSAKey = keyRSA.public
            cryptoHandler.privateRSAKey = keyRSA.private

            val encodedKeyAES = cryptoHandler.getSecretKeyEncoded(cryptoHandler.sharedAESKey!!)
            val encodedKeyRSA = cryptoHandler.getPublicKeyEncoded(cryptoHandler.publicRSAKey!!)
            val qrCode = QRCode(this)

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


            val bitmap = qrCode.generateQRCode("$wifiMacAddress||$encodedKeyAES||$encodedKeyRSA")
            /*val message: Message = handler.obtainMessage(QRCODE, bitmap)
            message.sendToTarget()*/

            qrCode.showQrCode(bitmap)


            /*activity.peerDao.insertAll(
                Peer(
                    Random().nextInt(), socket.remoteDevice.name,
                socket.remoteDevice.address, "", encodedKey)
            )
            activity.wifiP2pDriver.wantsToBeClient = false
            activity.wifiP2pDriver.discoverPeers()*/
        }

        scanQrBtn.setOnClickListener {
            val scanner = IntentIntegrator(this)
            scanner.initiateScan()
        }



        discoverableSwitch.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) {
                buttonView.text = "ON"
                bluetoothDriver.discoverable(true)
                bluetoothDriver.start()
            } else {
                buttonView.text = "OFF"
                bluetoothDriver.acceptThread?.cancel()
                bluetoothDriver.discoverable(false)
            }
        }

        /*scanBtn.setOnClickListener {
            //bluetoothDriver.start()
            bluetoothDriver.startDiscover()
        }*/

        scanBtn.setOnClickListener {
            //bluetoothDriver.start()
            wifiP2pDriver.discoverPeers()
        }

        stopConnectivityButton.setOnClickListener {
            bluetoothDriver.stop()
            wifiP2pDriver.stop()
        }
        /*listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            bluetoothDriver.bluetoothAdapter?.cancelDiscovery()
            Log.d(TAG, "You clicked on a device")
            val deviceName: String? = bluetoothDriver.devices[i]?.name
            val deviceAddress: String? = bluetoothDriver.devices[i]?.address
            Log.d(TAG, "You clicked on device: $deviceName, $deviceAddress")
            bluetoothDriver.connect(bluetoothDriver.devices[i])
            val message: Message = Message.obtain()
            message.what = STATE_CONNECTING
            handler.sendMessage(message)
        }*/

        wifiP2pDriver.listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            wifiP2pDriver.connect("", wifiP2pDriver.peers[i])
        }

        chooseFileBtn.setOnClickListener {
            val target = Intent(Intent.ACTION_GET_CONTENT)
            target.type = "*/*"
            //val intent = Intent.createChooser(target, "Choose a file")
            startActivityForResult(target, CHOOSE_FILE_RESULT_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /*val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }*/

        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                textView.text = "Sending"
                Log.d(TAG, "Intent------------------ $uri")
                val serviceIntent = Intent(this, FileTransferService::class.java).apply {
                    action = FileTransferService.ACTION_SEND_FILE
                    putExtra(FileTransferService.EXTRAS_FILE_PATH, uri?.toString())
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNDER_ADDRESS,
                        wifiP2pDriver.groupOwnerAddress)
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8888)
                }
                startService(serviceIntent)
            }
        } else {
            val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            bluetoothDriver.scannedContent = result.contents
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