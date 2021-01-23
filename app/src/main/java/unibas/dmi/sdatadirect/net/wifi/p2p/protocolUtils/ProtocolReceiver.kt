package unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils

import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.PeerInfoViewModel
import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.peer.PeerViewModel

/**
 * receiver side of the reconciliation protocol
 */
class ProtocolReceiver(peers: PeerViewModel, peerInfos: PeerInfoViewModel, feeds: FeedViewModel){
    val peers = peers
    val peerInfos = peerInfos
    val feeds = feeds
    //phase 1 methods
    fun unsubscribe(feedkey: String, peerAddress: String) {
        if (feeds.isKnown(feedkey)) {
            peerInfos.unsubscribe(peerAddress, feedkey)
        } else {}//TODO: implement feed discovery sub protocol
    }
    fun subscribe(feedkey: String, peerAddress: String) {
            if (feeds.isKnown(feedkey)) {
                var peerInfo = PeerInfo(
                    peer_id = peers.getPeerByWiFiAddress(peerAddress)!!.id!!,
                    feed_key = feedkey
                )
                peerInfos.subscribe(peerInfo)
            } else {}//TODO: implement feed discovery sub protocol
    }

}