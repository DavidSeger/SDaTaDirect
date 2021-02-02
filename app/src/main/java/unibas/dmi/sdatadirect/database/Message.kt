package unibas.dmi.sdatadirect.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Data class defining Messages that belong to a feed
 */

@Entity(foreignKeys = arrayOf(
    ForeignKey(
        entity = Feed::class,
        parentColumns = arrayOf("key"),
        childColumns = arrayOf("feed_key")
    )
), tableName = "message"
)

data class Message(
    @PrimaryKey(autoGenerate = true)
    val message_id: Long,

    @ColumnInfo(name = "sequenceNumber")
    var sequence_Nr: Long,

    @ColumnInfo(name = "signature")
    var signature: String? = null,

    @ColumnInfo(name = "feed_key")
    var feed_key: String? = null,

    @ColumnInfo(name = "content")
    var content: ByteArray? = null,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long? = null


)