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
    ), ForeignKey(entity = Peer::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("peer_id"))
), tableName = "peer_info"
)
/**
 * Data class for relation between Peers and their subscribed tables, id = peer id, key = feed id
 */
data class PeerInfo(
    @PrimaryKey()
    val peer_id: Int,
    val feed_key: String
)