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

    @Query("SELECT * FROM peer_table WHERE public_key LIKE :public_key")
    fun findByPublicKey(public_key: String): Peer?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg: Peer)

    @Delete
    suspend fun delete(peer: Peer)

    @Query("DELETE FROM peer_table")
    fun deleteAll()
}