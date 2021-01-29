package unibas.dmi.sdatadirect.database

import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface Peer_InfoDao {
    @Query("UPDATE peer_info SET isSubscribed = 'false' WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key ")
    fun unsubscribePeer(wifiAddress: String, feed_key: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: PeerInfo)

    @Query("SELECT EXISTS(SELECT * FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key AND isSubscribed = 'true')")
    fun isSubscribed(wifiAddress: String, feed_key: String): Boolean

    @Query("SELECT * FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress)")
    fun get(wifiAddress: String): Array<PeerInfo>

    @Query("SELECT EXISTS (SELECT * FROM peer_info WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feedKey)")
    fun exists(wifiAddress: String, feedKey: String): Boolean

    @Query("UPDATE peer_info SET isSubscribed = 'true' WHERE peer_id = (SELECT id FROM peer_table WHERE wifi_mac_address = :peerAddress) AND feed_key = :feedKey")
    fun subscribe(peerAddress: String, feedKey: String)
}