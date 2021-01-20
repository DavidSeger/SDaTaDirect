package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.database.Peer_InfoDao


/**
 * Repository to retrieve data from the database via the interface
 */
class PeerInfoRepository(private val peerInfoDao: Peer_InfoDao) {

     fun unsubscribePeer(public_key: String, feed_key: String){
         peerInfoDao.unsubscribePeer(public_key, feed_key)
     }

      suspend fun subscribePeer(vararg: PeerInfo){
         peerInfoDao.subscribePeer(vararg)
     }
}