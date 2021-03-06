package unibas.dmi.sdatadirect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.EventLog
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
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
        val feedPosTag = "UNIBAS_SDATA_FEEDPOS"
        val feedTypeTag = "UNIBAS_SDATA_FEEDTYPE"
        val canPublishTag = "UNIBAS_SDATA_CANPUBLISH"
        val feedSizeTag = "UNIBAS_SDATA_FEEDSIZE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        feedkey = intent.getStringExtra(feedkeyTag)
        var messageView = EventBus.getDefault().getStickyEvent(MessageViewModel::class.java)
        val selfView = EventBus.getDefault().getStickyEvent(SelfViewModel::class.java)
        val feedView = EventBus.getDefault().getStickyEvent(FeedViewModel::class.java)
        var msgview: ListView = findViewById(R.id.messages)
        msgview.isClickable = false
        var type = intent.getStringExtra(feedTypeTag)
        var canPublish = intent.getBooleanExtra(canPublishTag, false)
        var msgPublish: EditText = findViewById(R.id.publishText)
        var publishBtn: Button = findViewById(R.id.publishButton)
        if (!canPublish){
            msgPublish.visibility = View.INVISIBLE
            publishBtn.visibility = View.INVISIBLE
        }
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
                    sequence_Nr = messageView.getNewestMessage(feedkey) + 1,
                    signature = Base64.getEncoder().encodeToString(crypto.createSignature(msgPublish.text.toString().toByteArray(Charsets.UTF_8), selfView.getSelf().privKey!!)),
                    feed_key = feedkey,
                    content = msgPublish.text.toString().toByteArray(charset = Charsets.UTF_8),
                    timestamp = System.currentTimeMillis(),
                    publisher = selfView.getSelf().pubKey!!
                )
                messageView.insert(testMessage)
                message.add(testMessage)
                var myPubs = feedView.getPubsByHostDevice(selfView.getSelf().pubKey!!)
                for (f in myPubs){
                    if (f.subscribed!!) {
                        var copyMessage = Message (
                            message_id = 0,
                            sequence_Nr = messageView.getNewestMessage(f.key) + 1,
                            signature = Base64.getEncoder().encodeToString(crypto.createSignature(msgPublish.text.toString().toByteArray(Charsets.UTF_8), selfView.getSelf().privKey!!)),
                            feed_key = f.key,
                            content = msgPublish.text.toString().toByteArray(charset = Charsets.UTF_8),
                            timestamp = System.currentTimeMillis(),
                            publisher = selfView.getSelf().pubKey!!
                        )
                        messageView.insert(copyMessage)
                    }
                }
                msgPublish.text.clear()
                hideKeyboard(this)
                message.sortBy { it.sequence_Nr }
                adapter.notifyDataSetChanged()
            }
        }
        var unsub: Button = findViewById(R.id.unsubscribeButton)
        unsub.setOnClickListener(){
            Feed_overview_activity.feeds.removeAt(intent.getIntExtra(feedPosTag, -1))
            EventBus.getDefault().getStickyEvent(FeedViewModel::class.java).unsubscribe(feedkey)
            val returnIntent = Intent()
            var i = intent.getIntExtra(feedPosTag, -1)
            returnIntent.putExtra("result", i)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
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