package unibas.dmi.sdatadirect.content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.MessageActivity
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Message

class MessageViewModel(application: Application): AndroidViewModel(application) {

    private val repository: MessageRepository
    lateinit var feeds: FeedViewModel

    init {
        val messageDao = AppDatabase.getDatabase(application, viewModelScope).messagesDao()
        repository = MessageRepository(messageDao)

    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(message: Message) =viewModelScope.launch(Dispatchers.IO) {
        repository.insert(message)
        feeds.updateLastReceivedMessage(System.currentTimeMillis(), message.feed_key!!)
    }

    fun getFullFeed(feed_key: String): ArrayList<Message>{
        return repository.getFullFeed(feed_key).toCollection(ArrayList())
    }


    fun getNewMessages(feed_key: String, timestamp: Long): Array<Message>{
        return repository.getNewMessages(feed_key, timestamp)
    }

    fun getAllBySignature(feed_key: String, signature: String): Array<Message>{
        return repository.getAllBySignature(feed_key, signature)
    }

    fun getNewestMessage(feed_key: String): Long{
        return repository.getNewestMessage(feed_key)
    }
}