package unibas.dmi.sdatadirect.utils

import android.content.Context
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import unibas.dmi.sdatadirect.MainActivity
import unibas.dmi.sdatadirect.peer.PeerViewModel
import java.util.*

class PackageInterpreter(
    val context: Context,
    val activity: MainActivity,
    val peerViewModel: PeerViewModel,
    val objectMapper: ObjectMapper = ObjectMapper()
){

    fun interpret(msg: ByteArray){
        val node = objectMapper.readTree(msg)
        val method: String = node.get("method").asText()
        val body: String = node.get("body").asText()
        if (method == "SEND_STRING"){
            activity.runOnUiThread {
                Toast.makeText(activity, body, Toast.LENGTH_LONG).show()
            }
        }

    }
}