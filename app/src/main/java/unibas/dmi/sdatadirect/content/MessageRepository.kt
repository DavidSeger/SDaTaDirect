package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.database.MessageDao


/**
 * Repository to retrieve data from the database via the interface
 */
class MessageRepository(private val MessageoDao: MessageDao) {

    suspend fun insert(vararg: Message){
        MessageoDao.insert(vararg)
    }

    fun getFullFeed(feed_key: String): Array<Message>{
       return MessageoDao.getFullFeed(feed_key)
    }


    fun getNewMessages(feed_key: String, sequenceNumber: Long): Array<Message>{
        return MessageoDao.getNewMessages(feed_key, sequenceNumber)
    }

   fun getAllBySignature(feed_key: String, signature: String): Array<Message>{
       return MessageoDao.getAllBySignature(feed_key, signature)
   }

    fun getNewestMessage(feed_key: String): Long{
        return MessageoDao.getNewestMessage(feed_key)
    }
}