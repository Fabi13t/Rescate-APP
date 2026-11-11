package com.rescate.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.rescate.app.data.model.Category
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.SearchFilters
import com.rescate.app.data.repository.PackRepository
import com.rescate.app.util.LocationHelper
import com.rescate.app.util.UiState
import com.rescate.app.util.launchState
import kotlinx.coroutines.Job

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PackRepository(app)
    val packs = MutableLiveData<UiState<List<Pack>>?>()
    var filters = SearchFilters()
        private set
    private var job: Job? = null

    fun load(newFilters: SearchFilters = filters) {
        filters = newFilters
        job?.cancel()
        val loc = LocationHelper.lastLocation
        job = launchState(packs) { repo.packs(filters, loc?.latitude, loc?.longitude) }
    }

    fun loadFavorites() {
        job?.cancel()
        job = launchState(packs) { repo.favorites() }
    }

    fun setCategory(category: Category?) = load(filters.copy(category = category))

    fun favoriteIds(): Set<Long> = com.rescate.app.data.local.RescateDbHelper.get(getApplication()).favoriteIds()

    fun toggleFavorite(pack: Pack): Boolean = repo.toggleFavorite(pack.id)

    init {
        load()
    }
}
