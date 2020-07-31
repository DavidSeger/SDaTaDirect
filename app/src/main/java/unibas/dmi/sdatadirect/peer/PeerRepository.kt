package unibas.dmi.sdatadirect.peer

import androidx.lifecycle.LiveData
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerDao


/**
 * Repository to retrieve data from the database via the PeerDao interface
 */
class PeerRepository(private val peerDao: PeerDao) {

    val allPeers: LiveData<List<Peer>> = peerDao.getAll()

    fun getPeerbyBluetoothAddress(bluetoothAddress: String): Peer? {
        return peerDao.findByBluetoothAddress(bluetoothAddress)
    }

    fun getPeerByWifiAddress(wifiAddress: String): Peer? {
        return peerDao.findByWifiAddress(wifiAddress)
    }

    suspend fun insert(peer: Peer) {
        peerDao.insert(peer)
    }

    fun deleteAll(){
        peerDao.deleteAll()
    }
}