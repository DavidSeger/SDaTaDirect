package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*
import java.sql.Timestamp

/**
 * Intefaces defines functions for queries in order to retrieve Feed data from DB
 */

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg: Feed)

    @Query("UPDATE feed SET subscribed = 1 WHERE `key` = :feed_key")
    fun subscribe(feed_key: String)

    @Query("UPDATE feed SET subscribed = 0 WHERE `key` = :feed_key")
    fun unsubscribe(feed_key: String)

    @Query("SELECT * FROM feed")
    fun getAllFeeds(): Array<Feed>

    @Query("SELECT EXISTS(SELECT * FROM feed WHERE `key` = :feed_Key)")
    fun isKnown(feed_Key: String): Boolean

    @Query("SELECT * FROM feed WHERE `key` = :feed_key")
    fun getFeed(feed_key: String): Feed

    @Query("SELECT * FROM feed WHERE last_change >= :lastSync")
    fun getAllChangedSinceTimestamp(lastSync: Long): Array<Feed>

    @Query("UPDATE feed SET last_change = :l WHERE `key` = :feed_key")
    fun updateLastChange(feed_key: String, l: Long)

    @Query("SELECT feed.* FROM feed, peer_info WHERE isSubscribed AND peer_info.peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND last_received_message >= :lastSync AND feed_key = `key`")
    fun getPeerSubscribedFeedsWithChanges(wifiAddress: String, lastSync: Long): Array<Feed>

    @Query("UPDATE feed SET last_received_message = :timestamp WHERE `key` = :feedKey")
    fun updateLastReceivedMessage(timestamp: Long, feedKey: String)

    @Query("SELECT * FROM feed WHERE owner = :ownerKey AND type = 'priv'")
    fun getFeedByOwner(ownerKey: String): Feed
}