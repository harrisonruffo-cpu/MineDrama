package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.util.VideoUrlResolver
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VerticalEpisodePlayer(
    drama: Drama,
    episode: Episode,
    isPlaying: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onOpenEpisodesSheet: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showHeartAnimation by remember { mutableStateOf(false) }
    var isVideoBuffering by remember { mutableStateOf(true) }
    var isPlayerPaused by remember { mutableStateOf(false) }

    val videoSource = remember(episode) {
        if (!episode.localUri.isNullOrBlank()) {
            episode.localUri
        } else {
            VideoUrlResolver.resolve(episode.videoUrl)
        }
    }

    val exoPlayer = remember(videoSource) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoSource))
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = isPlaying
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isVideoBuffering = state == Player.STATE_BUFFERING
                }
            })
        }
    }

    DisposableEffect(videoSource) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying && !isPlayerPaused
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        showHeartAnimation = true
                        if (!isLiked) {
                            onToggleLike()
                        }
                    },
                    onTap = {
                        isPlayerPaused = !isPlayerPaused
                        exoPlayer.playWhenReady = !isPlayerPaused
                    }
                )
            }
    ) {
        // Player de Vídeo
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay de Pausa
        if (isPlayerPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Pausado",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        // Loading do Vídeo
        if (isVideoBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DramaCrimsonBright,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Coração animado de Double Tap
        DoubleTapHeartAnimation(
            show = showHeartAnimation,
            onAnimationEnd = { showHeartAnimation = false }
        )

        // Gradiente Inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Informações da Novela (Inferior Esquerdo)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 80.dp, end = 80.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(DramaCrimson, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "EP ${episode.episodeNumber}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = drama.genre,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = drama.title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (episode.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.title,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Botões Laterais Direitos (Estilo TikTok / Vertical Player)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 85.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Curtir
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Curtir",
                    tint = if (isLiked) DramaCrimsonBright else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Lista de Episódios
            IconButton(
                onClick = onOpenEpisodesSheet,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = "Episódios",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Próximo Episódio
            IconButton(
                onClick = onNextEpisode,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Próximo",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
