package unibas.dmi.sdatadirect.utils

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils.NaiveSynchronization
import unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils.SetSynchronization
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.statistics.Evaluator
import unibas.dmi.sdatadirect.utils.PackageFactory.*
import unibas.dmi.sdatadirect.utils.PackageFactory.METHOD.SEND_FEED_UPDATE
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PackageInterpreter(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val objectMapper: ObjectMapper = ObjectMapper()
){
    val TAG = "PackageInterpreter"
    fun interpret(msg: ByteArray, sender: String){
        val node = objectMapper.readTree(msg)
        val method: String = node.get("method").asText()
        Log.d(TAG, method)

        if (method == SEND_FEED_UPDATE.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var subscribed = node.get("subscribed").asBoolean()
            SetSynchronization.receiveFeedUpdate(feedKey, subscribed, sender)
        }
        if (method == METHOD.INQUIRE_FEED_DETAILS.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            SetSynchronization.receiveFeedInquiry(feedKey, sender)
        }
        if (method == METHOD.ANSWER_FEED_QUERY.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var subscribed = node.get("subscribed").asBoolean()
            var host = node.get("host").asText()
            var port = node.get("port").asText()
            var type = node.get("type").asText()
            var owner = node.get("owner").asText()
            SetSynchronization.receiveFeedInquiryAnswer(
                Feed(
                    key = feedKey,
                    type = type,
                    host = host,
                    port = port,
                    owner = owner,
                    subscribed = false
            ), subscribed, sender
            )
        }
        if (method == METHOD.END_PHASE_ONE.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            SetSynchronization.receiveEndPhaseOne(sender)
        }
        if (method == METHOD.SEND_LAST_SYNC.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var lastSync = node.get("lastSync").asLong()
            SetSynchronization.receiveLastSync(sender, lastSync)
        }
        if (method == METHOD.END_PHASE_TWO.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            SetSynchronization.receiveEndPhaseTwo(sender)
        }
        if (method == METHOD.SEND_LAST_SEQ_NR.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var lastSeq = node.get("lastSeq").asLong()
            SetSynchronization.receiveFeedUpdateList(sender, feedKey, lastSeq)
        }
        if (method == METHOD.SEND_RANGE_MESSAGE_REQUEST.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var lowerLimit = node.get("lowerLimit").asLong()
            SetSynchronization.receiveRangeMessageRequest(sender, feedKey, lowerLimit)
        }
        if (method == METHOD.SEND_MESSAGE.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var sequenceNr = node.get("sequenceNr").asLong()
            var content = node.get("content").asText().toByteArray(Charsets.UTF_8)
            var publisher = node.get("publisher").asText()
            var signature = node.get("signature").asText()
            var timestamp = node.get("timestamp").asLong()
            var receivedMessage = Message(
                message_id = 0,
                sequence_Nr = sequenceNr,
                feed_key = feedKey,
                content = content,
                publisher = publisher,
                signature = signature,
                timestamp = timestamp
            )
            SetSynchronization.receiveMessage(receivedMessage, sender)
        }
        if (method == METHOD.END_PHASE_THREE.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            SetSynchronization.receiveEndPhaseThree(sender)
        }
        if (method == METHOD.SEND_PUB_UPDATE.name){
            Evaluator.bytesReceivedHistoryAware += node.size()
            var feedKey = node.get("feedKey").asText()
            var sequenceNr = node.get("sequenceNr").asLong()
            var content = node.get("content").asText().toByteArray(Charsets.UTF_8)
            var publisher = node.get("publisher").asText()
            var signature = node.get("signature").asText()
            var timestamp = node.get("timestamp").asLong()
            var receivedMessage = Message(
                message_id = 0,
                sequence_Nr = sequenceNr,
                feed_key = feedKey,
                content = content,
                publisher = publisher,
                signature = signature,
                timestamp = timestamp
            )
            SetSynchronization.receivePubUpdate(receivedMessage, sender)
        }
        if (method == METHOD.NAIVE_SEND_FEED.name){
            Evaluator.bytesReceivedNaive += node.size()
            var feedKey = node.get("feedKey").asText()
            var subscribed = node.get("subscribed").asBoolean()
            var host = node.get("host").asText()
            var port = node.get("port").asText()
            var type = node.get("type").asText()
            var owner = node.get("owner").asText()
            NaiveSynchronization.receiveFeed(
                Feed(
                    key = feedKey,
                    type = type,
                    host = host,
                    port = port,
                    owner = owner,
                    subscribed = false
                ), sender
            )
        }
        if (method == METHOD.NAIVE_SEND_RANGE_MESSAGE_REQUEST.name){
            Evaluator.bytesReceivedNaive += node.size()
            var feedKey = node.get("feedKey").asText()
            var lowerLimit = node.get("lowerLimit").asLong()
            NaiveSynchronization.receiveRangeMessageRequest(sender, feedKey, lowerLimit)
        }
        if (method == METHOD.NAIVE_SEND_MESSAGE.name){
            Evaluator.bytesReceivedNaive += node.size()
            var feedKey = node.get("feedKey").asText()
            var sequenceNr = node.get("sequenceNr").asLong()
            var content = node.get("content").asText().toByteArray(Charsets.UTF_8)
            var publisher = node.get("publisher").asText()
            var signature = node.get("signature").asText()
            var timestamp = node.get("timestamp").asLong()
            var receivedMessage = Message(
                message_id = 0,
                sequence_Nr = sequenceNr,
                feed_key = feedKey,
                content = content,
                publisher = publisher,
                signature = signature,
                timestamp = timestamp
            )
            NaiveSynchronization.receiveMessage(receivedMessage, sender)
        }

    }

    /**
     * for getting the package size
     */
    fun byteArrayToSize(byteBarray: ByteArray?): Int {
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }
}