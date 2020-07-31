package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class defining Peer entity
 */

@Entity(tableName = "peer_table")
data class Peer (
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "bluetooth_mac_address")
    var bluetooth_mac_address: String? = null,

    @ColumnInfo(name = "wifi_mac_address")
    var wifi_mac_address: String? = null,

    @ColumnInfo(name = "shared_key")
    var shared_key: String? = null,

    @ColumnInfo(name = "public_key")
    var public_key: String? = null,

    @ColumnInfo(name = "private_key")
    var private_key: String? = null,

    @ColumnInfo(name = "foreign_public_key")
    var foreign_public_key: String? = null
)