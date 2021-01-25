package unibas.dmi.sdatadirect.database

import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface Peer_InfoDao {
    @Query("DELETE FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key ")
    fun unsubscribePeer(wifiAddress: String, feed_key: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun subscribePeer(vararg: PeerInfo)

    @Query("SELECT EXISTS(SELECT * FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key)")
    fun isSubscribed(wifiAddress: String, feed_key: String): Boolean
}