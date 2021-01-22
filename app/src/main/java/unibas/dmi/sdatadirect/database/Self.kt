package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Data class defining own data used in a public network environment
 */

@Entity(tableName = "self")
data class Self (

    @PrimaryKey
    var name: String = "",

    @ColumnInfo(name = "public_key")
    var pubKey: String? = null,

    @ColumnInfo(name = "private_key")
    var privKey: String? = null

)