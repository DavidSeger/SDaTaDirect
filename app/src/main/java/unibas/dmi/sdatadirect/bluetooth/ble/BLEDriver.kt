package unibas.dmi.sdatadirect.bluetooth.ble

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.Handler
import android.widget.ArrayAdapter
import unibas.dmi.sdatadirect.MainActivity

class BLEDriver(val activity: MainActivity) {

    private val TAG: String = "BLEDriver"

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    //val receiver: BluetoothBroadcastReceiver = BluetoothBroadcastReceiver(this, activity)

    val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            println("Device found: ${result?.scanRecord?.deviceName}, ${result?.device?.address}")
            if(!devices.contains(result?.device)) {
                adapter.add(result?.device)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private val handler: Handler = Handler()
    var scanning: Boolean = false
    private val DISCOVER_PERIOD: Long = 10000

    val pairedDevices: Set<BluetoothDevice?>? = bluetoothAdapter?.bondedDevices
    val devices: MutableList<BluetoothDevice?> = mutableListOf()
    val adapter: ArrayAdapter<BluetoothDevice?> = ArrayAdapter(
        activity.applicationContext,
        android.R.layout.simple_list_item_1,
        devices
    )

    var REQUEST_ENABLE_BT = 1



    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    init {
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

    }

    //TODO: Action missing to trigger app closing
    fun noBluetoothSupportAlert() {
        val builder: AlertDialog.Builder? = activity.let {
            AlertDialog.Builder(it)
        }

        builder?.setMessage("Your device does not support bluetooth!")
            ?.setTitle("Alert: Bluetooth")

        val dialog: AlertDialog? = builder?.create()
    }

    fun queryPairedDevices() {
        pairedDevices?.forEach {device ->
            val deviceName = device?.name
            val deviceHardwareAddress = device?.address // MAC address
        }
    }

    fun scanLeDevice(enable: Boolean) {
        when(enable) {
            true -> {
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner?.stopScan(leScanCallback)

                }, DISCOVER_PERIOD)
                scanning = true
                bluetoothLeScanner?.startScan(leScanCallback)
            }
            else -> {
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }
        }
    }
}
