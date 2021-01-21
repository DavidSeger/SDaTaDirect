package unibas.dmi.sdatadirect.ui

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.database.Feed

class FeedListAdapter(context: Context, val resource: Int, val feeds: ArrayList<Feed>):
    ArrayAdapter<Feed>(context, resource, feeds) {

    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return feeds.size
    }

    override fun getItem(position: Int): Feed? {
        return feeds[position]
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val rowView = layoutInflater.inflate(R.layout.device_adapter_view, parent, false)
        val name = rowView.findViewById<TextView>(R.id.deviceName)
        val msg = rowView.findViewById<TextView>(R.id.deviceAddress)
        name.text = feeds[position].key
        msg.text = if(feeds[position].subscribed!!) "subscribed" else "not subscribed"
        return rowView
    }

}