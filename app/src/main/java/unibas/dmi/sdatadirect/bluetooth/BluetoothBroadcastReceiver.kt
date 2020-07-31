package unibas.dmi.sdatadirect.bluetooth

import unibas.dmi.sdatadirect.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.ui.BluetoothDeviceListAdapter

/**
 * Received all Bluetooth related changes
 */
class BluetoothBroadcastReceiver(val bluetoothDriver: BluetoothDriver, val activity: MainActivity): BroadcastReceiver() {
    private val TAG: String = "BluetoothBroadcast"

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "STATE OFF")
                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d(TAG, "STATE TURNING OFF")
                    }

                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "STATE ON")
                    }

                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Log.d(TAG, "STATE TURNING ON")
                    }
                }
            }

            BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                val mode: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)

                when(mode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                        Log.d(TAG, "Discoverability Enabled")
                    }

                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> {
                        Log.d(TAG, "Discoverability Disabled. Able to receive connections")
                    }

                    BluetoothAdapter.SCAN_MODE_NONE -> {
                        Log.d(TAG, "Discoverability Disabled. Not able to receive connections")
                    }

                    BluetoothAdapter.STATE_CONNECTING -> {
                        Log.d(TAG, "Connecting ...")
                    }

                    BluetoothAdapter.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected.")
                    }
                }
            }

            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                println( "Device found: ${device?.name}:${device?.address}")
                if (!bluetoothDriver.devices.contains(device)) {
                    bluetoothDriver.devices.add(device)
                    bluetoothDriver.bluetoothDeviceListAdapter = BluetoothDeviceListAdapter(
                        context,
                        R.layout.device_adapter_view,
                        bluetoothDriver.devices
                    )
                    activity.listView.adapter = bluetoothDriver.bluetoothDeviceListAdapter
                }
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                when(device?.bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Log.d(TAG, "BOND_BOONDED")
                        activity.device = device
                    }

                    BluetoothDevice.BOND_BONDING -> {
                        Log.d(TAG, "BOND_BONDING")
                    }

                    BluetoothDevice.BOND_NONE -> {
                        Log.d(TAG, "BOND_NONE")
                    }
                }
            }


        }
    }
}