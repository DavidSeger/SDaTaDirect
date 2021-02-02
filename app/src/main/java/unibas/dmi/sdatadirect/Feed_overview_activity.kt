package unibas.dmi.sdatadirect

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.content.SelfViewModel
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Message
import unibas.dmi.sdatadirect.ui.FeedListAdapter


class Feed_overview_activity : AppCompatActivity() {
    lateinit var listView: ListView
    lateinit var addButton: Button
    lateinit var inputText: EditText

    companion object {
        var feeds: ArrayList<Feed> = ArrayList()
        val tag = "FEED_OVERVIEW_ACTIVITY"
        lateinit var feedViewModel: FeedViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_overview_activity)
        addButton = findViewById(R.id.addPub)
        feedViewModel = EventBus.getDefault().getStickyEvent(FeedViewModel::class.java)
        listView = findViewById(R.id.feedView)
        var messages = EventBus.getDefault().getStickyEvent(MessageViewModel::class.java)
        var selfViewModel = EventBus.getDefault().getStickyEvent(SelfViewModel::class.java)
        if (feedViewModel.getAllFeeds() != null) {
            feeds = feedViewModel.getAllFeeds().toCollection(ArrayList())
            feeds.sortBy { it.key }
        }
        var adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
        listView.adapter = adapter
        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            var f = adapter.getItem(i)!!
            if(f.subscribed!!) {
                var canPublish = f.owner == selfViewModel.getSelf().pubKey && f.type == "priv"
                val viewMessagesIntent = Intent(this, MessageActivity::class.java).apply {
                    EventBus.getDefault()
                        .postSticky(
                            messages
                        )
                    EventBus.getDefault()
                        .postSticky(EventBus.getDefault().getStickyEvent(FeedViewModel::class.java))
                    EventBus.getDefault()
                        .postSticky(EventBus.getDefault().getStickyEvent(SelfViewModel::class.java))
                    putExtra(MessageActivity.feedkeyTag, f.key)
                    putExtra(MessageActivity.feedPosTag, i)
                    putExtra(MessageActivity.canPublishTag, canPublish)
                    putExtra(MessageActivity.feedTypeTag, f.type)
                    putExtra(MessageActivity.feedSizeTag, messages.getFullFeed(f.key).size)
                }
                startActivityForResult(viewMessagesIntent, 1)
            } else{
                var addDialog = Dialog(this)
                addDialog.setContentView(R.layout.sample_subscribe_dialog)
                var subButton: Button = addDialog.findViewById(R.id.subscribeButton)
                var cancelButton: Button = addDialog.findViewById(R.id.cancelButton)
                subButton.setOnClickListener(){
                    feedViewModel.subscribe(f.key)
                    feeds.removeAt(i)
                    feeds.sortBy { it.key }
                    listView.adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
                    addDialog.cancel()
                }
                cancelButton.setOnClickListener(){
                    addDialog.cancel()
                }
                addDialog.show()
            }

        }
        addButton.setOnClickListener() {
            var addDialog = Dialog(this)
            addDialog.setContentView(R.layout.sample_add_feed_dialog)
            // val linearLayout = addDialog.findViewById<LinearLayout>(R.id.linearLayout)
            val saveBtn: Button = addDialog.findViewById(R.id.saveFeedButton)
            val nameField: EditText = addDialog.findViewById(R.id.name)
            val hostField: EditText = addDialog.findViewById(R.id.host)
            val portField: EditText = addDialog.findViewById(R.id.port)
            val subscribedSwitch: Switch = addDialog.findViewById(R.id.subscribed)
            addDialog.show()
            saveBtn.setOnClickListener {
                var feed = Feed(
                    key = nameField.text.toString(),
                    type = "pub",
                    host = hostField.text.toString(),
                    port = portField.text.toString(),
                    subscribed = subscribedSwitch.isChecked,
                    owner = selfViewModel.getSelf().pubKey
                )
                feedViewModel.insert(feed)
                var msgs = messages.getFullFeed(feedViewModel.getFeedByOwner(selfViewModel.getSelf().pubKey!!).key)
                /**
                 * replicate own messages to put into pub
                 */
                var msgCounter = 0
                for (m in msgs){
                    msgCounter++
                    var copyMessage = Message(
                        message_id = 0,
                        sequence_Nr = msgCounter.toLong(),
                        signature = m.signature,
                        feed_key = feed.key,
                        content = m.content,
                        timestamp = m.timestamp,
                        publisher = m.publisher
                    )
                    messages.insert(copyMessage)
                }
                feeds.sortBy { it.key }
                listView.adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
                addDialog.cancel()
            }

        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            feeds.sortBy { it.key }
            listView.adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)

        }

    }

}