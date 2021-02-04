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
import unibas.dmi.sdatadirect.utils.PackageFactory

/**
 * receiver side of the reconciliation protocol
 */
class SetSynchronization() {
    companion object {
        val TAG = "SetSynchronization"
        var isMaster: Boolean = false
        val availableFeedsByPeer: HashMap<String, ArrayList<String>> = HashMap()
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
            if (feeds.isKnown(feedkey)) {
                if (!peerInfos.exists(peerAddress, feedkey)) {
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
                        peerInfos.subscribe(peerAddress, feedkey)
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
            peerInfos.insert(
                PeerInfo(
                    peer_key = peers.getPeerByWiFiAddress(peerAddress)!!.foreign_public_key!!,
                    feed_key = feedkey,
                    isSubscribed = false
                )
            )
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
            sendEndPhaseOneFlag(receiver)
        }

        fun receiveFeedInquiryAnswer(
            feed: Feed,
            subscribed: Boolean,
            sender: String
        ) {
            feeds.insert(feed)
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

        private fun sendEndPhaseOneFlag(receiver: String) {
            ConnectionManager.sendPackage(receiver, PackageFactory.endPhaseOne())
        }

        fun receiveEndPhaseOne(sender: String) {
            if (!isMaster) {
                sendFeedUpdates(sender)
            } else {
                //Phase 2: Tell the partner for which feeds he is subscribed to he has received new content since the last
                //meeting
                sendFeedsWithNews(sender)
            }
        }


        //Phase 2 methods

        private fun sendFeedsWithNews(partner: String) {
            var privateFeed = feeds.getFeedByOwner(self.getSelf().pubKey!!)
            var peer = peerInfos.getAllSubscribed(partner)
            var updateAlreadySent = 0
            //send updates for the feeds the partner device has subscribed to
            for (f in peer) {
                if (feeds.getFeed(f.feed_key).owner != peers.getPeerByWiFiAddress(partner)!!.foreign_public_key) {
                    if (f.feed_key == privateFeed.key) updateAlreadySent = 1
                    var fLastSeq = messages.getNewestMessage(f.feed_key)
                    if (f.lastSentMessage < fLastSeq) {
                        ConnectionManager.sendPackage(
                            partner,
                            PackageFactory.sendSeqNr(f.feed_key, fLastSeq)
                        )
                    }
                }
            }
            //send updates on all feeds that are subscribed to a pub hosted by the partner device where the partner device is not subscribed
            //to the original private feed owner
            for (f in feeds.getAllFeeds()){
                if (f.type == "priv") {
                 for (p in feeds.getPubsByHostDevice(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!)) {
                        if (peerInfos.isSubscribed(f.owner!!, p.key)){
                            if (!peerInfos.isSubscribed(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!, f.key)){
                                var fLastSeq = messages.getNewestMessage(f.key)
                                ConnectionManager.sendPackage(
                                    partner,
                                    PackageFactory.sendSeqNr(f.key, fLastSeq)
                                )
                            }
                        }
                    }
                }
            }

            //send updates of this devices feeds to pubs that are not subscribed to this device
            if(updateAlreadySent == 0) {
                for (p in feeds.getPubsByHostDevice(peers.getPeerByWiFiAddress(partner)!!.foreign_public_key!!)) {
                    if (p.subscribed!!) {
                        var fLastSeq = messages.getNewestMessage(privateFeed.key)
                        ConnectionManager.sendPackage(
                            partner,
                            PackageFactory.sendSeqNr(privateFeed.key, fLastSeq)
                        )
                    }
                }
            }


            //MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKWH0wxqIsPg8O6iRahixIFJGo0A0K9E/guMZJ1Shwt6UUXxIHqRCv4DWeKxz+2Q0xRN+mU9JIG5HAITU0NuFSfOOx3DypfTk2cSIBTOSnBygb8zyuF5J57uQ05REswlShOX/pMS4biI8iyi2Tjptkz3bbLCa+cvtfI+yEx4JphwIDAQAB
            sendEndPhaseTwoFlag(partner)
        }

        fun receiveFeedUpdateList(sender: String, feedKey: String?, lastSeq: Long) {
            if (availableFeedsByPeer.containsKey(sender)) {
                var feedList = availableFeedsByPeer.get(sender)
                feedList!!.add(feedKey!! + "::" + lastSeq)
            } else {
                var feedList: ArrayList<String> = ArrayList<String>()
                feedList.add(feedKey!! + "::" + lastSeq)
                availableFeedsByPeer.put(sender, feedList)
            }
        }

        private fun sendEndPhaseTwoFlag(receiver: String) {
            ConnectionManager.sendPackage(receiver, PackageFactory.endPhaseTwo())
        }

        fun receiveEndPhaseTwo(sender: String) {
            if (!isMaster) {
                sendFeedsWithNews(sender)
            } else {
                //Phase 3:
                requestFullFeedUpdates(sender)
            }
        }
        //phase 3 methods

        /**
         * this method is used automatically upon a connection with a peer, it
         * asks for all news for all private feeds it has subscribed to, and updates all
         * pubs it is hosting.
         */
        private fun requestFullFeedUpdates(sender: String) {
            var feedsOfPeer = availableFeedsByPeer.get(sender)
            if (feedsOfPeer != null) {
                for (f in feedsOfPeer!!) {
                    if(feeds.getFeed(f.split("::")[0]).type != "pub" || feeds.getFeed(f.split("::")[0]).owner != self.getSelf().pubKey) {
                        var feed = feeds.getFeed(f.split("::")[0])
                        var mostRecentSeqPeer: Long = f.split("::")[1].toLong()
                        var lastReceivedSeqFeed: Long = feed.last_received_message_seq
                        if (lastReceivedSeqFeed < mostRecentSeqPeer) {
                            ConnectionManager.sendPackage(
                                sender,
                                PackageFactory.sendMessageRangeRequest(
                                    feed.key,
                                    lastReceivedSeqFeed
                                )
                            )

                        }
                    }
                }
            }
            sendEndPhaseThreeFlag(sender)
        }

        private fun sendEndPhaseThreeFlag(sender: String) {
            ConnectionManager.sendPackage(sender, PackageFactory.endPhaseThree())
        }

        fun receiveRangeMessageRequest(sender: String, feedKey: String?, lowerLimit: Long) {
            var msgs = messages.getNewMessages(feedKey!!, lowerLimit)
            for (m in msgs) {
                ConnectionManager.sendPackage(sender, PackageFactory.sendMessage(m))
            }
            peerInfos.updateLastSentMessage(sender, feedKey, messages.getNewestMessage(feedKey))
        }

        fun receiveMessage(receivedMessage: Message, sender: String) {
            if (feeds.getFeed(receivedMessage.feed_key!!).subscribed!!) {
                messages.insert(receivedMessage)
            }
                var myPubs = feeds.getPubsByHostDevice(self.getSelf().pubKey!!)
            if (myPubs != null) {
                for (f in myPubs) {
                    if (peerInfos.isSubscribed(receivedMessage.publisher, f.key)) {
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
                        if(peerInfos.isSubscribed(peers.getPeerByWiFiAddress(sender)!!.foreign_public_key!!, f.key)) {
                            ConnectionManager.sendPackage(
                                sender,
                                PackageFactory.sendPubUpdate(helpMessage)
                            )
                            peerInfos.updateLastSentMessage(sender, f.key, messages.getNewestMessage(f.key))
                        }
                    }
                }
            }
        }

        fun receiveEndPhaseThree(sender: String) {
            if (!isMaster) {
                requestFullFeedUpdates(sender)
            } else {
                sendLastSync(sender)
            }
        }

        fun receivePubUpdate(receivedMessage: Message, sender: String) {
            messages.insert(receivedMessage)
        }


        //phase 4 methods
        fun receiveLastSync(partner: String, lastSync: Long) {
            peers.setLastSync(peers.getPeerByWiFiAddress(partner)!!.public_key!!, lastSync)
        }


        /**
         * used to synchronize the "last sync" variable across both partners
         */
        private fun sendLastSync(receiver: String) {
            var lastSync = System.currentTimeMillis()
            peers.setLastSync(peers.getPeerByWiFiAddress(receiver)!!.public_key!!, lastSync)
            ConnectionManager.sendPackage(receiver, PackageFactory.sendLastSync(lastSync))
        }


    }


}