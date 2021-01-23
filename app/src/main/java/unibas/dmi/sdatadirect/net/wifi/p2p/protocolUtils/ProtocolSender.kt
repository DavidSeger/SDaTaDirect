package unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils

import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.PeerInfoViewModel
import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.peer.PeerViewModel


/**
 * sender side of the reconciliation protocol
 */
class ProtocolSender(peers: PeerViewModel, peerInfos: PeerInfoViewModel, feeds: FeedViewModel){
    val peers = peers
    val peerInfos = peerInfos
    val feeds = feeds
    //phase 1 methods
    fun unsubscribe(feedkey: String, peerAddress: String) {
      //TODO: implement 
    }
    fun subscribe(feedkey: String){
      //TODO: implement
    }

}