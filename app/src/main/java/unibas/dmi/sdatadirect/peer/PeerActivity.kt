package unibas.dmi.sdatadirect.peer

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import unibas.dmi.sdatadirect.R
import unibas.dmi.sdatadirect.ui.PeerListAdapter

class PeerActivity: AppCompatActivity() {

    //private lateinit var peerViewModel: PeerViewModel
    private lateinit var backBtn: Button
    private lateinit var deleteAllBtn: Button
    private lateinit var peerViewModel: PeerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer)
        peerViewModel = ViewModelProvider(this).get(PeerViewModel::class.java)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
        val adapter = PeerListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        //peerViewModel = ViewModelProvider(this).get(PeerViewModel::class.java)
        peerViewModel.allPeers.observe(this, Observer { peers ->
            peers?.let { adapter.setPeers(it) }
        })

        backBtn = findViewById(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        deleteAllBtn = findViewById(R.id.deleteBtn)
        deleteAllBtn.setOnClickListener { peerViewModel.deleteAllPeers() }
    }
}
