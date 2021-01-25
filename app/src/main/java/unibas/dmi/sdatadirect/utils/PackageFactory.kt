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
        DECLARE_FEED_KNOWN,
        INQUIRE_FEED_DETAILS,
        ANSWER_FEED_QUERY,
        END_PHASE_ONE
    }

    companion object {

        fun declareFeedKnown(f: Feed): ByteArray {
            var method = METHOD.DECLARE_FEED_KNOWN
            var feedKey = f.key
            var subscribed = f.subscribed
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" :  \"$feedKey\", \"subscribed\" : \"$subscribed\"}"
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }

        fun inquireFeedDetails(feedkey: String): ByteArray {
            var method = METHOD.INQUIRE_FEED_DETAILS
            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"$feedkey\"}"
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }

        fun answerFeedInquiry(f: Feed): ByteArray {
            var method = METHOD.ANSWER_FEED_QUERY

            val json = "{\"method\" : \"$method\"," +
                    "\"feedKey\" : \"${f.key}\", \"type\" : \"${f.type}\", \"host\" : " +
                    "\"${f.host}\", \"port\" : \"${f.port}\", \"subscribed\" : \"${f.subscribed}\"}"
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }

        fun endPhaseOne(): ByteArray {
            var method = METHOD.END_PHASE_ONE

            val json = "{\"method\" : \"$method\"}"
            val objectMapper = ObjectMapper()
            val node = objectMapper.readTree(json)
            val encodedNode = objectMapper.writeValueAsBytes(node)
            return encodedNode
        }


    }


}
