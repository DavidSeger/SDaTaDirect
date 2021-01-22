package unibas.dmi.sdatadirect.content


import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.FeedDao
import unibas.dmi.sdatadirect.Feed_overview_activity
import unibas.dmi.sdatadirect.database.Self
import unibas.dmi.sdatadirect.database.SelfDao
import unibas.dmi.sdatadirect.ui.FeedListAdapter


/**
 * Repository to retrieve data from the database via the interface
 */
class SelfRepository(private val selfDao: SelfDao) {


    suspend fun insert(vararg: Self){
            selfDao.insert(vararg)
    }

    fun getSelf():Self{
        return selfDao.getSelf()
    }
}