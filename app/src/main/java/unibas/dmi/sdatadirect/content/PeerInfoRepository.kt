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

     fun insert(vararg: PeerInfo){
         peerInfoDao.insert(vararg)
     }

    fun isSubscribed(pubKey: String, feed_key: String):Boolean{
        return peerInfoDao.isSubscribed(pubKey, feed_key)
    }

    fun getAllSubscribed(receiver: String): Array<PeerInfo> {
        return peerInfoDao.getAllSubscribed(receiver)
    }

    fun exists(pubKey: String, feed_key: String): Boolean{
        return peerInfoDao.exists(pubKey, feed_key)
    }

    fun subscribePeer(peerKey: String, feedkey: String) {
        return peerInfoDao.subscribe(peerKey, feedkey)
    }
    fun get(pubKey: String, feedKey: String): PeerInfo {
        return peerInfoDao.get(pubKey, feedKey)
    }

    fun updateLastSentMessage(peerKey: String, feedKey: String, newestMessage: Long) {
        return peerInfoDao.updateLastSentMessage(peerKey, feedKey, newestMessage)
    }

    fun getAll():Array<PeerInfo>{
        return peerInfoDao.getAll()
    }
}