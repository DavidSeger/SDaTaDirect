package unibas.dmi.sdatadirect.content

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.room.Query
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.FeedDao
import unibas.dmi.sdatadirect.Feed_overview_activity
import unibas.dmi.sdatadirect.ui.FeedListAdapter


/**
 * Repository to retrieve data from the database via the interface
 */
class FeedRepository(private val feedDao: FeedDao) {


   fun insert(vararg: Feed){
        feedDao.insert(vararg)
        feedDao.updateLastChange(vararg.key, System.currentTimeMillis())
        Feed_overview_activity.feeds.add(vararg)
    }

    fun subscribe(feed_key: String){
        feedDao.subscribe(feed_key)
        feedDao.updateLastChange(feed_key, System.currentTimeMillis())
        Feed_overview_activity.feeds.add(getFeed(feed_key))
    }

    fun unsubscribe(feed_key: String){
        feedDao.unsubscribe(feed_key)
        feedDao.updateLastChange(feed_key, System.currentTimeMillis())
        Feed_overview_activity.feeds.add(getFeed(feed_key))
    }

    fun getAllFeeds():Array<Feed>{
        return feedDao.getAllFeeds()
    }

    fun isKnown(feed_Key: String): Boolean {
        return feedDao.isKnown(feed_Key)
    }

    fun getFeed(feed_key: String): Feed{
        return feedDao.getFeed(feed_key)
    }

    fun getAllChangedSinceTimestamp(lastSync: Long): Array<Feed>{
        return feedDao.getAllChangedSinceTimestamp(lastSync)
    }

    fun getPeerSubscribedFeedsWithChanges(wifiAddress: String, lastSync: Long): Array<Feed>{
        return feedDao.getPeerSubscribedFeedsWithChanges(wifiAddress, lastSync)
    }


    fun updateLastReceivedMessage(timestamp: Long, feedKey: String){
        feedDao.updateLastReceivedMessage(timestamp, feedKey)
    }

    fun getFeedByOwner(ownerKey: String): Feed{
        return feedDao.getFeedByOwner(ownerKey)
    }

}