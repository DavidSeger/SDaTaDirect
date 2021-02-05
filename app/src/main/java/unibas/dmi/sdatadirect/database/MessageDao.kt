package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve message data from the database
 */

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: Message)

    @Query("SELECT * FROM message WHERE feed_key = :feed_key ORDER BY timestamp DESC")
    fun getFullFeed(feed_key: String): Array<Message>

    @Query("SELECT * FROM message WHERE feed_key = :feed_key AND sequenceNumber > :sequenceNumber ORDER BY sequenceNumber ASC")
    fun getNewMessages(feed_key: String, sequenceNumber: Long): Array<Message>

    @Query("SELECT * FROM message WHERE feed_key = :feed_key AND signature = :signature ORDER BY sequenceNumber ASC")
    fun getAllBySignature(feed_key: String, signature: String): Array<Message>

    @Query("SELECT MAX(sequenceNumber) FROM message WHERE feed_key = :feed_key")
    fun getNewestMessage(feed_key: String): Long
}