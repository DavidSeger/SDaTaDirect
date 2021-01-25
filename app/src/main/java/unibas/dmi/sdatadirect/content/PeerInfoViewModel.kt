package unibas.dmi.sdatadirect.content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.PeerInfo

class PeerInfoViewModel(application: Application): AndroidViewModel(application) {

    private val repository: PeerInfoRepository

    init {
        val peerInfoDao = AppDatabase.getDatabase(application, viewModelScope).peerInfosDao()
        repository = PeerInfoRepository(peerInfoDao)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun subscribe(peerInfo: PeerInfo) =viewModelScope.launch(Dispatchers.IO) {
        repository.subscribePeer(peerInfo)
    }

    fun unsubscribe(wifiAddress: String, feed_key: String){
        repository.unsubscribePeer(wifiAddress, feed_key)
    }

    fun isSubscribed(wifiAddress: String, feed_key: String): Boolean{
       return repository.isSubscribed(wifiAddress, feed_key)
    }
}