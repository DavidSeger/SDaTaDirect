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
    fun insert(peerInfo: PeerInfo) =viewModelScope.launch(Dispatchers.IO) {
        repository.insert(peerInfo)
    }

    fun unsubscribe(wifiAddress: String, feed_key: String){
        repository.unsubscribePeer(wifiAddress, feed_key)
    }

    fun isSubscribed(wifiAddress: String, feed_key: String): Boolean{
       return repository.isSubscribed(wifiAddress, feed_key)
    }

    fun get(receiver: String): Array<PeerInfo> {
        return repository.get(receiver)
    }

    fun exists(receiver: String, feed_key: String):Boolean{
        return repository.exists(receiver, feed_key)
    }

    fun subscribe(peerAddress: String, feedkey: String) {
        repository.subscribePeer(peerAddress, feedkey)
    }
}