package unibas.dmi.sdatadirect.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.database.Peer

/**
 * List adapter to list the entries from the database
 */
class PeerListAdapter(context: Context)
    : RecyclerView.Adapter<PeerListAdapter.PeerViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var peers = emptyList<Peer>()

    inner class PeerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val peerItemView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val itemView = inflater.inflate(R.layout.peerview_item, parent, false)
        return PeerViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return peers.size
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val current = peers[position]
        holder.peerItemView.text = current.name
    }

    internal fun setPeers(peers: List<Peer>) {
        this.peers = peers
        notifyDataSetChanged()
    }
}