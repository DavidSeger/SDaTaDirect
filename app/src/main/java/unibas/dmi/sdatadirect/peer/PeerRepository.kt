package unibas.dmi.sdatadirect.peer

import androidx.lifecycle.LiveData
import unibas.dmi.sdatadirect.database.Peer
import unibas.dmi.sdatadirect.database.PeerDao

class PeerRepository(private val peerDao: PeerDao) {

    val allPeers: LiveData<List<Peer>> = peerDao.getAll()

    suspend fun insert(peer: Peer) {
        peerDao.insert(peer)
    }
}