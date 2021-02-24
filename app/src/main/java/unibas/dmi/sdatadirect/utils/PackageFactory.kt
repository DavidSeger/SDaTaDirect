package unibas.dmi.sdatadirect.utils

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.statistics.Evaluator

/**
 * packages are Json objects containing a header signifying a method
 * and an optional body containing information. For now only a package
 * to send Strings across the devices will be implemented
 */
class PackageFactory {
    enum class METHOD {
        SEND_FEED_UPDATE,
        INQUIRE_FEED_DETAILS,
        ANSWER_FEED_QUERY,
        END_PHASE_ONE,
        SEND_LAST_SYNC,
        SEND_LAST_SEQ_NR,
        END_PHASE_TWO,
        SEND_RANGE_MESSAGE_REQUEST,
        SEND_MESSAGE,
        END_PHASE_THREE,
        SEND_PUB_UPDATE,
        NAIVE_SEND_FEED,
        NAIVE_SEND_RANGE_MESSAGE_REQUEST,
        NAIVE_SEND_MESSAGE
    }

    companion object {

        fun sendFeedUpdate(f: Feed): ByteArray {
            var method = METHOD.SEND_FEED_UPDATE
            var feedKey = f.key
            var subscribed = f.subscribed
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" :  \"$feedKey\", \"subscribed\" : \"$subscribed\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun inquireFeedDetails(feedkey: String): ByteArray {
            var method = METHOD.INQUIRE_FEED_DETAILS
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"$feedkey\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun answerFeedInquiry(f: Feed): ByteArray {
            var method = METHOD.ANSWER_FEED_QUERY

            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"${f.key}\", \"type\" : \"${f.type}\", \"host\" : " +
                    "\"${f.host}\", \"port\" : \"${f.port}\", \"subscribed\" : \"${f.subscribed}\"," +
                    "\"owner\" : \"${f.owner}\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun endPhaseOne(): ByteArray {
            var method = METHOD.END_PHASE_ONE

            val json = "{\"method\" : \"$method\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun sendLastSync(lastSync: Long): ByteArray {
            var method = METHOD.SEND_LAST_SYNC
            val json = "{\"method\" : \"$method\", \"lastSync\" : \"$lastSync\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }


        fun endPhaseTwo(): ByteArray {
            var method = METHOD.END_PHASE_TWO

            val json = "{\"method\" : \"$method\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun sendSeqNr(feedKey: String, fLastSeq: Long): ByteArray {
            var method = METHOD.SEND_LAST_SEQ_NR
            val json = "{\"method\" : \"$method\", \"feedKey\" : \"$feedKey\", \"lastSeq\" : \"$fLastSeq\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun sendMessageRangeRequest(feedKey: String, lastReceivedSeqFeed: Long): ByteArray {
            var method = METHOD.SEND_RANGE_MESSAGE_REQUEST
            val json = "{\"method\" : \"$method\", \"feedKey\" : \"$feedKey\", \"lowerLimit\" : \"$lastReceivedSeqFeed\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun sendMessage(m: Message): ByteArray {
            var method = METHOD.SEND_MESSAGE
            val json = "{\"method\" : \"$method\", \"sequenceNr\" : \"${m.sequence_Nr}\", \"feedKey\" : \"${m.feed_key}\", " +
                    "\"content\" : \"${m.content!!.toString(Charsets.UTF_8)}\", \"publisher\" : \"${m.publisher}\", \"signature\" : \"${m.signature}\", " +
                    "\"timestamp\" : \"${m.timestamp}\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }
        fun endPhaseThree(): ByteArray {
            var method = METHOD.END_PHASE_THREE
            val json = "{\"method\" : \"$method\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }

        fun sendPubUpdate(m: Message): ByteArray {
            var method = METHOD.SEND_PUB_UPDATE
            val json = "{\"method\" : \"$method\", \"sequenceNr\" : \"${m.sequence_Nr}\", \"feedKey\" : \"${m.feed_key}\", " +
                    "\"content\" : \"${m.content!!.toString(Charsets.UTF_8)}\", \"publisher\" : \"${m.publisher}\", \"signature\" : \"${m.signature}\", " +
                    "\"timestamp\" : \"${m.timestamp}\"}"
            var pack = encode(json)
            Evaluator.bytesSentHistoryAware += pack.size
            return pack
        }


        fun naiveSendFeed(f: Feed): ByteArray {
            var method = METHOD.NAIVE_SEND_FEED
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"${f.key}\", \"type\" : \"${f.type}\", \"host\" : " +
                    "\"${f.host}\", \"port\" : \"${f.port}\", \"subscribed\" : \"${f.subscribed}\"," +
                    "\"owner\" : \"${f.owner}\"}"
            var pack = encode(json)
            Evaluator.bytesSentNaive += pack.size
            return pack
        }
        fun naiveRequestMessages(key: String, newestMsg: Long): ByteArray {
            var method = METHOD.NAIVE_SEND_RANGE_MESSAGE_REQUEST
            val json = "{\"method\" : \"$method\", \"feedKey\" : \"$key\", \"lowerLimit\" : \"$newestMsg\"}"
            var pack = encode(json)
            Evaluator.bytesSentNaive += pack.size
            return pack
        }


        fun naiveSendMessages(m: Message): ByteArray {
            var method = METHOD.NAIVE_SEND_MESSAGE
            val json = "{\"method\" : \"$method\", \"sequenceNr\" : \"${m.sequence_Nr}\", \"feedKey\" : \"${m.feed_key}\", " +
                    "\"content\" : \"${m.content!!.toString(Charsets.UTF_8)}\", \"publisher\" : \"${m.publisher}\", \"signature\" : \"${m.signature}\", " +
                    "\"timestamp\" : \"${m.timestamp}\"}"
            var pack = encode(json)
            Evaluator.bytesSentNaive += pack.size
            return pack
        }


        private fun encode(json: String): ByteArray{
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }


    }


}
