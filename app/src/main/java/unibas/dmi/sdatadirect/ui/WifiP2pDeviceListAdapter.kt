package unibas.dmi.sdatadirect.ui

import android.content.Context
import android.graphics.Typeface
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import unibas.dmi.sdatadirect.R

class WifiP2pDeviceListAdapter(context: Context, val resource: Int, val devices: MutableList<WifiP2pDevice>):
    ArrayAdapter<WifiP2pDevice>(context, resource, devices) {

    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val tempConvertView = layoutInflater.inflate(resource, parent, false)
        val device: WifiP2pDevice? = devices[position]

        val deviceName: TextView = tempConvertView.findViewById(R.id.deviceName)
        val deviceAddress: TextView = tempConvertView.findViewById(R.id.deviceAddress)

        deviceName.text = device?.deviceName
        deviceName.setTypeface(null, Typeface.BOLD)
        deviceAddress.text = device?.deviceAddress

        return tempConvertView
    }
}