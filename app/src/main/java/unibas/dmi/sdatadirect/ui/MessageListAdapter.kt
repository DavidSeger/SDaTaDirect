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
import unibas.dmi.sdatadirect.database.Message
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

class MessageListAdapter(context: Context, val resource: Int, val messages: ArrayList<Message>):
    ArrayAdapter<Message>(context, resource, messages) {

    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return messages.size
    }

    override fun getItem(position: Int): Message? {
        return messages[position]
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val rowView = layoutInflater.inflate(R.layout.device_adapter_view, parent, false)
        val msg = rowView.findViewById<TextView>(R.id.deviceName)
        val info = rowView.findViewById<TextView>(R.id.deviceAddress)
        msg.text = messages[position].content!!.toString(Charsets.UTF_8)
        info.text = "published by " + messages[position].publisher + " on " + Date(Timestamp(messages[position].timestamp!!).time).toString()
        return rowView
    }

}