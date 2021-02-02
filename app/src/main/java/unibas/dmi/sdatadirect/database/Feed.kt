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
    var subscribed: Boolean? = false,

    /**
     * the last time a change to the feed itself was made (subscribed, unsubscribed, inserted etc.)
     */
    @ColumnInfo(name = "last_change")
    var last_change: Long = 0L,

    /**
     * the last time a message in this feed was received
     */
    @ColumnInfo(name = "last_received_message")
    var last_received_message: Long = 0L,

    @ColumnInfo(name = "owner")
    var owner: String? = null
)