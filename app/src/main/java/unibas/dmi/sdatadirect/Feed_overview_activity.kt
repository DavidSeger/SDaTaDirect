package unibas.dmi.sdatadirect

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_feed_overview_activity.*
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.content.MessageViewModel
import unibas.dmi.sdatadirect.content.SelfViewModel
import unibas.dmi.sdatadirect.database.Feed
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
        addButton = findViewById(R.id.addFeed)
        feedViewModel = EventBus.getDefault().getStickyEvent(FeedViewModel::class.java)
        listView = findViewById(R.id.feedView)
        if (feedViewModel.getAllFeeds() != null) {
            feeds = feedViewModel.getAllFeeds().toCollection(ArrayList())
            feeds.sortBy { it.key }
        }
        var adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
        listView.adapter = adapter
        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            var f = adapter.getItem(i)!!
            if(f.subscribed!!) {
                val viewMessagesIntent = Intent(this, MessageActivity::class.java).apply {
                    EventBus.getDefault()
                        .postSticky(
                            EventBus.getDefault().getStickyEvent(MessageViewModel::class.java)
                        )
                    EventBus.getDefault()
                        .postSticky(EventBus.getDefault().getStickyEvent(FeedViewModel::class.java))
                    EventBus.getDefault()
                        .postSticky(EventBus.getDefault().getStickyEvent(SelfViewModel::class.java))
                    putExtra(MessageActivity.feedkeyTag, f.key)
                    putExtra(MessageActivity.feedPosTag, i)
                    putExtra(MessageActivity.feedOwnerTag, f.owner)
                    putExtra(MessageActivity.feedTypeTag, f.type)
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
            val type: Switch = addDialog.findViewById(R.id.type)
            val hostField: EditText = addDialog.findViewById(R.id.host)
            val portField: EditText = addDialog.findViewById(R.id.port)
            val subscribedSwitch: Switch = addDialog.findViewById(R.id.subscribed)
            addDialog.show()
            saveBtn.setOnClickListener {
                var feed = Feed(
                    key = nameField.text.toString(),
                    type = if(type.isChecked) "Pub" else "Private",
                    host = hostField.text.toString(),
                    port = portField.text.toString(),
                    subscribed = subscribedSwitch.isChecked,
                    owner = true
                )
                feedViewModel.insert(feed)
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