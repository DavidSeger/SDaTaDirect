package unibas.dmi.sdatadirect

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.content.SelfViewModel
import unibas.dmi.sdatadirect.crypto.CryptoHandler
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.ui.MessageListAdapter
import java.util.*
import kotlin.collections.ArrayList


class MessageActivity(): AppCompatActivity(){
    lateinit var feedkey: String
    companion object{
        var message: ArrayList<Message> = ArrayList()
        val tag = "MESSAGE_ACTIVITY_VIEW"
        val feedkeyTag = "UNIBAS_SDATA_FEEDKEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        feedkey = intent.getStringExtra(feedkeyTag)
        var messageView = EventBus.getDefault().getStickyEvent(MessageViewModel::class.java)
        val selfView = EventBus.getDefault().getStickyEvent(SelfViewModel::class.java)
        var msgview: ListView = findViewById(R.id.messages)
        msgview.isClickable = false
        var msgPublish: EditText = findViewById(R.id.publishText)
        var publishBtn: Button = findViewById(R.id.publishButton)
        if (messageView.getFullFeed(feedkey) != null){
            message = messageView.getFullFeed(feedkey)

        }
        var adapter = MessageListAdapter(this, R.id.device_list_view, message)
        msgview.adapter = adapter
        publishBtn.setOnClickListener{
            if (msgPublish.text.toString() != ""){

                var crypto = EventBus.getDefault().getStickyEvent(CryptoHandler::class.java)
                var testMessage = Message(
                    message_id = 0,
                    signature = Base64.getEncoder().encodeToString(crypto.createSignature(msgPublish.text.toString().toByteArray(Charsets.UTF_8), selfView.getSelf().privKey!!)),
                    feed_key = feedkey,
                    content = msgPublish.text.toString().toByteArray(charset = Charsets.UTF_8),
                    timestamp = System.currentTimeMillis()
                )
                msgPublish.text.clear()
                hideKeyboard(this)

                messageView.insert(testMessage)
                message.add(testMessage)
                adapter.notifyDataSetChanged()
            }
        }
    }
    fun hideKeyboard(activity: Activity) {
        val inputManager = activity
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val currentFocusedView = activity.currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(
                currentFocusedView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }


}