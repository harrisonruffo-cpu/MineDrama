package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.data.model.Episode
import com.example.data.model.PlaybackEpisodeItem
import com.example.data.remote.DramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UploadProgressInfo(
    val isUploading: Boolean = false,
    val progressPercent: Int = 0,
    val currentStep: String = "",
    val errorMessage: String? = null
)

class DramaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DramaRepository(application)
    private val authManager = AuthManager(application)

    // Auth Flows
    val currentUser: StateFlow<UserProfile?> = authManager.currentUser
    val savedAccounts: StateFlow<List<UserProfile>> = authManager.savedAccounts
    val isAuthenticating: StateFlow<Boolean> = authManager.isAuthenticating
    val authError: StateFlow<String?> = authManager.authError

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _uploadProgress = MutableStateFlow(UploadProgressInfo())
    val uploadProgress: StateFlow<UploadProgressInfo> = _uploadProgress.asStateFlow()

    private val _publishMessage = MutableStateFlow<String?>(null)
    val publishMessage: StateFlow<String?> = _publishMessage.asStateFlow()

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

    // --- Authentication & Account Management ---

    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: (UserProfile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authManager.signInWithEmailAndPassword(email, pass)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure { err ->
                onError(err.message ?: "Erro ao entrar com e-mail.")
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        pass: String,
        onSuccess: (UserProfile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authManager.signUpWithEmailAndPassword(name, email, pass)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure { err ->
                onError(err.message ?: "Erro ao cadastrar conta.")
            }
        }
    }

    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authManager.sendPasswordResetEmail(email)
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Erro ao enviar e-mail de recuperação.")
            }
        }
    }

    fun signInWithGoogle(serverClientId: String = "") {
        viewModelScope.launch {
            authManager.signInWithGoogle(serverClientId)
        }
    }

    fun selectAccount(account: UserProfile) {
        authManager.selectAccount(account)
    }

    fun addAndSignInGoogleAccount(name: String, email: String, avatarUrl: String) {
        authManager.addAndSignInGoogleAccount(name, email, avatarUrl)
    }

    fun removeSavedAccount(email: String) {
        authManager.removeSavedAccount(email)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun switchAccount(serverClientId: String = "") {
        viewModelScope.launch {
            authManager.signInWithGoogle(serverClientId)
        }
    }

    // --- Publishing, Uploading & Video Management ---

    /**
     * Publishes a new drama or adds an episode with real-time online syncing.
     * Uploads local media files (MP4 video, cover) to Cloud Storage first,
     * receives remote HTTPS URLs, and then writes the document to Firestore.
     */
     fun publishNewDrama(
         title: String,
         originalTitle: String = "",
         synopsis: String,
         category: DramaCategory,
         posterUrl: String,
         bannerUrl: String,
         episodeTitle: String,
         videoUrl: String,
         durationSeconds: Int = 120,
         onSuccess: () -> Unit,
         onError: (String) -> Unit
     ) {
         viewModelScope.launch {
             _isPublishing.value = true
             _uploadProgress.value = UploadProgressInfo(
                 isUploading = true,
                 progressPercent = 5,
                 currentStep = "Preparando arquivos para a nuvem..."
             )

             val user = currentUser.value
             val authorId = user?.uid ?: "creator_${System.currentTimeMillis()}"
             val authorName = user?.displayName ?: "Criador Litoral Novelas"
             val authorPhoto = user?.photoUrl ?: ""
             val dramaId = "drama_${System.currentTimeMillis()}"

             // 1. Upload Cover Image to Cloud Storage (if not remote URL)
             var finalPosterUrl = posterUrl
             if (!posterUrl.startsWith("http://") && !posterUrl.startsWith("https://")) {
                 _uploadProgress.value = UploadProgressInfo(
                     isUploading = true,
                     progressPercent = 15,
                     currentStep = "Enviando capa para o Cloud Storage..."
                 )
                 val coverResult = repository.getStorageManager().uploadCoverImage(
                     uri = posterUrl.toUri(),
                     dramaId = dramaId,
                     onProgress = { percent, _, _ ->
                         _uploadProgress.value = UploadProgressInfo(
                             isUploading = true,
                             progressPercent = 15 + (percent * 20 / 100),
                             currentStep = "Enviando capa para o Storage ($percent%)..."
                         )
                     }
                 )
                 if (coverResult.isSuccess) {
                     finalPosterUrl = coverResult.getOrThrow()
                 } else {
                     // If upload fails, notify or fallback to safe placeholder
                     android.util.Log.w("DramaViewModel", "Cover upload error, falling back: ${coverResult.exceptionOrNull()?.message}")
                 }
             }

             // 2. Upload MP4 Video to Cloud Storage (if not remote URL)
             var finalVideoUrl = videoUrl
             if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                 _uploadProgress.value = UploadProgressInfo(
                     isUploading = true,
                     progressPercent = 35,
                     currentStep = "Iniciando upload do vídeo MP4 para a nuvem..."
                 )
                 val videoResult = repository.getStorageManager().uploadVideo(
                     uri = videoUrl.toUri(),
                     dramaId = dramaId,
                     episodeNumber = 1,
                     onProgress = { percent, _, _ ->
                         _uploadProgress.value = UploadProgressInfo(
                             isUploading = true,
                             progressPercent = 35 + (percent * 55 / 100),
                             currentStep = "Enviando vídeo MP4 para o Storage ($percent%)..."
                         )
                     }
                 )
                 if (videoResult.isSuccess) {
                     finalVideoUrl = videoResult.getOrThrow()
                 } else {
                     val errorMsg = videoResult.exceptionOrNull()?.message ?: "Falha no upload do vídeo."
                     android.util.Log.w("DramaViewModel", "Video storage upload warning: $errorMsg. Using direct uri as fallback.")
                     finalVideoUrl = videoUrl
                 }
             }

             // 3. Save remote metadata to Firestore online database
             _uploadProgress.value = UploadProgressInfo(
                 isUploading = true,
                 progressPercent = 92,
                 currentStep = "Gravando metadados no Firestore..."
             )

             val ep1 = Episode(
                 id = "${dramaId}_ep_1",
                 dramaId = dramaId,
                 episodeNumber = 1,
                 title = episodeTitle.ifBlank { "Episódio 1 - $title" },
                 durationSeconds = durationSeconds,
                 videoUrl = finalVideoUrl,
                 thumbnailUrl = finalPosterUrl,
                 synopsis = synopsis,
                 isFree = true,
                 likesCount = 0L,
                 uploadedAt = System.currentTimeMillis()
             )

             val newDrama = Drama(
                 id = dramaId,
                 title = title,
                 originalTitle = originalTitle.ifBlank { title },
                 synopsis = synopsis,
                 category = category,
                 posterUrl = finalPosterUrl,
                 bannerUrl = if (bannerUrl.isBlank() || (!bannerUrl.startsWith("http://") && !bannerUrl.startsWith("https://"))) finalPosterUrl else bannerUrl,
                 rating = 5.0,
                 views = 1L,
                 likes = 0L,
                 releaseYear = 2026,
                 director = authorName,
                 cast = listOf(authorName),
                 totalEpisodes = 1,
                 isTrending = true,
                 isTop10 = false,
                 topRank = null,
                 tags = listOf(category.displayName, "Litoral Novelas", "Online"),
                 episodes = listOf(ep1),
                 authorId = authorId,
                 authorName = authorName,
                 authorPhotoUrl = authorPhoto
             )

             val success = repository.publishOrUpdateDrama(newDrama)
             _isPublishing.value = false
             _uploadProgress.value = UploadProgressInfo(
                 isUploading = false,
                 progressPercent = 100,
                 currentStep = "Publicação concluída com sucesso!"
             )

             if (success) {
                 _publishMessage.value = "Vídeo e novela publicados online! Disponível para todos os usuários."
                 loadCatalog(forceRefresh = true)
                 onSuccess()
             } else {
                 onError("Não foi possível publicar online. Verifique a conexão com a nuvem.")
             }
         }
     }

    /**
     * Rename a video/episode title in an existing drama online.
     */
    fun renameEpisode(
        dramaId: String,
        episodeId: String,
        newTitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.renameEpisode(dramaId, episodeId, newTitle)
            if (success) {
                // Update currently open detail or feed if applicable
                _selectedDramaForDetail.value?.let { current ->
                    if (current.id == dramaId) {
                        _selectedDramaForDetail.value = current.copy(
                            episodes = current.episodes.map { if (it.id == episodeId) it.copy(title = newTitle) else it }
                        )
                    }
                }
                loadCatalog(forceRefresh = true)
                onSuccess()
            } else {
                onError("Falha ao renomear o episódio online.")
            }
        }
    }

    /**
     * Rename drama title online.
     */
    fun renameDrama(
        dramaId: String,
        newTitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.renameDrama(dramaId, newTitle)
            if (success) {
                _selectedDramaForDetail.value?.let { current ->
                    if (current.id == dramaId) {
                        _selectedDramaForDetail.value = current.copy(title = newTitle)
                    }
                }
                loadCatalog(forceRefresh = true)
                onSuccess()
            } else {
                onError("Falha ao renomear o drama.")
            }
        }
    }

    /**
     * Delete drama online.
     */
    fun deleteDrama(dramaId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteDrama(dramaId)
            if (success) {
                if (_selectedDramaForDetail.value?.id == dramaId) {
                    _selectedDramaForDetail.value = null
                }
                loadCatalog(forceRefresh = true)
                onSuccess()
            } else {
                onError("Falha ao excluir o drama.")
            }
        }
    }

    /**
     * Add new episode to existing drama online.
     */
    fun addEpisodeToDrama(
        dramaId: String,
        episodeTitle: String,
        videoUrl: String,
        durationSeconds: Int = 120,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val drama = repository.getDramaById(dramaId)
            if (drama == null) {
                _isPublishing.value = false
                onError("Drama não encontrado.")
                return@launch
            }
            val nextEpNumber = drama.episodes.size + 1
            val newEpisode = Episode(
                id = "${dramaId}_ep_$nextEpNumber",
                dramaId = dramaId,
                episodeNumber = nextEpNumber,
                title = episodeTitle.ifBlank { "Episódio $nextEpNumber" },
                durationSeconds = durationSeconds,
                videoUrl = videoUrl,
                thumbnailUrl = drama.posterUrl,
                synopsis = "Novo episódio de ${drama.title}",
                isFree = true,
                likesCount = 0L,
                uploadedAt = System.currentTimeMillis()
            )

            val success = repository.addEpisodeToDrama(dramaId, newEpisode)
            _isPublishing.value = false
            if (success) {
                loadCatalog(forceRefresh = true)
                onSuccess()
            } else {
                onError("Falha ao adicionar novo episódio online.")
            }
        }
    }
}
