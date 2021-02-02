package unibas.dmi.sdatadirect.peer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerInfo

class PeerViewModel(application: Application): AndroidViewModel(application) {

    private val repository: PeerRepository
    val allPeers: LiveData<List<Peer>>

    init {
        val peerDao = AppDatabase.getDatabase(application, viewModelScope).peersDao()
        repository = PeerRepository(peerDao)
        allPeers = repository.allPeers
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(peer: Peer) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(peer)
    }

    fun getPeerByBluetoothAddress(bluetoothAddress: String): Peer? {
        return repository.getPeerbyBluetoothAddress(bluetoothAddress)
    }

    fun getPeerByWiFiAddress(wifiAddress: String): Peer? {
        return repository.getPeerByWifiAddress(wifiAddress)
    }

    fun getIpByWifiAddress(wifiAddress: String): String? {
        return repository.getIpByWifiAddress(wifiAddress)
    }

    fun insertIp(ip: String, wifiAddress: String) = viewModelScope.launch(Dispatchers.IO){
        repository.insertIp(ip, wifiAddress)
    }

    fun deleteAllPeers(){
       repository.deleteAll()
    }

    fun getIp(wifiAddress: String): String?{
       return repository.getIp(wifiAddress)
    }

    fun getLastSync(public_key: String): Long?{
        return repository.getLastSync(public_key)
    }

    fun getPeerSubscriptions(public_key: String): Array<PeerInfo>?{
        return repository.getPeerSubscriptions(public_key)
    }
    fun getPeerId(public_key: String): Int?{
        return repository.getPeerId(public_key)
    }

    fun setLastSync(public_key: String, lastSync: Long){
        repository.setLastSync(public_key, lastSync)
    }

    fun getByPublicKey(public_key: String): Peer?{
        return repository.getByPublicKey(public_key)
    }
}