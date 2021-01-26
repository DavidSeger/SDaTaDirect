package unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils

import android.util.Log
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.PeerInfoViewModel
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.net.wifi.p2p.ConnectionManager
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.PackageFactory

/**
 * receiver side of the reconciliation protocol
 */
class SetSynchronization() {
    companion object {
        val TAG = "SetSynchronization"
        var isMaster: Boolean = false
        fun startSynchronization(partner: String) {
            isMaster = true //flag used in communication
            //phase 1: the host of the connection (group owner of the p2p group) starts with updating the client
            //about all of the feeds it has newly discovered/subscribed to/unsubscribed from
            sendFeedUpdates(partner)
            sendEndPhaseOneFlag(partner)
            sendLastSync(partner)
        }

        private lateinit var peers: PeerViewModel
        private lateinit var peerInfos: PeerInfoViewModel
        private lateinit var feeds: FeedViewModel

        fun setup(peerViewModel: PeerViewModel, peerInfoViewModel: PeerInfoViewModel, feedViewModel: FeedViewModel){
            peers = peerViewModel
            peerInfos = peerInfoViewModel
            feeds = feedViewModel
        }
        //phase 1 methods

        /**
         * used after device receives a feed update message by a peer,
         * if the device also knows this feed, just the peer info table will be updated, otherwise
         * the device asks for full feed information to save it
         */
        fun receiveFeedUpdate(feedkey: String, subscribed: Boolean, peerAddress: String) {
            if (feeds.isKnown(feedkey)) {
                if (subscribed) {
                    var peerInfo = PeerInfo(
                        peer_id = peers.getPeerByWiFiAddress(peerAddress)!!.id!!,
                        feed_key = feedkey
                    )
                    peerInfos.subscribe(peerInfo)
                } else {
                    if (peerInfos.isSubscribed(peerAddress, feedkey)) {
                        peerInfos.unsubscribe(peerAddress, feedkey)
                    }
                }
            } else {
                inquireFeedDetails(feedkey, peerAddress)
            }
        }

        private fun inquireFeedDetails(feedkey: String, peerAddress: String) {
            ConnectionManager.sendPackage(peerAddress, PackageFactory.inquireFeedDetails(feedkey))
        }

        fun receiveFeedInquiry(feedkey: String, peerAddress: String) {
            var f = feeds.getFeed(feedkey)
            ConnectionManager.sendPackage(peerAddress, PackageFactory.answerFeedInquiry(f))
        }

        private fun sendFeedUpdates(receiver: String) {
                //phase 1: get all feeds that have changed something since the last sync,
                //send for all changed feeds a package notifying the sync partner (if this is
                //the first time connecting with the partner, the last Sync variable is 0, so it
                //will get all feeds)
                var lastSync = peers.getLastSync(peers.getPeerByWiFiAddress(receiver)!!.public_key!!)
                var myFeeds = feeds.getAllChangedSinceTimestamp(lastSync!!)
                Log.d(TAG, myFeeds.size.toString())
                for (f in myFeeds) {
                    ConnectionManager.sendPackage(receiver, PackageFactory.sendFeedUpdate(f))
                }
        }
        fun receiveFeedInquiryAnswer(
            feed: Feed,
            subscribed: Boolean,
            sender: String
        ) {
            feeds.insert(feed)
            if (subscribed) {
                peerInfos.subscribe(PeerInfo(peer_id = peers.getPeerByWiFiAddress(sender)!!.id!!, feed_key = feed.key))
            }
        }

       private fun sendEndPhaseOneFlag(receiver: String){
            ConnectionManager.sendPackage(receiver, PackageFactory.endPhaseOne())
        }

        fun receiveEndPhaseOne(sender: String) {
            if(!isMaster){
                sendFeedUpdates(sender)
            }
        }

        /**
         * used to synchronize the "last sync" variable across both partners
         */
        private fun sendLastSync(receiver: String){
            var lastSync = System.currentTimeMillis()
            peers.setLastSync(peers.getPeerByWiFiAddress(receiver)!!.public_key!!, lastSync)
            ConnectionManager.sendPackage(receiver, PackageFactory.sendLastSync(lastSync))
        }

        fun receiveLastSync(partner: String, lastSync: Long){
            peers.setLastSync(peers.getPeerByWiFiAddress(partner)!!.public_key!!, lastSync)
        }
    }

}