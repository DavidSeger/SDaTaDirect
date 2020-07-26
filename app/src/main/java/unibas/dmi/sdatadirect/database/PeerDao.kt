package unibas.dmi.sdatadirect.database

import androidx.room.*
import unibas.dmi.sdatadirect.database.Peer
import javax.crypto.SecretKey

@Dao
interface PeerDao {
    @Query("SELECT * FROM peer")
    fun getAll(): List<Peer>

    @Query("SELECT * FROM peer WHERE id IN (:Ids)")
    fun loadAllByIds(Ids: IntArray): List<Peer>

    @Query("SELECT * FROM peer WHERE bluetooth_mac_address LIKE :bluetoothAddress")
    fun findByBluetoothAddress(bluetoothAddress: String): Peer?

    @Query("SELECT * FROM peer WHERE wifi_mac_address LIKE :wifiAddress")
    fun findByWifiAddress(wifiAddress: String): Peer?

    @Query("SELECT * FROM peer WHERE shared_key LIKE :key")
    fun findByKey(key: String): Peer?

    @Insert
    fun insertAll(vararg: Peer)

    @Delete
    fun delete(peer: Peer)

    @Query("UPDATE peer SET wifi_mac_address = :wifiAddress WHERE name = :name")
    fun updatePeer(name: String, wifiAddress: String): Int

}