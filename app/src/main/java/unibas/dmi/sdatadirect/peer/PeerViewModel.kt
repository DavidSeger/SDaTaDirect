package unibas.dmi.sdatadirect.peer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Peer

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

    fun deleteAllPeers(){
       repository.deleteAll()
    }
}