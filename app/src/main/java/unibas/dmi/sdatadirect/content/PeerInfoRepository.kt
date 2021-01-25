package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.database.Peer_InfoDao


/**
 * Repository to retrieve data from the database via the interface
 */
class PeerInfoRepository(private val peerInfoDao: Peer_InfoDao) {

     fun unsubscribePeer(wifiAddress: String, feed_key: String){
         peerInfoDao.unsubscribePeer(wifiAddress, feed_key)
     }

      suspend fun subscribePeer(vararg: PeerInfo){
         peerInfoDao.subscribePeer(vararg)
     }

    fun isSubscribed(wifiAddress: String, feed_key: String):Boolean{
        return peerInfoDao.isSubscribed(wifiAddress, feed_key)
    }
}