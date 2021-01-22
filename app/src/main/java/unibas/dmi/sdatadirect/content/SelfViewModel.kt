package unibas.dmi.sdatadirect.content

import unibas.dmi.sdatadirect.ui.FeedListAdapter
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import unibas.dmi.sdatadirect.database.AppDatabase
import unibas.dmi.sdatadirect.database.Feed
import unibas.dmi.sdatadirect.database.Self

class SelfViewModel(application: Application): AndroidViewModel(application) {

    private val repository: SelfRepository

    init {
        val selfDao = AppDatabase.getDatabase(application, viewModelScope).selfDao()
        repository = SelfRepository(selfDao)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(self: Self) =viewModelScope.launch(Dispatchers.IO) {
        repository.insert(self)
    }
    fun getSelf():Self{
        return repository.getSelf()
    }
}