package unibas.dmi.sdatadirect.utils

import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.utils.PackageFactory.METHOD.*

/**
 * packages are Json objects containing a header signifying a method
 * and an optional body containing information. For now only a package
 * to send Strings across the devices will be implemented
 */
class PackageFactory {
    enum class METHOD {
        `SEND_STRING`
    }

    companion object {

        /**
         *
         */
        fun buildPackage(method: METHOD, msg: String): ByteArray {
            if (method == `SEND_STRING`) {
                val json = "{\"method\" : \"${`SEND_STRING`}\"," +
                        "\"body\" : \"$msg\"}"
                val objectMapper = ObjectMapper()
                val node = objectMapper.readTree(json)
                val encodedNode = objectMapper.writeValueAsBytes(node)
                return encodedNode
            }
            return "invalid method received".toByteArray(Charsets.UTF_8)
        }

    }

}

/**
 * used to test the package building
 */
class Test {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.out.println(PackageFactory.buildPackage(`SEND_STRING`, "suck my kiss").toString(Charsets.UTF_8))
        }
    }
}