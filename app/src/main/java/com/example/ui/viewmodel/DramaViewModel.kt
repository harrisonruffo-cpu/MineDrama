package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.data.model.PlaybackEpisodeItem
import com.example.data.remote.DramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DramaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DramaRepository(application)

    private val _allDramas = MutableStateFlow<List<Drama>>(emptyList())
    val allDramas: StateFlow<List<Drama>> = _allDramas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DramaCategory.TODAS)
    val selectedCategory: StateFlow<DramaCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Drama>>(emptyList())
    val searchResults: StateFlow<List<Drama>> = _searchResults.asStateFlow()

    // Vertical playback feed list (each item is an episode from a drama)
    private val _playbackFeed = MutableStateFlow<List<PlaybackEpisodeItem>>(emptyList())
    val playbackFeed: StateFlow<List<PlaybackEpisodeItem>> = _playbackFeed.asStateFlow()

    private val _currentFeedIndex = MutableStateFlow(0)
    val currentFeedIndex: StateFlow<Int> = _currentFeedIndex.asStateFlow()

    // Currently selected drama for details screen or explicit series playback
    private val _selectedDramaForDetail = MutableStateFlow<Drama?>(null)
    val selectedDramaForDetail: StateFlow<Drama?> = _selectedDramaForDetail.asStateFlow()

    // Player settings & preferences
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isAutoPlayNext = MutableStateFlow(true)
    val isAutoPlayNext: StateFlow<Boolean> = _isAutoPlayNext.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Room DB Flows
    val favorites: StateFlow<List<FavoriteEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedKeys: StateFlow<Set<String>> = repository.getLikedKeys()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadCatalog()
        observeRealtimeCatalog()
    }

    private fun observeRealtimeCatalog() {
        viewModelScope.launch {
            repository.observeRealtimeCatalog().collect { realtimeDramas ->
                if (realtimeDramas.isNotEmpty()) {
                    _allDramas.value = realtimeDramas
                    // Refresh playback items with real-time episodes
                    val items = mutableListOf<PlaybackEpisodeItem>()
                    for (drama in realtimeDramas) {
                        drama.episodes.forEachIndexed { index, ep ->
                            items.add(
                                PlaybackEpisodeItem(
                                    drama = drama,
                                    episode = ep,
                                    episodeIndex = index,
                                    totalEpisodes = drama.episodes.size
                                )
                            )
                        }
                    }
                    if (items.isNotEmpty()) {
                        _playbackFeed.value = items
                    }
                }
            }
        }
    }

    fun loadCatalog(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val dramas = repository.getDramas(forceRefresh)
            _allDramas.value = dramas

            val items = repository.getAllPlaybackItems()
            _playbackFeed.value = items

            if (_selectedDramaForDetail.value == null && dramas.isNotEmpty()) {
                _selectedDramaForDetail.value = dramas.first()
            }

            _isLoading.value = false
        }
    }

    fun selectCategory(category: DramaCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = repository.searchDramas(query, _selectedCategory.value)
        }
    }

    fun openDramaDetails(drama: Drama) {
        _selectedDramaForDetail.value = drama
    }

    fun closeDramaDetails() {
        _selectedDramaForDetail.value = null
    }

    /**
     * Jump the vertical player directly to a specific drama and episode.
     */
    fun playDramaEpisode(dramaId: String, episodeNumber: Int, onNavigateToFeed: () -> Unit = {}) {
        val feed = _playbackFeed.value
        val targetIndex = feed.indexOfFirst { it.drama.id == dramaId && it.episode.episodeNumber == episodeNumber }
        if (targetIndex >= 0) {
            _currentFeedIndex.value = targetIndex
        } else {
            // If drama not in feed yet, search drama
            val drama = _allDramas.value.find { it.id == dramaId }
            if (drama != null) {
                _selectedDramaForDetail.value = drama
                val epIndex = (episodeNumber - 1).coerceIn(0, drama.episodes.lastIndex.coerceAtLeast(0))
                val subItems = drama.episodes.mapIndexed { idx, ep ->
                    PlaybackEpisodeItem(drama, ep, idx, drama.episodes.size)
                }
                _playbackFeed.value = subItems
                _currentFeedIndex.value = epIndex
            }
        }
        onNavigateToFeed()
    }

    fun onFeedPageChanged(pageIndex: Int) {
        if (pageIndex in _playbackFeed.value.indices) {
            _currentFeedIndex.value = pageIndex
            val item = _playbackFeed.value[pageIndex]
            saveWatchProgress(
                drama = item.drama,
                episodeNumber = item.episode.episodeNumber,
                positionMs = 0L,
                durationMs = item.episode.durationSeconds * 1000L
            )
        }
    }

    fun toggleFavorite(drama: Drama) {
        viewModelScope.launch {
            repository.toggleFavorite(drama)
        }
    }

    fun toggleLike(dramaId: String, episodeNumber: Int) {
        viewModelScope.launch {
            val key = "${dramaId}_$episodeNumber"
            val isLiked = likedKeys.value.contains(key)
            repository.toggleLikeEpisode(dramaId, episodeNumber, isLiked)
        }
    }

    fun saveWatchProgress(drama: Drama, episodeNumber: Int, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.saveWatchHistory(
                dramaId = drama.id,
                dramaTitle = drama.title,
                posterUrl = drama.posterUrl,
                episodeNumber = episodeNumber,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                totalEpisodes = drama.totalEpisodes
            )
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleAutoPlayNext() {
        _isAutoPlayNext.value = !_isAutoPlayNext.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }
}
