package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve Feed data from DB
 */

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: Feed)

    @Query("UPDATE feed SET subscribed = 1 WHERE `key` = :feed_key")
    fun subscribe(feed_key: String)

    @Query("UPDATE feed SET subscribed = 0 WHERE `key` = :feed_key")
    fun unsubscribe(feed_key: String)
}