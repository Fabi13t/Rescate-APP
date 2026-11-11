package com.rescate.app.data.repository

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rescate.app.BuildConfig
import com.rescate.app.data.demo.DemoData
import com.rescate.app.data.local.RescateDbHelper
import com.rescate.app.data.model.Pack
import com.rescate.app.data.model.SearchFilters
import com.rescate.app.data.model.Store
import com.rescate.app.data.remote.ApiClient
import com.rescate.app.data.remote.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext




class PackRepository(context: Context) {

    private val db = RescateDbHelper.get(context)
    private val api by lazy { ApiClient.get(context) }
    private val gson = Gson()

    suspend fun packs(filters: SearchFilters, myLat: Double?, myLon: Double?): List<Pack> =
        withContext(Dispatchers.IO) {
            val all: List<Pack> = if (BuildConfig.DEMO_MODE) {
                delay(800)
                DemoData.packs.filter { it.status.name == "EN_PROCESO" }
            } else {
                try {
                    api.packs(myLat ?: DEFAULT_LAT, myLon ?: DEFAULT_LON, filters.maxKm,
                        filters.category?.name, filters.query.ifBlank { null })
                        .also { db.saveFeed(gson.toJson(it)) }
                } catch (e: Exception) {
                     
                    val cached = db.loadFeed() ?: throw e
                    gson.fromJson<List<Pack>>(cached, object : TypeToken<List<Pack>>() {}.type)
                }
            }
             
            if (!BuildConfig.DEMO_MODE && myLat != null && myLon != null) all.forEach { it.distanceMeters = distance(myLat, myLon, it.store) }
            all.filter { p ->
                (filters.category == null || p.category == filters.category) &&
                    (filters.query.isBlank() ||
                        p.title.contains(filters.query, true) ||
                        p.store.name.contains(filters.query, true) ||
                        p.category.label.contains(filters.query, true)) &&
                    p.distanceMeters <= filters.maxKm * 1000 &&
                    p.rescuePrice <= filters.maxPrice &&
                    (!filters.onlyWithStock || p.stock > 0)
            }.sortedBy { it.distanceMeters }
        }

    suspend fun pack(id: Long): Pack = withContext(Dispatchers.IO) {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            DemoData.packs.firstOrNull { it.id == id } ?: throw AppException("Este pack ya no está disponible.")
        } else api.pack(id)
    }

    fun stores(): List<Store> = DemoData.stores

    fun packsOfStore(storeId: Long): List<Pack> = DemoData.packs.filter { it.store.id == storeId && it.stock > 0 }

    suspend fun favorites(): List<Pack> = withContext(Dispatchers.IO) {
        val ids = db.favoriteIds()
        delay(300)
        DemoData.packs.filter { it.id in ids }
    }

    fun isFavorite(id: Long) = db.isFavorite(id)

    fun toggleFavorite(id: Long): Boolean {
        val newValue = !db.isFavorite(id)
        db.setFavorite(id, newValue)
        return newValue
    }

    fun districts(): List<String> = db.districts()

    companion object {
        const val DEFAULT_LAT = -11.9905
        const val DEFAULT_LON = -77.0680

        fun distance(lat: Double, lon: Double, store: Store): Int {
            val out = FloatArray(1)
            Location.distanceBetween(lat, lon, store.lat, store.lon, out)
            return out[0].toInt()
        }
    }
}
