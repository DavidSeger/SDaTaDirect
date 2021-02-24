package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.ui.FeedListAdapter
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Feed

class FeedViewModel(application: Application): AndroidViewModel(application) {

    private val repository: FeedRepository

    init {
        val feedDao = AppDatabase.getDatabase(application, viewModelScope).feedsDao()
        repository = FeedRepository(feedDao)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(feed: Feed){
        repository.insert(feed)
    }


    fun subscribe(feed_key: String){
        repository.subscribe(feed_key)
    }

    fun unsubscribe(feed_key: String){
        repository.unsubscribe(feed_key)
    }

    fun getAllFeeds(): Array<Feed>{
       return repository.getAllFeeds()
    }

    fun isKnown(feed_key: String): Boolean{
        return repository.isKnown(feed_key)
    }

    fun getFeed(feed_key: String): Feed{
        return repository.getFeed(feed_key)
    }

    fun getAllChangedSinceTimestamp(lastSync: Long): Array<Feed>{
        return repository.getAllChangedSinceTimestamp(lastSync)
    }


    fun updateLastReceivedMessage(seqNr: Long, feedKey: String){
        repository.updateLastReceivedMessage(seqNr, feedKey)
    }

    fun getAllSubscribed():Array<Feed>{
        return repository.getAllSubscribed()
    }

    fun getFeedByOwner(ownerKey: String): Feed{
        return repository.getFeedByOwner(ownerKey)
    }


    fun getPubsByHostDevice(ownerKey: String): Array<Feed>{
        return repository.getPubsByHostDevice(ownerKey)
    }
}