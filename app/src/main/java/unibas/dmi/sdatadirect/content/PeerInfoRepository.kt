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

      suspend fun insert(vararg: PeerInfo){
         peerInfoDao.insert(vararg)
     }

    fun isSubscribed(wifiAddress: String, feed_key: String):Boolean{
        return peerInfoDao.isSubscribed(wifiAddress, feed_key)
    }

    fun get(receiver: String): Array<PeerInfo> {
        return peerInfoDao.get(receiver)
    }

    fun exists(receiver: String, feed_key: String): Boolean{
        return peerInfoDao.exists(receiver, feed_key)
    }

    fun subscribePeer(peerAddress: String, feedkey: String) {
        return peerInfoDao.subscribe(peerAddress, feedkey)
    }
}