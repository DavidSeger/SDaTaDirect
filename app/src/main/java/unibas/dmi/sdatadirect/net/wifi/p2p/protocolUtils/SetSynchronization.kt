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

/**
 * The set synchronization protocol implementation. The synchronization can be started from any phase,
 * connection stays open after the synchronization is done. When you connect to a peer it
 * automatically goes through the full protocol with the newly connected peer. The isMaster flag
 * is used for communication flow control. If the synchronization is to be started manually, starting
 * from any phase needed, the flag has to be set to true for whoever started the synchronization.
 */

class SetSynchronization() {
    companion object {
        val TAG = "SetSynchronization"
        var isMaster: Boolean = false
        val availableFeedsByPeer: HashMap<String, ArrayList<String>> = HashMap()
        var timestamp: Long = 0L
        var databaseAccessTimes: ArrayList<Long> = ArrayList()
        fun startSynchronization(partner: String) {
            isMaster = true //flag used in communication
            //phase 1: the host of the connection (group owner of the p2p group) starts with updating the client
            //about all of the feeds it has newly discovered/subscribed to/unsubscribed from
            sendFeedUpdates(partner)
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
        //phase 1 methods

        /**
         * used after device receives a feed update message by a peer,
         * if the device also knows this feed, just the peer info table will be updated, otherwise
         * the device asks for full feed information to save it
         */
        fun receiveFeedUpdate(feedkey: String, subscribed: Boolean, peerAddress: String) {
            Evaluator.packagesReceivedHistoryAware++
            if (feeds.isKnown(feedkey)) {
                if (!peerInfos.exists(peers.getPeerByWiFiAddress(peerAddress)!!.foreign_public_key!!, feedkey)) {
                    var peerInfo = PeerInfo(
                        peer_key = peers.getPeerByWiFiAddress(peerAddress)!!.foreign_public_key!!,
                        feed_key = feedkey,
                        isSubscribed = subscribed
                    )
                    peerInfos.insert(peerInfo)
                } else {
                    if (!subscribed) {
                        peerInfos.unsubscribe(peerAddress, feedkey)
                    } else {
                        peerInfos.subscribe(peers.getPeerByWiFiAddress(peerAddress)!!.foreign_public_key!!, feedkey)
                    }
                }
            } else {
                inquireFeedDetails(feedkey, peerAddress)
            }
        }

        /**
         * ask the peer for the full feed
         */
        private fun inquireFeedDetails(feedkey: String, peerAddress: String) {
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(peerAddress, PackageFactory.inquireFeedDetails(feedkey))
        }

        /**
         * method that answers a feed inquiry, it updates the peer info table by inserting
         * the fact that the peer now knows this feed as well (set to fals eby default, since
         * the feed subscription is an opt-in thing)
         */
        fun receiveFeedInquiry(feedkey: String, peerAddress: String) {
            Evaluator.packagesReceivedHistoryAware++
            var f = feeds.getFeed(feedkey)
            peerInfos.insert(
                PeerInfo(
                    peer_key = peers.getPeerByWiFiAddress(peerAddress)!!.foreign_public_key!!,
                    feed_key = feedkey,
                    isSubscribed = false
                )
            )
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(peerAddress, PackageFactory.answerFeedInquiry(f))
        }

        /**
         * start of phase 1: based on the last synchronization with the peer, the device sends updates
         * about all the changes in feeds that occurred since the last snyc. This includes subscribing to a feed,
         * unsubscribing from a feed or if the device has discovered/created a new feed or pub
         */
        private fun sendFeedUpdates(receiver: String) {
            //phase 1: get all feeds that have changed something since the last sync,
            //send for all changed feeds a package notifying the sync partner (if this is
            //the first time connecting with the partner, the last Sync variable is 0, so it
            //will get all feeds)
            timestamp = System.currentTimeMillis()
            var lastSync = peers.getLastSync(peers.getPeerByWiFiAddress(receiver)!!.foreign_public_key!!)
            var myFeeds = feeds.getAllChangedSinceTimestamp(lastSync!!)
            Log.d(TAG, myFeeds.size.toString())
            for (f in myFeeds) {
                Evaluator.packagesSentHistoryAware++
                ConnectionManager.sendPackage(receiver, PackageFactory.sendFeedUpdate(f))
            }
            sendEndPhaseOneFlag(receiver)
        }

        /**
         * Receive an, as of yet, undiscovered feed and save it as a new known feed, per default
         * the device is not subscribed to it.
         */
        fun receiveFeedInquiryAnswer(
            feed: Feed,
            subscribed: Boolean,
            sender: String
        ) {
            Evaluator.packagesReceivedHistoryAware++
            feed.subscribed = false
            var insertiontime: Long = System.currentTimeMillis()
            feeds.insert(feed)
            databaseAccessTimes.add(System.currentTimeMillis() - insertiontime)
            /**
             * we dont know the owner of this peer, meaning we insert it into our Peer database
             */
            if (peers.getByPublicKey(feed.owner!!) == null) {
                var peer = Peer(
                    id = 0,
                    foreign_public_key = feed.owner
                )
                peers.insert(peer)
            }
            peerInfos.insert(
                PeerInfo(
                    peer_key = feed.owner!!,
                    feed_key = feed.key,
                    isSubscribed = subscribed,
                    lastSentMessage = 0L
                )
            )

        }

        /**
         * send a flow control flag to the partner, signaling it that all the updates are sent,
         * giving the control to it to send its updates.
         */
        private fun sendEndPhaseOneFlag(receiver: String) {
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(receiver, PackageFactory.endPhaseOne())
        }

        /**
         * receive the signal to end phase one for this device. If this device is the instigator
         * of the synchronization (shown with the "isMaster" boolean) the protocol moves on to
         * phase 2, if it is the passive partner, this starts the feed update process for this device
         */
        fun receiveEndPhaseOne(sender: String) {
            Evaluator.packagesReceivedHistoryAware++
            if (!isMaster) {
                sendFeedUpdates(sender)
            } else {
                //Phase 2: Tell the partner for which feeds he is subscribed to he has received new content since the last
                //meeting
                sendFeedsWithNews(sender)
            }
        }


        //Phase 2 methods
        /**
         * with this method the device lets its partner know in which feeds, that the partner has
         * subscribed to, new messages have been inserted since the last meeting. we use the "last sent sequence number"
         * field in the peer info table as a way to know if new messages have been inserted. because of the nature
         * of pubs, we also forward messages of feeds that the partner is not subscribed to, if the partner
         * is hosting a pub that the feed is subscribed to (this is what happens in the second loop).
         * Here we also update the last sent message to a peer regarding a feed.
         */
        private fun sendFeedsWithNews(partner: String) {
            var peer = peerInfos.getAllSubscribed(partner)
            //send updates for the feeds the partner device has subscribed to.
            //Update the peer info to reflect that we have offered the peer the messages up to this seq.
            for (f in peer) {
                if (feeds.getFeed(f.feed_key).owner != peers.getPeerByWiFiAddress(partner)!!.foreign_public_key) {
                    var fLastSeq = messages.getNewestMessage(f.feed_key)
                    if (f.lastSentMessage < fLastSeq) {
                        Log.d("packageFactory", "sent From First loop")
                        Evaluator.packagesSentHistoryAware++
                        ConnectionManager.sendPackage(
                            partner,
                            PackageFactory.sendSeqNr(f.feed_key, fLastSeq)
                        )
                        peerInfos.updateLastSentMessage(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!, f.feed_key, fLastSeq)
                    }
                }
            }
            //send updates on all feeds that are subscribed to a pub hosted by the partner device where the partner device is not subscribed
            //to the original private feed owner. Update the peer info to reflect that we have offered the peer the messages up to this seq.
            for (f in feeds.getAllFeeds()){
                if (f.type == "priv") {
                 for (p in feeds.getPubsByHostDevice(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!)) {
                        if (peerInfos.isSubscribed(f.owner!!, p.key)){
                            if (!peerInfos.isSubscribed(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!, f.key)){
                                var fLastSeq = messages.getNewestMessage(f.key)
                                var lastSent = peerInfos.get(f.owner!!, p.key).lastSentMessage
                                if (lastSent < fLastSeq) {
                                    Log.d("packageFactory", "sent From second loop")
                                    Evaluator.packagesSentHistoryAware++
                                    ConnectionManager.sendPackage(
                                        partner,
                                        PackageFactory.sendSeqNr(f.key, fLastSeq)
                                    )
                                }
                                peerInfos.updateLastSentMessage(f.owner!!, p.key, fLastSeq)
                            }
                        }
                    }
                }
            }



             sendEndPhaseTwoFlag(partner)
        }

        /**
         * receive a feed that contains potentially new messages. encode it as a String to store it in
         * a temporary hash map
         */
        fun receiveFeedUpdateList(sender: String, feedKey: String?, lastSeq: Long) {
            Evaluator.packagesReceivedHistoryAware++
            if (availableFeedsByPeer.containsKey(sender)) {
                var feedList = availableFeedsByPeer.get(sender)
                feedList!!.add(feedKey!! + "::" + lastSeq)
            } else {
                var feedList: ArrayList<String> = ArrayList<String>()
                feedList.add(feedKey!! + "::" + lastSeq)
                availableFeedsByPeer.put(sender, feedList)
            }
        }

        /**
         * end this phase of the protocol for this device
         */
        private fun sendEndPhaseTwoFlag(receiver: String) {
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(receiver, PackageFactory.endPhaseTwo())
        }

        /**
         * receive the signal to end phase two for this device. If this device is the instigator
         * of the synchronization (shown with the "isMaster" boolean) the protocol moves on to
         * phase 3, if it is the passive partner, this starts the message offering process for this device
         */
        fun receiveEndPhaseTwo(sender: String) {
            Evaluator.packagesReceivedHistoryAware++
            if (!isMaster) {
                sendFeedsWithNews(sender)
            } else {
                //Phase 3:
                requestFullFeedUpdates(sender)
            }
        }
        //phase 3 methods

        /**
         * this method asks for all news for all private feeds and pubs it has subscribed to (that are not hosted
         * by itself). it compares the newest sequence number it has in the feed and the newest sequence number
         * of the feed of the partner, if the partner has newer messages, it requests them from the partner
         */
        private fun requestFullFeedUpdates(sender: String) {
            var feedsOfPeer = availableFeedsByPeer.get(sender)
            if (feedsOfPeer != null) {
                var removeableFeeds = ArrayList<String>()
                for (f in feedsOfPeer!!) {
                    if(feeds.getFeed(f.split("::")[0]).type != "pub" || feeds.getFeed(f.split("::")[0]).owner != self.getSelf().pubKey) {
                        var feed = feeds.getFeed(f.split("::")[0])
                        var mostRecentSeqPeer: Long = f.split("::")[1].toLong()
                        var lastReceivedSeqFeed: Long = feed.last_received_message_seq
                        if (lastReceivedSeqFeed < mostRecentSeqPeer) {
                            Evaluator.packagesSentHistoryAware++
                            ConnectionManager.sendPackage(
                                sender,
                                PackageFactory.sendMessageRangeRequest(
                                    feed.key,
                                    lastReceivedSeqFeed
                                )
                            )

                        }
                    }
                    removeableFeeds.add(f)
                }
                feedsOfPeer.removeAll(removeableFeeds)
            }
            sendEndPhaseThreeFlag(sender)
        }

        /**
         * end the phase three for this device
         */
        private fun sendEndPhaseThreeFlag(sender: String) {
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(sender, PackageFactory.endPhaseThree())
        }

        /**
         * answer a message request by sending all messages that the partner has requested,
         * starting from the newest message the partner has in its feed
         */
        fun receiveRangeMessageRequest(sender: String, feedKey: String?, lowerLimit: Long) {
            Evaluator.packagesReceivedHistoryAware++
            var msgs = messages.getNewMessages(feedKey!!, lowerLimit)
            for (m in msgs) {
                Evaluator.packagesSentHistoryAware++
                ConnectionManager.sendPackage(sender, PackageFactory.sendMessage(m))
            }
        }

        /**
         * receive the requested messages and insert them in the corresponding feeds. if the message is part
         * of a pub this device is hosting, insert it in the pub feed as well. Furthermore, if the sync partner
         * itself is subscribed to one of the pubs, send it the new pub update directly, so both are up to date.
         * We also update the peer info to reflect the last messages of the pub that we have sent to the peer
         */
        fun receiveMessage(receivedMessage: Message, sender: String) {
            Evaluator.packagesReceivedHistoryAware++
            var insertiontime: Long = System.currentTimeMillis()
            messages.insert(receivedMessage)
            databaseAccessTimes.add(System.currentTimeMillis() - insertiontime)
            feeds.updateLastReceivedMessage(messages.getNewestMessage(receivedMessage.feed_key!!), receivedMessage.feed_key!!)
            var myPubs = feeds.getPubsByHostDevice(self.getSelf().pubKey!!)
            if (myPubs != null) {
                for (f in myPubs) {
                    if (peerInfos.isSubscribed(receivedMessage.publisher, f.key)) {
                        var insertiontimeTwo: Long = System.currentTimeMillis()
                        var helpMessage = Message(
                            message_id = 0,
                            sequence_Nr = messages.getNewestMessage(f.key) + 1,
                            signature = receivedMessage.signature,
                            feed_key = f.key,
                            content = receivedMessage.content,
                            timestamp = receivedMessage.timestamp,
                            publisher = receivedMessage.publisher
                        )
                        messages.insert(helpMessage)
                        databaseAccessTimes.add(System.currentTimeMillis() - insertiontimeTwo)
                        if(peerInfos.isSubscribed(peers.getPeerByWiFiAddress(sender)!!.foreign_public_key!!, f.key)) {
                            Evaluator.packagesSentHistoryAware++
                            ConnectionManager.sendPackage(
                                sender,
                                PackageFactory.sendPubUpdate(helpMessage)
                            )
                            peerInfos.updateLastSentMessage(peers.getPeerByWiFiAddress(sender)!!.foreign_public_key!!, f.key, messages.getNewestMessage(f.key))
                        }
                    }
                }
            }
        }

        /**
         * receive the signal to end phase three for this device. If this device is the instigator
         * of the synchronization (shown with the "isMaster" boolean) the protocol moves on to
         * phase 4, if it is the passive partner, this starts the message update process for this device
         */
        fun receiveEndPhaseThree(sender: String) {
            Evaluator.packagesReceivedHistoryAware++
            if (!isMaster) {
                requestFullFeedUpdates(sender)
            } else {
                sendLastSync(sender)
            }
        }

        /**
         * receive a pub update from the current synchronization partner, update the last
         * sent message to reflect that we have forwarded the message from Feed X to pub Y
         */
        fun receivePubUpdate(receivedMessage: Message, sender: String) {
            Evaluator.packagesReceivedHistoryAware++
            var insertiontime: Long = System.currentTimeMillis()
            messages.insert(receivedMessage)
            databaseAccessTimes.add(System.currentTimeMillis() - insertiontime)
            peerInfos.updateLastSentMessage(peers.getPeerByWiFiAddress(sender)!!.foreign_public_key!!, receivedMessage.feed_key!!, messages.getNewestMessage(receivedMessage.feed_key!!))
        }


        //phase 4 methods
        /**
         * receive a timestamp sync message, safe the last sync time with this device.
         */
        fun receiveLastSync(partner: String, lastSync: Long) {
            Evaluator.packagesReceivedHistoryAware++
            peers.setLastSync(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!, lastSync)
            if (NaiveSynchronization.done){
                Evaluator.syncTimeHistoryAware = System.currentTimeMillis() - timestamp
                for (t in databaseAccessTimes){
                    Evaluator.syncTimeHistoryAware -= t
                }
                Evaluator.evaluate()
            }
        }


        /**
         * used to synchronize the "last sync" variable across both partners
         */
        private fun sendLastSync(receiver: String) {
            var lastSync = System.currentTimeMillis()
            peers.setLastSync(peers.getPeerByWiFiAddress(receiver)!!.foreign_public_key!!, lastSync)
            Evaluator.packagesSentHistoryAware++
            ConnectionManager.sendPackage(receiver, PackageFactory.sendLastSync(lastSync))
        }


    }


}