package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.LikedEpisodeEntity
import com.example.data.local.LocalPublishedDramaStore
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.data.model.Episode
import com.example.data.model.PlaybackEpisodeItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DramaRepository(context: Context) {

    private val dramaDao = AppDatabase.getDatabase(context).dramaDao()
    private val firestoreDataSource = FirestoreDramaDataSource()
    private val storageManager = FirebaseStorageManager(context)
    private val localPublishedStore = LocalPublishedDramaStore(context)

    fun getStorageManager(): FirebaseStorageManager = storageManager

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val apiService: DramaApiService = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/open-media-stream/mine-drama/main/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DramaApiService::class.java)

    // In-memory cache of current loaded catalog
    private var cachedDramas: List<Drama> = emptyList()

    /**
     * Real-time stream of catalog updates directly from Firebase Firestore.
     */
    fun observeRealtimeCatalog(): Flow<List<Drama>> = firestoreDataSource.observeDramas()

    suspend fun getDramas(forceRefresh: Boolean = false): List<Drama> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedDramas.isNotEmpty()) {
            return@withContext cachedDramas
        }

        val localUserDramas = localPublishedStore.getAllPublishedDramas()
        val combined = mutableListOf<Drama>()
        combined.addAll(localUserDramas)

        // 1. Try Firebase Firestore first
        try {
            val firestoreDramas = firestoreDataSource.fetchDramasOnce()
            if (firestoreDramas.isNotEmpty()) {
                for (d in firestoreDramas) {
                    if (combined.none { it.id == d.id }) {
                        combined.add(d)
                    }
                }
                cachedDramas = combined
                return@withContext cachedDramas
            }
        } catch (e: Exception) {
            Log.w("DramaRepository", "Firestore fetch returned: ${e.message}")
        }

        // 2. Try REST API
        try {
            val response = apiService.getDramasCatalog()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val apiDramas = response.body()!!
                for (d in apiDramas) {
                    if (combined.none { it.id == d.id }) {
                        combined.add(d)
                    }
                }
                cachedDramas = combined
                try {
                    firestoreDataSource.seedCatalog(apiDramas)
                } catch (_: Exception) {}
                return@withContext cachedDramas
            }
        } catch (e: Exception) {
            Log.w("DramaRepository", "REST API fetch returned: ${e.message}")
        }

        // 3. Fallback to curated catalog
        val curated = getCuratedRealDramas()
        for (d in curated) {
            if (combined.none { it.id == d.id }) {
                combined.add(d)
            }
        }
        cachedDramas = combined
        try {
            firestoreDataSource.seedCatalog(curated)
        } catch (_: Exception) {}

        cachedDramas
    }

    suspend fun getDramaById(id: String): Drama? = withContext(Dispatchers.IO) {
        getDramas().find { it.id == id }
    }

    suspend fun getDramasByCategory(category: DramaCategory): List<Drama> = withContext(Dispatchers.IO) {
        val all = getDramas()
        if (category == DramaCategory.TODAS) {
            all
        } else if (category == DramaCategory.EM_ALTA) {
            all.filter { it.isTrending || (it.topRank != null && it.topRank <= 5) }
        } else {
            all.filter { it.category == category }
        }
    }

    suspend fun searchDramas(query: String, category: DramaCategory? = null): List<Drama> = withContext(Dispatchers.IO) {
        val all = getDramas()
        all.filter { drama ->
            val matchesQuery = query.isBlank() ||
                    drama.title.contains(query, ignoreCase = true) ||
                    drama.originalTitle.contains(query, ignoreCase = true) ||
                    drama.synopsis.contains(query, ignoreCase = true) ||
                    drama.tags.any { it.contains(query, ignoreCase = true) } ||
                    drama.cast.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = category == null || category == DramaCategory.TODAS || drama.category == category

            matchesQuery && matchesCategory
        }
    }

    suspend fun publishOrUpdateDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        // Save locally first so it's guaranteed to be available
        localPublishedStore.saveOrUpdateDrama(drama)

        val updated = cachedDramas.filterNot { it.id == drama.id }.toMutableList()
        updated.add(0, drama)
        cachedDramas = updated

        // Push to Firestore in parallel/online
        try {
            firestoreDataSource.publishOrUpdateDrama(drama)
        } catch (e: Exception) {
            Log.w("DramaRepository", "Firestore publish sync failed: ${e.message}")
        }
        true
    }

    suspend fun deleteDrama(dramaId: String): Boolean = withContext(Dispatchers.IO) {
        localPublishedStore.deleteDrama(dramaId)
        cachedDramas = cachedDramas.filterNot { it.id == dramaId }
        try {
            firestoreDataSource.deleteDrama(dramaId)
        } catch (e: Exception) {
            Log.w("DramaRepository", "Firestore delete sync failed: ${e.message}")
        }
        true
    }

    suspend fun renameEpisode(dramaId: String, episodeId: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        localPublishedStore.renameEpisode(dramaId, episodeId, newTitle)
        cachedDramas = cachedDramas.map { drama ->
            if (drama.id == dramaId) {
                drama.copy(
                    episodes = drama.episodes.map { ep ->
                        if (ep.id == episodeId) ep.copy(title = newTitle) else ep
                    }
                )
            } else drama
        }
        try {
            firestoreDataSource.renameEpisode(dramaId, episodeId, newTitle)
        } catch (e: Exception) {
            Log.w("DramaRepository", "Firestore rename episode failed: ${e.message}")
        }
        true
    }

    suspend fun renameDrama(dramaId: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        localPublishedStore.renameDrama(dramaId, newTitle)
        cachedDramas = cachedDramas.map { drama ->
            if (drama.id == dramaId) drama.copy(title = newTitle) else drama
        }
        try {
            firestoreDataSource.renameDrama(dramaId, newTitle)
        } catch (e: Exception) {
            Log.w("DramaRepository", "Firestore rename drama failed: ${e.message}")
        }
        true
    }

    suspend fun addEpisodeToDrama(dramaId: String, newEpisode: Episode): Boolean = withContext(Dispatchers.IO) {
        localPublishedStore.addEpisode(dramaId, newEpisode)
        val drama = getDramaById(dramaId) ?: return@withContext false
        val updatedEpisodes = drama.episodes.toMutableList().apply { add(newEpisode) }
        val updatedDrama = drama.copy(
            episodes = updatedEpisodes,
            totalEpisodes = updatedEpisodes.size
        )
        publishOrUpdateDrama(updatedDrama)
    }

    suspend fun getAllPlaybackItems(): List<PlaybackEpisodeItem> = withContext(Dispatchers.IO) {
        val dramas = getDramas()
        val list = mutableListOf<PlaybackEpisodeItem>()
        for (drama in dramas) {
            drama.episodes.forEachIndexed { index, ep ->
                list.add(
                    PlaybackEpisodeItem(
                        drama = drama,
                        episode = ep,
                        episodeIndex = index,
                        totalEpisodes = drama.episodes.size
                    )
                )
            }
        }
        list
    }

    // Local DB integration
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = dramaDao.getAllWatchHistory().flowOn(Dispatchers.IO)

    suspend fun saveWatchHistory(
        dramaId: String,
        dramaTitle: String,
        posterUrl: String,
        episodeNumber: Int,
        lastPositionMs: Long,
        durationMs: Long,
        totalEpisodes: Int
    ) = withContext(Dispatchers.IO) {
        dramaDao.insertWatchHistory(
            WatchHistoryEntity(
                dramaId = dramaId,
                dramaTitle = dramaTitle,
                posterUrl = posterUrl,
                lastEpisodeNumber = episodeNumber,
                lastPositionMs = lastPositionMs,
                durationMs = durationMs,
                totalEpisodes = totalEpisodes,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Increment drama views in Firestore
        firestoreDataSource.incrementDramaViews(dramaId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dramaDao.clearWatchHistory()
    }

    fun getFavorites(): Flow<List<FavoriteEntity>> = dramaDao.getAllFavorites().flowOn(Dispatchers.IO)

    fun isFavorite(dramaId: String): Flow<Boolean> = dramaDao.isFavoriteFlow(dramaId).flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(drama: Drama) = withContext(Dispatchers.IO) {
        val isFav = dramaDao.isFavorite(drama.id)
        if (isFav) {
            dramaDao.removeFavorite(drama.id)
        } else {
            dramaDao.insertFavorite(
                FavoriteEntity(
                    dramaId = drama.id,
                    dramaTitle = drama.title,
                    posterUrl = drama.posterUrl,
                    categoryName = drama.category.displayName,
                    rating = drama.rating,
                    totalEpisodes = drama.totalEpisodes,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun getLikedKeys(): Flow<List<String>> = dramaDao.getAllLikedKeys().flowOn(Dispatchers.IO)

    suspend fun toggleLikeEpisode(dramaId: String, episodeNumber: Int, isCurrentlyLiked: Boolean) = withContext(Dispatchers.IO) {
        val key = "${dramaId}_$episodeNumber"
        if (isCurrentlyLiked) {
            dramaDao.removeLikedEpisode(dramaId, episodeNumber)
            firestoreDataSource.updateDramaLikes(dramaId, -1L)
        } else {
            dramaDao.insertLikedEpisode(
                LikedEpisodeEntity(
                    compositeKey = key,
                    dramaId = dramaId,
                    episodeNumber = episodeNumber,
                    likedAt = System.currentTimeMillis()
                )
            )
            firestoreDataSource.updateDramaLikes(dramaId, 1L)
        }
    }

    companion object {
        fun getCuratedRealDramas(): List<Drama> {
            return listOf(
                Drama(
                    id = "drama_01_herdeira",
                    title = "O Destino da Herdeira Secreta",
                    originalTitle = "The Secret Heiress's Destiny",
                    synopsis = "Trocada na infância e criada na humildade, Elena descobre ser a única sucessora de um império bilionário. Ao retornar à alta sociedade, ela deve desmascarar a falsa herdeira e enfrentar o poderoso CEO Arthur Monteiro, que jurou protegê-la sem saber de seu segredo.",
                    category = DramaCategory.ROMANCE_CEO,
                    posterUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.9,
                    views = 1420500,
                    likes = 389400,
                    releaseYear = 2024,
                    director = "Mariana Albuquerque",
                    cast = listOf("Clara Valente", "Lucas Mendonça", "Vitória Bittencourt", "Rodrigo Paes"),
                    totalEpisodes = 10,
                    isTrending = true,
                    isTop10 = true,
                    topRank = 1,
                    tags = listOf("CEO", "Herdeira Secreta", "Identidade Oculta", "Romance Intenso", "Alta Sociedade"),
                    episodes = generateEpisodes(
                        dramaId = "drama_01_herdeira",
                        dramaTitle = "O Destino da Herdeira Secreta",
                        episodeCount = 10,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_02_vinganca",
                    title = "A Vingança da Esposa Perfeita",
                    originalTitle = "Perfect Wife's Retribution",
                    synopsis = "Após ser traída e perder tudo para o marido ganancioso e sua melhor amiga, Beatriz renasce determinada a recuperar a dignidade da família. Armada com segredos corporativos e um contrato audacioso com o magnata rival, ela executa uma vingança implacável.",
                    category = DramaCategory.VINGANCA,
                    posterUrl = "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.8,
                    views = 985200,
                    likes = 274100,
                    releaseYear = 2024,
                    director = "Carlos Vasconcelos",
                    cast = listOf("Isabela Castro", "Guilherme Santos", "Renata Meireles", "Felipe Diniz"),
                    totalEpisodes = 12,
                    isTrending = true,
                    isTop10 = true,
                    topRank = 2,
                    tags = listOf("Vingança", "Reviravolta", "Casamento Por Contrato", "Drama Psicológico", "Poder"),
                    episodes = generateEpisodes(
                        dramaId = "drama_02_vinganca",
                        dramaTitle = "A Vingança da Esposa Perfeita",
                        episodeCount = 12,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_03_magnata",
                    title = "O Retorno do Magnata Supremo",
                    originalTitle = "Return of the Supreme Tycoon",
                    synopsis = "Disfarçado como um humilde entregador para testar a lealdade das pessoas ao seu redor, o bilionário Leonardo revela sua verdadeira identidade quando sua família e sua amada são ameaçadas por rivais implacáveis no mundo financeiro.",
                    category = DramaCategory.ROMANCE_CEO,
                    posterUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.9,
                    views = 1890000,
                    likes = 512000,
                    releaseYear = 2024,
                    director = "Fernando Brandão",
                    cast = listOf("Alexandre Ramos", "Camila Ferraz", "Eduardo Silveira"),
                    totalEpisodes = 8,
                    isTrending = true,
                    isTop10 = true,
                    topRank = 3,
                    tags = listOf("Bilionário Disfarçado", "Superação", "Ação & Drama", "Justiça"),
                    episodes = generateEpisodes(
                        dramaId = "drama_03_magnata",
                        dramaTitle = "O Retorno do Magnata Supremo",
                        episodeCount = 8,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_04_amor_proibido",
                    title = "Amor Proibido no Clã Imperial",
                    originalTitle = "Forbidden Love in the Clan",
                    synopsis = "Entre duas dinastias rivais de negócios em São Paulo e Xangai, a jovem designer Sofia se apaixona pelo herdeiro do clã rival. Segredos do passado e intrigas familiares colocam o romance à prova de fogo.",
                    category = DramaCategory.AMOR_PROIBIDO,
                    posterUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.7,
                    views = 845000,
                    likes = 210000,
                    releaseYear = 2024,
                    director = "Helena Siqueira",
                    cast = listOf("Sofia Chen", "Gabriel Leão", "Patricia Andrade"),
                    totalEpisodes = 10,
                    isTrending = false,
                    isTop10 = true,
                    topRank = 4,
                    tags = listOf("Famílias Rivais", "Amor Impossível", "Paixão Proibida", "Segredos"),
                    episodes = generateEpisodes(
                        dramaId = "drama_04_amor_proibido",
                        dramaTitle = "Amor Proibido no Clã Imperial",
                        episodeCount = 10,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_05_guardiao",
                    title = "O Guarda-Costas do Destino",
                    originalTitle = "Destiny's Bodyguard",
                    synopsis = "Ex-agente de operações especiais recebe a missão de proteger a herdeira mais mimada e ameaçada do país. Conforme os perigos aumentam, a convivência forçada se transforma em uma paixão irresistível e perigosa.",
                    category = DramaCategory.SUSPENSE,
                    posterUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1533488765986-dfa2a9939acd?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.8,
                    views = 1120000,
                    likes = 315000,
                    releaseYear = 2024,
                    director = "Marcos Vinicius",
                    cast = listOf("Bruno Falcão", "Larissa Prado", "Marcio Guedes"),
                    totalEpisodes = 8,
                    isTrending = true,
                    isTop10 = true,
                    topRank = 5,
                    tags = listOf("Guarda-Costas", "Ação", "Tensão Romântica", "Suspense"),
                    episodes = generateEpisodes(
                        dramaId = "drama_05_guardiao",
                        dramaTitle = "O Guarda-Costas do Destino",
                        episodeCount = 8,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_06_comedia",
                    title = "Contrato de Noivado por Acidente",
                    originalTitle = "Accidental Engagement Contract",
                    synopsis = "Uma confeiteira atrapalhada finge ser noiva do herdeiro de uma grande rede de hotéis para salvá-lo de um casamento arranjado. O plano de mentira vira confusão total quando a família passa a morar sob o mesmo teto.",
                    category = DramaCategory.COMEDIA,
                    posterUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.9,
                    views = 760000,
                    likes = 230000,
                    releaseYear = 2024,
                    director = "Juliana Moreira",
                    cast = listOf("Marina Rios", "Lucas Santana", "Dona Zilda"),
                    totalEpisodes = 10,
                    isTrending = true,
                    isTop10 = true,
                    topRank = 6,
                    tags = listOf("Comédia Romântica", "Noivado Falso", "Enemies to Lovers", "Divertido"),
                    episodes = generateEpisodes(
                        dramaId = "drama_06_comedia",
                        dramaTitle = "Contrato de Noivado por Acidente",
                        episodeCount = 10,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_07_imperio_historico",
                    title = "A Princesa Guerreira do Reino Perdido",
                    originalTitle = "Warrior Princess of the Lost Kingdom",
                    synopsis = "Em um épico reino oriental antigo, uma jovem guerreira descobre sua linhagem real e lidera a rebelião para recuperar o trono usurpado pelo falso imperador, encontrando lealdade e paixão no general rebelde.",
                    category = DramaCategory.HISTORICO,
                    posterUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.8,
                    views = 1290000,
                    likes = 398000,
                    releaseYear = 2024,
                    director = "Zhang Wei & Tiago Rocha",
                    cast = listOf("Mei Ling", "Rafael Vargas", "Danilo Cunha"),
                    totalEpisodes = 12,
                    isTrending = false,
                    isTop10 = true,
                    topRank = 7,
                    tags = listOf("Histórico", "Fantasia", "Batalha Épica", "Princesa Guerreira"),
                    episodes = generateEpisodes(
                        dramaId = "drama_07_imperio_historico",
                        dramaTitle = "A Princesa Guerreira do Reino Perdido",
                        episodeCount = 12,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=600&auto=format&fit=crop&q=80"
                    )
                ),
                Drama(
                    id = "drama_08_pacto_misterio",
                    title = "Segredos na Mansão das Sombras",
                    originalTitle = "Secrets in Shadow Mansion",
                    synopsis = "Uma jovem jornalista se infiltra como governanta na mansão da família mais rica da cidade para investigar o desaparecimento de sua irmã, descobrindo uma teia de mentiras e uma atração fatal pelo misterioso herdeiro.",
                    category = DramaCategory.SUSPENSE,
                    posterUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=1200&auto=format&fit=crop&q=80",
                    rating = 4.7,
                    views = 690000,
                    likes = 185000,
                    releaseYear = 2024,
                    director = "Bernardo Luz",
                    cast = listOf("Talita Miranda", "Gustavo Nogueira", "Lorena Aguiar"),
                    totalEpisodes = 8,
                    isTrending = false,
                    isTop10 = true,
                    topRank = 8,
                    tags = listOf("Mistério", "Investigação", "Mansão", "Romance Obscuro"),
                    episodes = generateEpisodes(
                        dramaId = "drama_08_pacto_misterio",
                        dramaTitle = "Segredos na Mansão das Sombras",
                        episodeCount = 8,
                        baseVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        secondaryVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        thumbnail = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=600&auto=format&fit=crop&q=80"
                    )
                )
            )
        }

        private fun generateEpisodes(
            dramaId: String,
            dramaTitle: String,
            episodeCount: Int,
            baseVideoUrl: String,
            secondaryVideoUrl: String,
            thumbnail: String
        ): List<Episode> {
            val episodes = mutableListOf<Episode>()
            val episodeNames = listOf(
                "O Encontro Inesperado",
                "A Revelação do Segredo",
                "O Desafio do CEO",
                "Chamas de Paixão",
                "A Armadilha do Rival",
                "Verdades que Doem",
                "A Escolha Decisiva",
                "A Grande Reviravolta",
                "Aliança Perigosa",
                "O Confronto Final",
                "O Preço do Perdão",
                "Destinos Unidos"
            )

            for (i in 1..episodeCount) {
                val epTitle = if (i <= episodeNames.size) episodeNames[i - 1] else "Capítulo $i: A Continuação"
                val url = if (i % 2 == 1) baseVideoUrl else secondaryVideoUrl
                episodes.add(
                    Episode(
                        id = "${dramaId}_ep_$i",
                        dramaId = dramaId,
                        episodeNumber = i,
                        title = "Ep. $i - $epTitle",
                        durationSeconds = 60 + (i * 15) % 180,
                        videoUrl = url,
                        thumbnailUrl = thumbnail,
                        synopsis = "No episódio $i de $dramaTitle, tensões atingem o ápice quando acontecimentos chocantes mudam o rumo da trama.",
                        isFree = true,
                        likesCount = (12000L + i * 3420L)
                    )
                )
            }
            return episodes
        }
    }
}
