package unibas.dmi.sdatadirect

import android.os.Bundle
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.ui.MessageListAdapter

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
        var testMessage = Message(
            message_id = (message.size + 1).toLong(),
            signature = "TestSignature " + feedkey,
            feed_key = feedkey,
            content = "This is a test message".toByteArray(charset = Charsets.UTF_8),
            timestamp = System.currentTimeMillis()
        )
        messageView.insert(testMessage)
        var msgview: ListView = findViewById(R.id.messages)
        msgview.isClickable = false
        var msgPublish: EditText = findViewById(R.id.publishText)
        if (messageView.getFullFeed(feedkey) != null){
            message = messageView.getFullFeed(feedkey)
        }
        var adapter = MessageListAdapter(this, R.id.device_list_view, message)
        msgview.adapter = adapter
    }
}