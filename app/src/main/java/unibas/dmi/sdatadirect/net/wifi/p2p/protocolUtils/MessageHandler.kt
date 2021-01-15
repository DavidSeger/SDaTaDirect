package unibas.dmi.sdatadirect.net.wifi.p2p.protocolUtils

import android.content.Context
import android.os.Message
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.peer.PeerViewModel
import unibas.dmi.sdatadirect.utils.PackageInterpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.concurrent.thread

/**
 * class that has a static message buffer, it verifies a message before it handles it to the interpreter
 */
class MessageHandler(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val cryptoHandler: CryptoHandler,
    val source_device_address: String?,
    val interpreter: PackageInterpreter = PackageInterpreter(context, activity, peerViewModel)
) {

    val objectMapper = ObjectMapper()
    val peer = peerViewModel.getPeerByWiFiAddress(source_device_address!!)

    companion object MsgBuffer {
        val msgQueue: Queue<ByteArray> = LinkedList<ByteArray>()
    }

    init {
        thread {
            while (true) {
                if (msgQueue.peek() != null) {
                    verify(msgQueue.remove())
                }
            }
        }
    }

    /**
     * verifies a received package and forwards it to the interpreter
     */
    fun verify(input: ByteArray) {
        val decodedNode = objectMapper.readTree(input)
        val receivedPackage: ByteArray =
            Base64.getUrlDecoder().decode(decodedNode.get("package").asText())
        val receivedType: ByteArray = Base64.getDecoder().decode(decodedNode.get("type").asText()) //for use later
        val receivedSignature: ByteArray =
            Base64.getDecoder().decode(decodedNode.get("signature").asText())

        val verification = cryptoHandler.verifySignature(
            receivedSignature,
            receivedPackage,
            peer?.foreign_public_key!!
        )

        if (verification) {
            val decryptedPackage = cryptoHandler.decryptAES(receivedPackage, peer?.shared_key)
            interpreter.interpret(decryptedPackage)
        } else {
            val message = Message.obtain()
            message.what = activity.VERIFICATION_FAILED
            activity.handler.sendMessage(message)
        }
    }

}