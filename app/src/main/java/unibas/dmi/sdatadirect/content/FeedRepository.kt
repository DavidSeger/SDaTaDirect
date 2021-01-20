package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.FeedDao


/**
 * Repository to retrieve data from the database via the interface
 */
class FeedRepository(private val feedDao: FeedDao) {

    suspend fun insert(vararg: Feed){
        feedDao.insert(vararg)
    }

    fun subscribe(feed_key: String){
        feedDao.subscribe(feed_key)
    }

    fun unsubscribe(feed_key: String){
        feedDao.unsubscribe(feed_key)
    }
}