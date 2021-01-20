package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: Message)

    @Query("SELECT * FROM message WHERE feed_key = :feed_key ORDER BY timestamp DESC")
    fun getFullFeed(feed_key: String): Array<Message>

    @Query("SELECT * FROM message WHERE feed_key = :feed_key AND timestamp > :timestamp ORDER BY timestamp DESC")
    fun getNewMessages(feed_key: String, timestamp: Long): Array<Message>

    @Query("SELECT * FROM message WHERE feed_key = :feed_key AND signature = :signature ORDER BY timestamp DESC")
    fun getAllBySignature(feed_key: String, signature: String): Array<Message>

    @Query("SELECT MAX(timestamp) FROM message WHERE feed_key = :feed_key")
    fun getNewestMessage(feed_key: String): Long
}