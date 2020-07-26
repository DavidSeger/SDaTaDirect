package unibas.dmi.sdatadirect.database

import androidx.lifecycle.LiveData
import androidx.room.*
import unibas.dmi.sdatadirect.database.Peer
import javax.crypto.SecretKey

@Dao
interface PeerDao {
    @Query("SELECT * FROM peer")
    fun getAll(): LiveData<List<Peer>>

    @Query("SELECT * FROM peer WHERE bluetooth_mac_address LIKE :bluetoothAddress")
    fun findByBluetoothAddress(bluetoothAddress: String): Peer?

    @Query("SELECT * FROM peer WHERE wifi_mac_address LIKE :wifiAddress")
    fun findByWifiAddress(wifiAddress: String): Peer?

    @Query("SELECT * FROM peer WHERE shared_key LIKE :shared_key")
    fun findBySharedKey(shared_key: String): Peer?

    @Query("SELECT * FROM peer WHERE public_key LIKE :public_key")
    fun findByPublicKey(public_key: String): Peer?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg: Peer)

    @Delete
    suspend fun delete(peer: Peer)

    @Query("DELETE FROM peer")
    suspend fun deletaAll()
}