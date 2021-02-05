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
    fun insert(peerInfo: PeerInfo){
        repository.insert(peerInfo)
    }

    fun unsubscribe(wifiAddress: String, feed_key: String){
        repository.unsubscribePeer(wifiAddress, feed_key)
    }

    fun isSubscribed(pubKey: String, feed_key: String): Boolean{
       return repository.isSubscribed(pubKey, feed_key)
    }

    fun getAllSubscribed(receiver: String): Array<PeerInfo> {
        return repository.getAllSubscribed(receiver)
    }

    fun exists(pubKey: String, feed_key: String):Boolean{
        return repository.exists(pubKey, feed_key)
    }

    fun subscribe(peerKey: String, feedkey: String) {
        repository.subscribePeer(peerKey, feedkey)
    }

    fun get(pubKey: String, feedKey: String): PeerInfo {
        return repository.get(pubKey, feedKey)
    }

    fun updateLastSentMessage(peerKey: String, feedKey: String, newestMessage: Long) {
        return repository.updateLastSentMessage(peerKey, feedKey, newestMessage)
    }

    fun getAll():Array<PeerInfo>{
        return repository.getAll()
    }

}