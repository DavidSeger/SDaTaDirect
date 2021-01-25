package unibas.dmi.sdatadirect.net.wifi.p2p

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils.SetSynchronization
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.collections.HashMap

class ConnectionManager() {
    companion object {
        lateinit var peers: PeerViewModel
        val TAG: String = "ConnectionManager"
        val connections: HashMap<String?, Socket> = HashMap()
        val listeners: HashMap<String?, ConnectionListener> = HashMap()
        lateinit var cryptoHandler: CryptoHandler
        fun addConnection(wifiMacAddress: String?, sock: Socket) {
            connections.put(wifiMacAddress, sock)
        }

        fun getSocket(wifiMacAddress: String?): Socket? {
            return connections.get(wifiMacAddress)
        }

        fun addListener(wifiMacAddress: String?, listener: ConnectionListener) {
            listeners.put(wifiMacAddress, listener)
        }

        private fun getListener(wifiMacAddress: String?): ConnectionListener? {
            return listeners.get(wifiMacAddress)
        }

        fun sendPackage(receiverAddress: String, pack: ByteArray) {
            val peer = peers.getPeerByWiFiAddress(receiverAddress)

            val encryptedPackage = cryptoHandler.encryptAES(pack, peer?.shared_key!!)

            val packageStreamEncodedToString = Base64.getUrlEncoder().encodeToString(encryptedPackage)

            val signature = cryptoHandler.createSignature(encryptedPackage, peer.private_key)

            val signatureEncodedToString = Base64.getEncoder().encodeToString(signature)


            val json: String =  "{\"package\" : \"$packageStreamEncodedToString\"," +
                    "\"signature\" : \"$signatureEncodedToString\"}"

            Log.d(TAG, json)
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            val outStream: OutputStream = getSocket(receiverAddress)!!.getOutputStream()
            val dataWriter = DataOutputStream(outStream)

            dataWriter.writeInt(encodedNode.size)
            dataWriter.write(encodedNode)
        }
    }
}