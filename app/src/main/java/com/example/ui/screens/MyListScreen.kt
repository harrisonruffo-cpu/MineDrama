package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.auth.UserProfile
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Drama
import com.example.ui.components.AuthDialog
import com.example.ui.components.DramaCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.DramaGold
import com.example.ui.theme.LitoralCyanBright
import com.example.ui.theme.LitoralGold
import com.example.ui.theme.LitoralWaveBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.DramaViewModel

@Composable
fun MyListScreen(
    viewModel: DramaViewModel,
    onPlayEpisode: (String, Int) -> Unit,
    onDramaClick: (Drama) -> Unit,
    onExploreClick: () -> Unit,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watchHistory by viewModel.watchHistory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val likedKeys by viewModel.likedKeys.collectAsState()
    val allDramas by viewModel.allDramas.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Limpar Histórico",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "Deseja realmente apagar todo o seu histórico de episódios assistidos?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearWatchHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                ) {
                    Text("Limpar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // User Profile & Google Sign-In Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (currentUser == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conecte sua Conta no Litoral Novelas",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Acesse com Google ou E-mail para sincronizar seu histórico e novelas na nuvem",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAuthDialog = true },
                            enabled = !isAuthenticating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LitoralCyanBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("google_signin_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Conectando...", fontSize = 12.sp, color = Color.Black)
                            } else {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Entrar / Login", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LitoralCyanBright),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LitoralCyanBright.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("custom_profile_button")
                        ) {
                            Text("Trocar Conta", fontSize = 12.sp)
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = authError ?: "",
                            color = DramaCrimsonBright,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    val user = currentUser!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = user.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, LitoralCyanBright, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LitoralWaveBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LitoralGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CRIADOR", color = LitoralGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Switch account button
                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("switch_account_button")
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = LitoralCyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trocar Conta / Login", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        // Logout button
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("signout_button")
                        ) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = DramaCrimsonBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sair", fontSize = 12.sp, color = DramaCrimsonBright)
                        }
                    }
                }
            }
        }

        // Top Navigation Tabs for List
        val userDramas = allDramas.filter { it.authorId.isNotBlank() && it.authorId == currentUser?.uid || it.id.startsWith("drama_user_") }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = DramaCrimsonBright,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DramaCrimsonBright,
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Histórico (${watchHistory.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color.White else TextSecondary
                        )
                    )
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Favoritos (${favorites.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color.White else TextSecondary
                        )
                    )
                }
            )

            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "Curtidos (${likedKeys.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) Color.White else TextSecondary
                        )
                    )
                }
            )

            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Text(
                        text = "Uploads (${userDramas.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) DramaGold else TextSecondary
                        )
                    )
                }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> WatchHistoryTabContent(
                watchHistory = watchHistory,
                onPlayEpisode = onPlayEpisode,
                onExploreClick = onExploreClick
            )
            1 -> FavoritesTabContent(
                favorites = favorites,
                allDramas = allDramas,
                onDramaClick = onDramaClick,
                onExploreClick = onExploreClick
            )
            2 -> LikedTabContent(
                likedKeys = likedKeys,
                allDramas = allDramas,
                onPlayEpisode = onPlayEpisode,
                onExploreClick = onExploreClick
            )
            3 -> {
                if (userDramas.isEmpty()) {
                    EmptyListState(
                        icon = Icons.Filled.CloudUpload,
                        title = "Nenhum vídeo publicado ainda",
                        subtitle = "Publique suas próprias minisséries com capas e vídeos para toda a comunidade assistir.",
                        buttonText = "Publicar Novo Vídeo",
                        onButtonClick = onNavigateToUpload
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(userDramas) { drama ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDramaClick(drama) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(70.dp)
                                            .aspectRatio(0.72f)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = drama.posterUrl,
                                            contentDescription = drama.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = drama.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${drama.category.displayName} • ${drama.episodes.size} episódios",
                                            color = DramaGold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Status: Publicado Online",
                                            color = DramaCrimsonBright,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { onNavigateToUpload() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Gerenciar", fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchHistoryTabContent(
    watchHistory: List<WatchHistoryEntity>,
    onPlayEpisode: (String, Int) -> Unit,
    onExploreClick: () -> Unit
) {
    if (watchHistory.isEmpty()) {
        EmptyListState(
            icon = Icons.Filled.History,
            title = "Nenhum histórico ainda",
            subtitle = "Assista a episódios nas abas 'Para Você' ou 'Novelas' para continuar de onde parou.",
            buttonText = "Descobrir Novelas",
            onButtonClick = onExploreClick
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(watchHistory) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayEpisode(item.dramaId, item.lastEpisodeNumber) }
                    .testTag("history_item_${item.dramaId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.dramaTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.dramaTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Episódio ${item.lastEpisodeNumber} de ${item.totalEpisodes}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DramaGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val progress = if (item.durationMs > 0) {
                            (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.1f, 1f)
                        } else 0.5f

                        LinearProgressIndicator(
                            progress = { progress },
                            color = DramaCrimson,
                            trackColor = DarkSurfaceHighlight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = DramaCrimsonBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Continuar Assistindo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DramaCrimsonBright
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favorites: List<FavoriteEntity>,
    allDramas: List<Drama>,
    onDramaClick: (Drama) -> Unit,
    onExploreClick: () -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyListState(
            icon = Icons.Filled.Bookmark,
            title = "Nenhuma novela favoritada",
            subtitle = "Toque no ícone de salvar em qualquer novela para acessá-la rapidamente aqui.",
            buttonText = "Explorar Catálogo",
            onButtonClick = onExploreClick
        )
        return
    }

    val favDramas = favorites.mapNotNull { fav ->
        allDramas.find { it.id == fav.dramaId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val chunked = favDramas.chunked(2)
        items(chunked) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { drama ->
                    DramaCard(
                        drama = drama,
                        onClick = { onDramaClick(drama) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LikedTabContent(
    likedKeys: Set<String>,
    allDramas: List<Drama>,
    onPlayEpisode: (String, Int) -> Unit,
    onExploreClick: () -> Unit
) {
    if (likedKeys.isEmpty()) {
        EmptyListState(
            icon = Icons.Filled.Favorite,
            title = "Nenhum episódio curtido",
            subtitle = "Dê dois toques na tela enquanto assiste a um episódio para curtir e salvar!",
            buttonText = "Assistir Agora",
            onButtonClick = onExploreClick
        )
        return
    }

    val likedEpisodes = likedKeys.mapNotNull { key ->
        val parts = key.split("_")
        if (parts.size >= 2) {
            val dramaId = parts[0]
            val epNum = parts[1].toIntOrNull() ?: 1
            val drama = allDramas.find { it.id == dramaId }
            val ep = drama?.episodes?.find { it.episodeNumber == epNum }
            if (drama != null && ep != null) {
                Triple(drama, ep, epNum)
            } else null
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(likedEpisodes) { (drama, ep, epNum) ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayEpisode(drama.id, epNum) }
                    .testTag("liked_item_${drama.id}_$epNum")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        AsyncImage(
                            model = drama.posterUrl,
                            contentDescription = drama.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drama.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ep.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DramaGold,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Curtido",
                        tint = DramaCrimsonBright,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyListState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
