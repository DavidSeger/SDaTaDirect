package unibas.dmi.sdatadirect.database

import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface Peer_InfoDao {
    @Query("DELETE FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE public_key = :public_key) AND feed_key = :feed_key ")
    fun unsubscribePeer(public_key: String, feed_key: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun subscribePeer(vararg: PeerInfo)
}