package unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils
import android.util.Log
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.content.PeerInfoViewModel
import unibas.dmi.sdatadirect.content.SelfViewModel
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerInfo
import unibas.dmi.sdatadirect.net.wifi.p2p.ConnectionManager
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.statistics.Evaluator
import unibas.dmi.sdatadirect.utils.PackageFactory



class NaiveSynchronization() {
    companion object {
        val TAG = "NaiveSetSynchronization"
        var feedList: HashMap<String, Feed> = HashMap()
        var done: Boolean = false
        var syncing: Boolean = false
        fun simulateSynchronization(partner: String) {
            var timestamp: Long = System.currentTimeMillis()
            simulateFeedSynchronization(partner)
            Evaluator.syncTimeNaive = System.currentTimeMillis() - timestamp
        }
        private lateinit var peers: PeerViewModel
        private lateinit var peerInfos: PeerInfoViewModel
        private lateinit var feeds: FeedViewModel
        private lateinit var messages: MessageViewModel
        private lateinit var self: SelfViewModel

        fun setup(
            peerViewModel: PeerViewModel,
            peerInfoViewModel: PeerInfoViewModel,
            feedViewModel: FeedViewModel,
            messageViewModel: MessageViewModel,
            selfViewModel: SelfViewModel
        ) {
            peers = peerViewModel
            peerInfos = peerInfoViewModel
            feeds = feedViewModel
            messages = messageViewModel
            self = selfViewModel
        }

        fun simulateFeedSynchronization(partner: String) {
            var feedListTmp = feeds.getAllFeeds().toCollection(ArrayList())
            for (f in feedListTmp){
                feedList.put(f.key, f)
            }
            for (f in feedList.values){
                Evaluator.packagesSentNaive++
                ConnectionManager.sendPackage(partner, PackageFactory.naiveSendFeed(f))
            }
            simulateMessageSynchronization(partner)
        }

        fun receiveFeed(feed: Feed, sender: String) {
            Evaluator.packagesReceivedNaive++
            if(!syncing) {
                syncing = true
                simulateSynchronization(sender)
            }
            if (feedList.containsKey(feed.key)){
                /* no-op */
            }
            else feedList.put(feed.key, feed)
        }

        fun simulateMessageSynchronization(partner: String) {
            var feedListTmp = feeds.getAllSubscribed()
            for (f in feedListTmp){
                var newestMsg = f.last_received_message_seq
                Evaluator.packagesSentNaive++
                ConnectionManager.sendPackage(partner, PackageFactory.naiveRequestMessages(f.key, newestMsg))
            }
        }

        fun receiveRangeMessageRequest(sender: String, feedKey: String?, lowerLimit: Long) {
            if (feedList.containsKey(feedKey)){
               var msgs = messages.getNewMessages(feedKey!!, lowerLimit)
                for (m in msgs){
                    Evaluator.packagesSentNaive++
                    ConnectionManager.sendPackage(sender, PackageFactory.naiveSendMessages(m))
                }
                done = true
            } else{
                /* no-op */
            }
        }

        fun receiveMessage(receivedMessage: Message, sender: String) {
            Evaluator.packagesReceivedNaive++
        }


    }

}