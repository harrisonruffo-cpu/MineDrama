package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalPublishedDramaStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mine_drama_published_store", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Drama::class.java)
    private val adapter = moshi.adapter<List<Drama>>(listType)

    private val _publishedDramas = MutableStateFlow<List<Drama>>(emptyList())
    val publishedDramas: StateFlow<List<Drama>> = _publishedDramas.asStateFlow()

    init {
        loadPublishedDramas()
    }

    private fun loadPublishedDramas() {
        val json = prefs.getString("published_dramas_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val list = adapter.fromJson(json) ?: emptyList()
                _publishedDramas.value = list
            } catch (e: Exception) {
                Log.e("LocalPublishedDramaStore", "Error loading local published dramas", e)
            }
        }
    }

    @Synchronized
    private fun saveListToPrefs(list: List<Drama>) {
        try {
            val json = adapter.toJson(list)
            prefs.edit().putString("published_dramas_json", json).apply()
            _publishedDramas.value = list
        } catch (e: Exception) {
            Log.e("LocalPublishedDramaStore", "Error saving local published dramas", e)
        }
    }

    fun getAllPublishedDramas(): List<Drama> {
        return _publishedDramas.value
    }

    fun saveOrUpdateDrama(drama: Drama) {
        val current = _publishedDramas.value.toMutableList()
        val index = current.indexOfFirst { it.id == drama.id }
        if (index >= 0) {
            current[index] = drama
        } else {
            current.add(0, drama)
        }
        saveListToPrefs(current)
    }

    fun renameDrama(dramaId: String, newTitle: String): Boolean {
        val current = _publishedDramas.value.toMutableList()
        val index = current.indexOfFirst { it.id == dramaId }
        if (index >= 0) {
            current[index] = current[index].copy(title = newTitle)
            saveListToPrefs(current)
            return true
        }
        return false
    }

    fun renameEpisode(dramaId: String, episodeId: String, newTitle: String): Boolean {
        val current = _publishedDramas.value.toMutableList()
        val index = current.indexOfFirst { it.id == dramaId }
        if (index >= 0) {
            val drama = current[index]
            val updatedEpisodes = drama.episodes.map { ep ->
                if (ep.id == episodeId) ep.copy(title = newTitle) else ep
            }
            current[index] = drama.copy(episodes = updatedEpisodes)
            saveListToPrefs(current)
            return true
        }
        return false
    }

    fun deleteDrama(dramaId: String): Boolean {
        val current = _publishedDramas.value.toMutableList()
        val removed = current.removeAll { it.id == dramaId }
        if (removed) {
            saveListToPrefs(current)
            return true
        }
        return false
    }

    fun addEpisode(dramaId: String, episode: Episode): Boolean {
        val current = _publishedDramas.value.toMutableList()
        val index = current.indexOfFirst { it.id == dramaId }
        if (index >= 0) {
            val drama = current[index]
            val updatedEpisodes = drama.episodes.toMutableList().apply { add(episode) }
            current[index] = drama.copy(
                episodes = updatedEpisodes,
                totalEpisodes = updatedEpisodes.size
            )
            saveListToPrefs(current)
            return true
        }
        return false
    }
}
