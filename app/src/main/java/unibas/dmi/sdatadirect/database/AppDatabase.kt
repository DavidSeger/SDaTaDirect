package unibas.dmi.sdatadirect.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

/**
 * Abstract class defining the database
 */

@Database(entities = arrayOf(Peer::class, PeerInfo::class, Message::class, Feed::class, Self::class), version = 26)
abstract class AppDatabase: RoomDatabase() {

    abstract fun peersDao(): PeerDao
    abstract fun feedsDao(): FeedDao
    abstract fun messagesDao(): MessageDao
    abstract fun peerInfosDao(): Peer_InfoDao
    abstract fun selfDao(): SelfDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "peer_database"
                )
                    .allowMainThreadQueries()
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
        }
    }

}