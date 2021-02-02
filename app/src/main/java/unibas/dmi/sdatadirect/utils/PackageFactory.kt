package unibas.dmi.sdatadirect.utils

import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.database.Feed

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
        SEND_FEED_WITH_NEWS,
        END_PHASE_TWO
    }

    companion object {

        fun sendFeedUpdate(f: Feed): ByteArray {
            var method = METHOD.SEND_FEED_UPDATE
            var feedKey = f.key
            var subscribed = f.subscribed
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" :  \"$feedKey\", \"subscribed\" : \"$subscribed\"}"
            return encode(json)
        }

        fun inquireFeedDetails(feedkey: String): ByteArray {
            var method = METHOD.INQUIRE_FEED_DETAILS
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"$feedkey\"}"
            return encode(json)
        }

        fun answerFeedInquiry(f: Feed): ByteArray {
            var method = METHOD.ANSWER_FEED_QUERY

            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"${f.key}\", \"type\" : \"${f.type}\", \"host\" : " +
                    "\"${f.host}\", \"port\" : \"${f.port}\", \"subscribed\" : \"${f.subscribed}\"," +
                    "\"owner\" : \"${f.owner}\"}"
            return encode(json)
        }

        fun endPhaseOne(): ByteArray {
            var method = METHOD.END_PHASE_ONE

            val json = "{\"method\" : \"$method\"}"
            return encode(json)
        }

        fun sendLastSync(lastSync: Long): ByteArray {
            var method = METHOD.SEND_LAST_SYNC
            val json = "{\"method\" : \"$method\", \"lastSync\" : \"$lastSync\"}"
            return encode(json)
        }

        fun sendFeedsWithNews(f: Feed): ByteArray {
            var method = METHOD.SEND_FEED_WITH_NEWS
            val json = "{\"method\" : \"$method\", \"feedKey\" : \"${f.key}\"}"
            return encode(json)

        }

        fun endPhaseTwo(): ByteArray {
            var method = METHOD.END_PHASE_TWO

            val json = "{\"method\" : \"$method\"}"
            return encode(json)
        }

        private fun encode(json: String): ByteArray{
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }



    }


}
