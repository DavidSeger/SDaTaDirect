package unibas.dmi.sdatadirect

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.greenrobot.eventbus.EventBus
import unibas.dmi.sdatadirect.content.FeedViewModel
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.ui.FeedListAdapter


class feed_overview_activity : AppCompatActivity() {
    lateinit var listView: ListView
    companion object{
        val feeds: ArrayList<Feed> = ArrayList()
        val tag = "FEED_OVERVIEW_ACTIVITY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_overview_activity)
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
        var adapter = FeedListAdapter(this, R.layout.device_adapter_view, feeds)
        listView.adapter = adapter
        listView.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->

        }
        
    }

}