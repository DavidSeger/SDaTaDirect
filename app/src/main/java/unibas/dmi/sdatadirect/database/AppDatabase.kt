package unibas.dmi.sdatadirect.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Peer::class), version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun peersDao(): PeerDao
}