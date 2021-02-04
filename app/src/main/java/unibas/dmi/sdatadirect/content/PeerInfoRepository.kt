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

    fun exists(receiver: String, feed_key: String): Boolean{
        return peerInfoDao.exists(receiver, feed_key)
    }

    fun subscribePeer(peerAddress: String, feedkey: String) {
        return peerInfoDao.subscribe(peerAddress, feedkey)
    }
    fun get(peerAddress: String, feedKey: String): PeerInfo {
        return peerInfoDao.get(peerAddress, feedKey)
    }

    fun updateLastSentMessage(sender: String, feedKey: String, newestMessage: Long) {
        return peerInfoDao.updateLastSentMessage(sender, feedKey, newestMessage)
    }
}