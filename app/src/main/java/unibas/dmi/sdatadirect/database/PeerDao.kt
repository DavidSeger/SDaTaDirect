package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve peer data from the database
 */

@Dao
interface PeerDao {
    @Query("SELECT * FROM peer_table")
    fun getAll(): LiveData<List<Peer>>

    @Query("SELECT * FROM peer_table WHERE bluetooth_mac_address LIKE :bluetoothAddress")
    fun findByBluetoothAddress(bluetoothAddress: String): Peer?

    @Query("SELECT * FROM peer_table WHERE wifi_mac_address LIKE :wifiAddress")
    fun findByWifiAddress(wifiAddress: String): Peer?

    @Query("SELECT * FROM peer_table WHERE shared_key LIKE :shared_key")
    fun findBySharedKey(shared_key: String): Peer?

    @Query("SELECT * FROM peer_table WHERE foreign_public_key LIKE :public_key")
    fun findByPublicKey(public_key: String): Peer?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: Peer)

    @Query("SELECT ip_address FROM peer_table WHERE wifi_mac_address LIKE :wifiAddress")
    fun getIp(wifiAddress: String): String?

    @Query("UPDATE peer_table SET ip_address = :ip WHERE wifi_mac_address LIKE :wifiAddress")
    suspend fun insertIp(ip: String, wifiAddress: String)

    @Query("SELECT last_sync FROM peer_table WHERE public_key = :public_key")
    fun getLastSync(public_key: String): Long?

    @Query("UPDATE peer_table SET last_sync = :lastSync WHERE public_key = :public_key")
    fun setLastSync(public_key: String, lastSync: Long)

    @Query("SELECT peer_info.* FROM peer_table, peer_info WHERE public_key = :public_key AND peer_key = foreign_public_key")
    fun getPeerSubscriptions(public_key: String): Array<PeerInfo>?

    @Query("SELECT id FROM peer_table WHERE public_key = :public_key")
    fun getPeerId(public_key: String): Int?

    @Delete
    suspend fun delete(peer: Peer)

    @Query("DELETE FROM peer_table")
    fun deleteAll()
}