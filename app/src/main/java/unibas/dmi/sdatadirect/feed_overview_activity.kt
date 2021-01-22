package unibas.dmi.sdatadirect

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.ui.FeedListAdapter


class feed_overview_activity : AppCompatActivity() {
    lateinit var listView: ListView
    lateinit var addButton: Button
    lateinit var inputText: EditText
    companion object{
        var feeds: ArrayList<Feed> = ArrayList()
        val tag = "FEED_OVERVIEW_ACTIVITY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_overview_activity)
        addButton = findViewById(R.id.addFeed)
        //for testing purposes
        var feed = Feed(
            key = "TestFeed",
            type = "pub",
            host = "unibas.SDataDirect",
            port = "8888",
            subscribed = true
        )
        var feedView = EventBus.getDefault().getStickyEvent(FeedViewModel::class.java)
        feedView.insert(feed)
        listView = findViewById(R.id.feedView)
        if(feedView.getAllFeeds() != null) {
            feeds = feedView.getAllFeeds()
        }
        var adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
        listView.adapter = adapter
        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->

        }
        addButton.setOnClickListener(){
            var addDialog = Dialog(this)
            addDialog.setContentView(R.layout.sample_add_feed_dialog)
           // val linearLayout = addDialog.findViewById<LinearLayout>(R.id.linearLayout)
            val saveBtn: Button = addDialog.findViewById(R.id.saveFeedButton)
            val nameField: EditText = addDialog.findViewById(R.id.name)
            val typeField: EditText = addDialog.findViewById(R.id.type)
            val hostField: EditText = addDialog.findViewById(R.id.host)
            val portField: EditText = addDialog.findViewById(R.id.port)
            val subscribedSwitch: Switch = addDialog.findViewById(R.id.subscribed)
            addDialog.show()
            saveBtn.setOnClickListener {
                var feed = Feed(
                    key = nameField.text.toString(),
                    type = typeField.text.toString(),
                    host = hostField.text.toString(),
                    port = portField.text.toString(),
                    subscribed = subscribedSwitch.isChecked
                )
                feedView.insert(feed)
                listView.adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
                addDialog.cancel()
            }
           // linearLayou

        }
        
    }

}