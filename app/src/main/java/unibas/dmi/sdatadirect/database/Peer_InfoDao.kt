package unibas.dmi.sdatadirect.database

import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface Peer_InfoDao {
    @Query("UPDATE peer_info SET isSubscribed = 0 WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key ")
    fun unsubscribePeer(wifiAddress: String, feed_key: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg: PeerInfo)

    @Query("SELECT EXISTS(SELECT * FROM peer_info WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feed_key AND isSubscribed = 1)")
    fun isSubscribed(wifiAddress: String, feed_key: String): Boolean

    @Query("SELECT * FROM peer_info WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND isSubscribed = 1")
    fun getAllSubscribed(wifiAddress: String): Array<PeerInfo>

    @Query("SELECT EXISTS (SELECT * FROM peer_info WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :wifiAddress) AND feed_key = :feedKey)")
    fun exists(wifiAddress: String, feedKey: String): Boolean

    @Query("UPDATE peer_info SET isSubscribed = 1 WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :peerAddress) AND feed_key = :feedKey")
    fun subscribe(peerAddress: String, feedKey: String)

    @Query("SELECT * FROM peer_info WHERE peer_key = (SELECT foreign_public_key FROM peer_table WHERE wifi_mac_address = :peerAddress) and feed_key = :feedKey")
    fun get(peerAddress: String, feedKey: String): PeerInfo

}