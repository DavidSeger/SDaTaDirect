package unibas.dmi.sdatadirect.content

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.FeedDao
import unibas.dmi.sdatadirect.Feed_overview_activity
import unibas.dmi.sdatadirect.ui.FeedListAdapter


/**
 * Repository to retrieve data from the database via the interface
 */
class FeedRepository(private val feedDao: FeedDao) {


    suspend fun insert(vararg: Feed){
        feedDao.insert(vararg)
        Feed_overview_activity.feeds.add(vararg)
    }

    fun subscribe(feed_key: String){
        feedDao.subscribe(feed_key)
    }

    fun unsubscribe(feed_key: String){
        feedDao.unsubscribe(feed_key)
    }

    fun getAllFeeds():Array<Feed>{
        return feedDao.getAllFeeds()
    }

    fun isKnown(feed_Key: String): Boolean {
        return feedDao.isKnown(feed_Key)
    }
}