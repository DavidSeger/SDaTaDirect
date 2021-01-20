package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class defining Feeds
 */

@Entity(tableName = "feed")
data class Feed (
    @PrimaryKey()
    val key: String,

    /**
     * can be either a pub or a user
     */
    @ColumnInfo(name = "type")
    var type: String? = null,

    @ColumnInfo(name = "host")
    var host: String? = null,

    @ColumnInfo(name = "port")
    var port: String? = null,

    /**
     * flag that shows if you yourself are subcribed to the feed
     */
    @ColumnInfo(name = "subscribed")
    var subscribed: Boolean? = false

)