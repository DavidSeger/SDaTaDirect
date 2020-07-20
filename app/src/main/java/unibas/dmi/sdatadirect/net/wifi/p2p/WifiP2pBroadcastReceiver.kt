package unibas.dmi.sdatadirect.net.wifi.p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_WIFI_P2P
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.R

class WifiP2pBroadcastReceiver(val driver: WifiP2pDriver,
                                  val manager: WifiP2pManager,
                                  val channel: WifiP2pManager.Channel,
                                  val activity: MainActivity): BroadcastReceiver() {

    private val TAG: String = "WIfiP2pBroadCastReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.

                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)

                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    driver.isWifiP2pEnabled = true
                    Toast.makeText(context, "Wifi is on!", Toast.LENGTH_SHORT).show()
                    Log.d("TAG", "Wifi is on")
                } else {
                    driver.isWifiP2pEnabled = false
                    Toast.makeText(context, "Wifi is off!", Toast.LENGTH_SHORT).show()
                    Log.d("TAG", "Wifi is off")
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                // The peer list has changed! We should probably do something about
                // that.
                driver.checkPermission()
                manager.requestPeers(channel, driver.peerListListener)
                //activity.wifiP2pDriver.updatePeerList()
                Log.d("TAG", "P2P peers changed")

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                // Connection state changed! We should probably do something about
                // that.
                manager.let { manager ->

                    val networkInfo: NetworkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo

                    if (networkInfo.isConnected) {
                        println("YAAAAAAAAY")

                        // We are connected with the other device, request connection
                        // info to find group owner IP

                        manager.requestConnectionInfo(channel, driver.connectionInfoListener)
                    } else {
                        activity.textView.text = "Device Disconnected"
                        Log.d("TAG", "Device disconnected")
                    }
                }

                /*manager.let { manager ->
                    val cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val n: Network? = cm.activeNetwork
                    if (cm.getNetworkCapabilities(n).hasCapability(NET_CAPABILITY_WIFI_P2P)) {
                        println("YAAAAAAAAY")
                        manager.requestConnectionInfo(channel, driver.connectionListener)
                    } else {
                        Log.d("PeerDisconnected", "Device disconnected")
                    }
                }*/


            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {

                //THIS device details changed

            }
        }
    }
}