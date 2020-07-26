package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Peer (
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bluetooth_mac_address")
    val bluetooth_mac_address: String,

    @ColumnInfo(name = "wifi_mac_address")
    val wifi_mac_address: String,

    @ColumnInfo(name = "shared_key")
    val shared_key: String,

    @ColumnInfo(name = "public_key")
    val public_key: String
)