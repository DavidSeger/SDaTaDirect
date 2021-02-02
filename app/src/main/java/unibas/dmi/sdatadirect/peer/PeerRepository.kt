package unibas.dmi.sdatadirect.peer

import androidx.lifecycle.LiveData
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerDao
import unibas.dmi.sdatadirect.database.PeerInfo


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

    fun getIpByWifiAddress(wifiAddress: String): String? {
        return peerDao.getIp(wifiAddress)
    }


    fun deleteAll(){
        peerDao.deleteAll()
    }

    fun getIp(wifiAddress: String): String?{
        return peerDao.getIp(wifiAddress)
    }

    suspend fun insertIp(ip: String, wifiAddress: String){
        peerDao.insertIp(ip, wifiAddress)
    }

    fun getLastSync(public_key: String): Long?{
        return peerDao.getLastSync(public_key)
    }

    fun getPeerSubscriptions(public_key: String): Array<PeerInfo>?{
        return peerDao.getPeerSubscriptions(public_key)
    }
    fun getPeerId(public_key: String): Int?{
        return peerDao.getPeerId(public_key)
    }

    fun setLastSync(public_key: String, lastSync: Long){
        peerDao.setLastSync(public_key, lastSync)
    }

    fun getByPublicKey(public_key: String): Peer?{
        return peerDao.findByPublicKey(public_key)
    }

}