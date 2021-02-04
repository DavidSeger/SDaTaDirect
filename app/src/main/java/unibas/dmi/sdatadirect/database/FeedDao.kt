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

    @Query("UPDATE feed SET last_received_message_seq = :timestamp WHERE `key` = :feedKey")
    fun updateLastReceivedMessage(timestamp: Long, feedKey: String)

    @Query("SELECT * FROM feed WHERE owner = :ownerKey AND type = 'priv'")
    fun getFeedByOwner(ownerKey: String): Feed
}