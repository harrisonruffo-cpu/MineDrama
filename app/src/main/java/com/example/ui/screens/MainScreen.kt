package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.model.Drama
import com.example.ui.components.AuthDialog
import com.example.ui.components.EpisodesBottomSheet
import com.example.ui.components.VerticalEpisodePlayer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DramaViewModel
import com.example.ui.viewmodel.VideoUploadViewModel

@Composable
fun MainScreen(
    dramaViewModel: DramaViewModel,
    uploadViewModel: VideoUploadViewModel
) {
    var currentNavIndex by remember { mutableIntStateOf(0) }
    var selectedDramaForDetail by remember { mutableStateOf<Drama?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }

    val allDramas by dramaViewModel.allDramas.collectAsState()
    val currentDrama by dramaViewModel.currentDrama.collectAsState()
    val currentEpisodeIndex by dramaViewModel.currentEpisodeIndex.collectAsState()
    val favoriteDramaIds by dramaViewModel.favoriteDramaIds.collectAsState()
    val currentUser by dramaViewModel.currentUser.collectAsState()

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onLogin = { name, email ->
                dramaViewModel.login(name, email)
                showAuthDialog = false
            }
        )
    }

    if (showEpisodesSheet && currentDrama != null) {
        EpisodesBottomSheet(
            dramaTitle = currentDrama!!.title,
            episodes = currentDrama!!.episodes,
            currentEpisodeIndex = currentEpisodeIndex,
            onSelectEpisode = { idx ->
                dramaViewModel.selectEpisode(idx)
            },
            onDismiss = { showEpisodesSheet = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (selectedDramaForDetail == null) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = DramaCrimson
                ) {
                    NavigationBarItem(
                        selected = currentNavIndex == 0,
                        onClick = { currentNavIndex = 0 },
                        icon = { Icon(Icons.Filled.PlayCircle, contentDescription = "Para Você") },
                        label = { Text("Para Você", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 1,
                        onClick = { currentNavIndex = 1 },
                        icon = { Icon(Icons.Filled.Tv, contentDescription = "Novelas") },
                        label = { Text("Novelas", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 2,
                        onClick = { currentNavIndex = 2 },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Explorar") },
                        label = { Text("Explorar", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 3,
                        onClick = { currentNavIndex = 3 },
                        icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Publicar") },
                        label = { Text("Publicar", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 4,
                        onClick = { currentNavIndex = 4 },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Minha Lista") },
                        label = { Text("Minha Lista", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DramaCrimson,
                            selectedTextColor = DramaCrimson,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (selectedDramaForDetail == null) innerPadding else androidx.compose.foundation.layout.PaddingValues())
        ) {
            if (selectedDramaForDetail != null) {
                DramaDetailScreen(
                    drama = selectedDramaForDetail!!,
                    viewModel = dramaViewModel,
                    onBack = { selectedDramaForDetail = null },
                    onPlayEpisode = { epIdx ->
                        dramaViewModel.selectDrama(selectedDramaForDetail!!, epIdx)
                        selectedDramaForDetail = null
                        currentNavIndex = 0
                    }
                )
            } else {
                when (currentNavIndex) {
                    0 -> {
                        // FEED VERTICAL (Estilo TikTok)
                        if (allDramas.isNotEmpty()) {
                            val activeDrama = currentDrama ?: allDramas.first()
                            val episodes = activeDrama.episodes
                            if (episodes.isNotEmpty()) {
                                val currentEp = episodes.getOrNull(currentEpisodeIndex) ?: episodes.first()
                                VerticalEpisodePlayer(
                                    drama = activeDrama,
                                    episode = currentEp,
                                    isPlaying = true,
                                    isLiked = favoriteDramaIds.contains(activeDrama.id),
                                    onToggleLike = { dramaViewModel.toggleFavorite(activeDrama.id) },
                                    onOpenEpisodesSheet = { showEpisodesSheet = true },
                                    onNextEpisode = { dramaViewModel.nextEpisode() }
                                )
                            }
                        }
                    }
                    1 -> NovelasCatalogScreen(
                        viewModel = dramaViewModel,
                        onDramaSelected = { drama -> selectedDramaForDetail = drama }
                    )
                    2 -> ExploreSearchScreen(
                        viewModel = dramaViewModel,
                        onDramaSelected = { drama -> selectedDramaForDetail = drama }
                    )
                    3 -> PublishDramaScreen(
                        viewModel = dramaViewModel,
                        uploadViewModel = uploadViewModel
                    )
                    4 -> MyListScreen(
                        viewModel = dramaViewModel,
                        onDramaSelected = { drama -> selectedDramaForDetail = drama }
                    )
                }
            }
        }
    }
}
