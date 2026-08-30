package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.model.PlaybackEpisodeItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.DramaGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VerticalEpisodePlayer(
    items: List<PlaybackEpisodeItem>,
    initialIndex: Int,
    isFavorite: (String) -> Boolean,
    isLiked: (String, Int) -> Boolean,
    onToggleFavorite: (Drama) -> Unit,
    onToggleLike: (String, Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    onProgressUpdate: (Drama, Int, Long, Long) -> Unit,
    onSearchClick: () -> Unit,
    onDramaDetailsClick: (Drama) -> Unit,
    currentUser: com.example.data.auth.UserProfile? = null,
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Carregando novelas...",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size }
    )

    var activeDramaForEpisodesSheet by remember { mutableStateOf<Drama?>(null) }
    var activeEpisodeForSheet by remember { mutableStateOf<Int>(1) }

    LaunchedEffect(initialIndex) {
        if (initialIndex in items.indices && pagerState.currentPage != initialIndex) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            val isCurrentPage = pagerState.currentPage == page

            SingleEpisodePlayerView(
                item = item,
                isCurrentPage = isCurrentPage,
                isDramaFavorite = isFavorite(item.drama.id),
                isEpisodeLiked = isLiked(item.drama.id, item.episode.episodeNumber),
                onToggleFavorite = { onToggleFavorite(item.drama) },
                onToggleLike = { onToggleLike(item.drama.id, item.episode.episodeNumber) },
                onOpenEpisodesSheet = {
                    activeDramaForEpisodesSheet = item.drama
                    activeEpisodeForSheet = item.episode.episodeNumber
                },
                onDramaClick = { onDramaDetailsClick(item.drama) },
                onProgressUpdate = { pos, dur ->
                    onProgressUpdate(item.drama, item.episode.episodeNumber, pos, dur)
                },
                onAutoAdvance = {
                    if (pagerState.currentPage < items.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }

        // Top Navigation Controls Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 42.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDramaDetailsClick(items[pagerState.currentPage].drama) }
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.litoral_novelas_logo_1788090147754),
                    contentDescription = "Litoral Novelas",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LITORAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "NOVELAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD166),
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prominent Login Button on Home Screen
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = if (currentUser != null) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFFFFD166).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onLoginClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("home_top_login_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUser != null && currentUser.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentUser.displayName.take(10),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Entrar",
                            tint = if (currentUser != null) Color(0xFF00E5FF) else Color(0xFFFFD166),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentUser != null) "Conta" else "Entrar",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("feed_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Buscar Novelas",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Episodes Bottom Sheet
        activeDramaForEpisodesSheet?.let { drama ->
            EpisodesBottomSheet(
                drama = drama,
                currentEpisodeNumber = activeEpisodeForSheet,
                onEpisodeSelected = { selectedEp ->
                    val targetIndex = items.indexOfFirst {
                        it.drama.id == drama.id && it.episode.episodeNumber == selectedEp.episodeNumber
                    }
                    if (targetIndex >= 0) {
                        scope.launch {
                            pagerState.scrollToPage(targetIndex)
                        }
                    }
                    activeDramaForEpisodesSheet = null
                },
                onDismiss = { activeDramaForEpisodesSheet = null }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SingleEpisodePlayerView(
    item: PlaybackEpisodeItem,
    isCurrentPage: Boolean,
    isDramaFavorite: Boolean,
    isEpisodeLiked: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenEpisodesSheet: () -> Unit,
    onDramaClick: () -> Unit,
    onProgressUpdate: (Long, Long) -> Unit,
    onAutoAdvance: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var hasAttemptedFallback by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    val hearts = remember { mutableStateListOf<HeartEffect>() }

    val fallbackUrls = listOf(
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
    )

    // Robust ExoPlayer creation with mobile UserAgent & headers to prevent HTTP 403
    val exoPlayer = remember(item.episode.id, retryCount) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Range" to "bytes=0-"
                )
            )

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        val rawUrl = item.episode.videoUrl
        val normalizedUrl = if (rawUrl.contains("commondatastorage.googleapis.com")) {
            rawUrl.replace("commondatastorage.googleapis.com", "storage.googleapis.com")
        } else {
            rawUrl
        }

        val videoUri = if (normalizedUrl.startsWith("/")) {
            Uri.fromFile(java.io.File(normalizedUrl))
        } else {
            Uri.parse(normalizedUrl)
        }

        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.prepare()
        player
    }

    DisposableEffect(item.episode.id, retryCount) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Playback parameter changes
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Play / Pause based on page visibility
    LaunchedEffect(isCurrentPage, isPlaying, hasPlaybackError) {
        if (isCurrentPage && isPlaying && !hasPlaybackError) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Track playback state & progress ticker
    LaunchedEffect(isCurrentPage, exoPlayer) {
        if (!isCurrentPage) return@LaunchedEffect

        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("VerticalEpisodePlayer", "Playback error on episode ${item.episode.episodeNumber}: ${error.message}", error)
                if (!hasAttemptedFallback) {
                    hasAttemptedFallback = true
                    val fallback = fallbackUrls[item.episode.episodeNumber % fallbackUrls.size]
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(fallback)))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    hasPlaybackError = true
                    isBuffering = false
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackError = false
                    isBuffering = false
                }
                if (playbackState == Player.STATE_ENDED) {
                    onAutoAdvance()
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            if (exoPlayer.isPlaying && !isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(1L)
                onProgressUpdate(currentPositionMs, durationMs)
            }
            delay(400)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        hearts.add(HeartEffect(System.currentTimeMillis(), offset.x, offset.y))
                        if (!isEpisodeLiked) {
                            onToggleLike()
                        }
                    },
                    onTap = {
                        isPlaying = !isPlaying
                        showPlayPauseIndicator = true
                    }
                )
            }
    ) {
        // ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark Gradients for Readable Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Buffering Indicator
        if (isBuffering && !hasPlaybackError) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DramaGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Playback Error Overlay with Retry
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Erro de reprodução",
                        tint = DramaGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Não foi possível carregar este vídeo.",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tentando restabelecer o link de streaming...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            hasPlaybackError = false
                            hasAttemptedFallback = false
                            retryCount++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Hearts from double taps
        hearts.forEach { heart ->
            HeartBurstOverlay(heart = heart) {
                hearts.remove(heart)
            }
        }

        // Center Play/Pause Animated Feedback
        LaunchedEffect(showPlayPauseIndicator) {
            if (showPlayPauseIndicator) {
                delay(800)
                showPlayPauseIndicator = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Right Side Vertical Action Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drama Poster circle avatar with Follow/Favorite action
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.dp, DramaGold, CircleShape)
                    .clickable { onDramaClick() }
            ) {
                AsyncImage(
                    model = item.drama.posterUrl,
                    contentDescription = item.drama.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Plus or Check badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isDramaFavorite) DramaGold else DramaCrimson)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDramaFavorite) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "Favoritar Novela",
                        tint = if (isDramaFavorite) Color.Black else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Like Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToggleLike() }
            ) {
                Icon(
                    imageVector = if (isEpisodeLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Curtir Episódio",
                    tint = if (isEpisodeLiked) DramaCrimsonBright else Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val baseLikes = item.episode.likesCount
                val totalLikes = if (isEpisodeLiked) baseLikes + 1 else baseLikes
                Text(
                    text = formatCount(totalLikes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Favorite / My List Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToggleFavorite() }
            ) {
                Icon(
                    imageVector = if (isDramaFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Salvar na Minha Lista",
                    tint = if (isDramaFavorite) DramaGold else Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDramaFavorite) "Salvo" else "Lista",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Episode Selector Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenEpisodesSheet() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatListNumbered,
                        contentDescription = "Lista de Episódios",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.episode.episodeNumber}/${item.totalEpisodes}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Playback Speed Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable {
                        playbackSpeed = when (playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.75f
                            else -> 1.0f
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DramaGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            // Mute / Unmute
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mudo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Share Action
            IconButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Assista à novela '${item.drama.title}' - ${item.episode.title} no Mine Drama!\nhttps://minedrama.app/watch/${item.drama.id}/${item.episode.episodeNumber}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Novela"))
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Compartilhar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Details & Scrubber Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 72.dp, bottom = 16.dp)
        ) {
            // Drama Tag and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDramaClick() }
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DramaCrimson)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.drama.category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "@${item.drama.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Episode Title
            Text(
                text = item.episode.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DramaGold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Synopsis Expandable
            Text(
                text = if (isSynopsisExpanded) item.drama.synopsis else "${item.drama.synopsis.take(80)}...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = if (isSynopsisExpanded) 6 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Video Progress Scrubber Bar
            val currentSec = (currentPositionMs / 1000).toInt()
            val totalSec = (durationMs / 1000).toInt()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTime(currentSec),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                )

                Slider(
                    value = if (isDraggingSlider) sliderPosition else (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val seekMs = (sliderPosition * durationMs).toLong()
                        exoPlayer.seekTo(seekMs)
                        isDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = DramaCrimsonBright,
                        activeTrackColor = DramaCrimson,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = formatTime(totalSec),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
