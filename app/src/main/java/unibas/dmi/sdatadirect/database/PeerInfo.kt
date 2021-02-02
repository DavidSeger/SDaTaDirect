package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        entity = Feed::class,
        parentColumns = arrayOf("key"),
        childColumns = arrayOf("feed_key")
    )), tableName = "peer_info"
)
/**
 * Data class for relation between Peers and their subscribed tables, id = peer id, key = feed id
 */
data class PeerInfo(
    @PrimaryKey(autoGenerate = true)
    val peerInfo_id: Long = 0L,

    @ColumnInfo(name = "peer_key")
    val peer_key: String,
    @ColumnInfo(name = "feed_key")
    val feed_key: String,

    @ColumnInfo(name = "isSubscribed")
    var isSubscribed: Boolean = true
)