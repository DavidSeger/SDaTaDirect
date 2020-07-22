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

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*

import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

import kotlinx.android.synthetic.main.activity_main.*
import kotlin.jvm.javaClass
import unibas.dmi.sdatadirect.bluetooth.BluetoothDriver
import unibas.dmi.sdatadirect.net.wifi.p2p.FileTransferService

import unibas.dmi.sdatadirect.net.wifi.p2p.WifiP2pDriver

import java.io.File
import java.io.IOException
import java.nio.charset.Charset

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
    lateinit var scanQRCodeBtn: Button
    lateinit var chooseFileBtn: FloatingActionButton

    val STATE_LISTENING: Int = 1
    val STATE_CONNECTING: Int = 2
    val STATE_CONNECTED: Int = 3
    val STATE_CONNECTION_FAILED: Int = 4
    val STATE_MESSAGE_RECEIVED: Int = 5
    val MESSAGE_TOAST: Int = 6
    val MESSAGE_WRITE: Int = 7

    lateinit var serviceIntent: Intent

    val handler: Handler = Handler(Handler.Callback {msg: Message ->

        when (msg.what) {

            STATE_LISTENING -> { textView.text = "Listening" }
            STATE_CONNECTING -> { textView.text = "Connecting" }
            STATE_CONNECTED -> { textView.text = "Connected" }
            STATE_CONNECTION_FAILED -> { textView.text = "Connection Failed" }
            STATE_MESSAGE_RECEIVED -> {
                val readBuff: ByteArray = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                println("MESSAGE: $tempMsg")
            }
            MESSAGE_TOAST -> {textView.text = "Couldn't send data"}
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
        scanQRCodeBtn = findViewById(R.id.scanQRCodeBtn)
        chooseFileBtn = findViewById(R.id.chooseFileBtn)

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

        wifiP2pDriver = WifiP2pDriver(this)
        scanBtn.setOnClickListener { wifiP2pDriver.discoverPeers() }

        wifiP2pDriver.listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->

            wifiP2pDriver.connect(wifiP2pDriver.peers[i])
        }

        serviceIntent = Intent()

        chooseFileBtn.setOnClickListener {
            val target = Intent(Intent.ACTION_GET_CONTENT)
            target.type = "*/*"
            //val intent = Intent.createChooser(target, "Choose a file")
            startActivityForResult(target, CHOOSE_FILE_RESULT_CODE)
        }

        /*qrCode = QRCode(this)
        qrCodeDialogView = QRCodeDialog(this)
        qrCode.createQrCode(qrCodeDialogView)
        scanQRCodeBtn.setOnClickListener { qrCode.scanQrCode() }
        bluetoothDriver = BluetoothDriver(this, handler)
        discoverableSwitch.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) {
                buttonView.text = "ON"
                qrCodeDialogView.createDialog()
                bluetoothDriver.discoverable(true)
                bluetoothDriver.start()
            } else {
                buttonView.text = "OFF"
                bluetoothDriver.acceptThread?.cancel()
                bluetoothDriver.discoverable(false)
            }
        }
        scanBtn.setOnClickListener {
            bluetoothDriver.start()
            bluetoothDriver.startDiscover()
        }
        stopConnectivityButton.setOnClickListener {
            bluetoothDriver.stop()
        }
        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
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
            val uri = data?.data
            textView.text = "Sending"
            Log.d(TAG, "Intent------------------ $uri")
            /*serviceIntent = Intent(this, wifiP2pDriver.client::class.java).apply {
            action = FileTransferService.ACTION_SEND_FILE
            putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())
            putExtra(FileTransferService.EXTRAS_GROUP_OWNDER_ADDRESS,
                wifiP2pDriver.groupOwnerAddress)
            putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8888)
        }
        Log.d(TAG, "Hallo 1")
        Log.d(TAG, wifiP2pDriver.groupOwnerAddress)
        startService(serviceIntent)
        Log.d(TAG, "Hallo 2")*/
            wifiP2pDriver.fileUri = uri.toString()
        }


        /*if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                textView.text = "Sending"
                Log.d(TAG, "Intent------------------ $uri")
                val serviceIntent = Intent(this, FileTransferService::class.java).apply {
                    action = FileTransferService.ACTION_SEND_FILE
                    putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNDER_ADDRESS,
                        wifiP2pDriver.groupOwnerAddress)
                    putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8888)
                }
                startService(serviceIntent)
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiP2pDriver.receiver, intentFilter)
        //registerReceiver(bluetoothDriver.receiver, intentFilter)
        //registerReceiver(bleDriver.receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiP2pDriver.receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        /*unregisterReceiver(bluetoothDriver.receiver)
        for (device: BluetoothDevice in bluetoothDriver.bluetoothAdapter?.bondedDevices!!) {
            bluetoothDriver.removePairs(device)
        }
        bluetoothDriver.stop()*/
        unregisterReceiver(wifiP2pDriver.receiver)
        wifiP2pDriver.stopWifiP2pDriver()
    }
}