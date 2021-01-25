package unibas.dmi.sdatadirect.net.wifi.p2p

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.fasterxml.jackson.databind.ObjectMapper
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.PackageFactory
import unibas.dmi.sdatadirect.utils.PackageFactory.METHOD.*
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.collections.HashMap

class ConnectionManager(){
    companion object {
        val TAG: String = "ConnectionManager"
        val connections: HashMap<String?, Socket> = HashMap()
        val listeners: HashMap<String?, ConnectionListener> = HashMap()
        val socket: Socket = Socket()
        fun addConnection(wifiMacAddress: String?, sock: Socket){
            connections.put(wifiMacAddress, sock)
        }
        fun getSocket(wifiMacAddress: String?): Socket? {
            return connections.get(wifiMacAddress)
        }
        fun addListener(wifiMacAddress: String?, listener: ConnectionListener){
            listeners.put(wifiMacAddress, listener)
        }
        fun getListener(wifiMacAddress: String?): ConnectionListener? {
            return listeners.get(wifiMacAddress)
        }
    }
}