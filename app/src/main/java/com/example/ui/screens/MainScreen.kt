package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Drama
import com.example.ui.components.VerticalEpisodePlayer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DramaViewModel

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tv

enum class MainTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PARA_VOCE("Para Você", Icons.Filled.PlayCircleFilled, Icons.Outlined.Movie),
    NOVELAS("Novelas", Icons.Filled.Tv, Icons.Outlined.Tv),
    PUBLICAR("Publicar", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload),
    EXPLORAR("Explorar", Icons.Filled.Search, Icons.Outlined.Search),
    PERFIL("Perfil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun MainScreen(
    viewModel: DramaViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val allDramas by viewModel.allDramas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val playbackFeed by viewModel.playbackFeed.collectAsState()
    val currentFeedIndex by viewModel.currentFeedIndex.collectAsState()
    val selectedDramaForDetail by viewModel.selectedDramaForDetail.collectAsState()

    val favorites by viewModel.favorites.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val likedKeys by viewModel.likedKeys.collectAsState()

    val currentDetailDrama = selectedDramaForDetail

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Scaffold(
            bottomBar = {
                // If viewing drama detail screen, we can keep or hide bottom bar; keeping it provides quick navigation
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp
                ) {
                    MainTab.values().forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index && currentDetailDrama == null
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.closeDramaDetails()
                                selectedTab = index
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DramaCrimsonBright,
                                selectedTextColor = DramaCrimsonBright,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // If detail screen is active
                if (currentDetailDrama != null) {
                    val isFav = favorites.any { it.dramaId == currentDetailDrama.id }
                    DramaDetailScreen(
                        drama = currentDetailDrama,
                        isFavorite = isFav,
                        onBack = { viewModel.closeDramaDetails() },
                        onPlayEpisode = { epNum ->
                            viewModel.closeDramaDetails()
                            viewModel.playDramaEpisode(currentDetailDrama.id, epNum) {
                                selectedTab = 0
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(currentDetailDrama) }
                    )
                } else {
                    when (selectedTab) {
                        0 -> {
                            VerticalEpisodePlayer(
                                items = playbackFeed,
                                initialIndex = currentFeedIndex,
                                isFavorite = { dramaId -> favorites.any { it.dramaId == dramaId } },
                                isLiked = { dramaId, epNum -> likedKeys.contains("${dramaId}_$epNum") },
                                onToggleFavorite = { drama -> viewModel.toggleFavorite(drama) },
                                onToggleLike = { dramaId, epNum -> viewModel.toggleLike(dramaId, epNum) },
                                onPageChanged = { page -> viewModel.onFeedPageChanged(page) },
                                onProgressUpdate = { drama, epNum, pos, dur ->
                                    viewModel.saveWatchProgress(drama, epNum, pos, dur)
                                },
                                onSearchClick = { selectedTab = 3 },
                                onDramaDetailsClick = { drama -> viewModel.openDramaDetails(drama) }
                            )
                        }
                        1 -> {
                            NovelasCatalogScreen(
                                dramas = allDramas,
                                isLoading = isLoading,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onDramaClick = { drama -> viewModel.openDramaDetails(drama) },
                                onWatchClick = { drama ->
                                    viewModel.playDramaEpisode(drama.id, 1) {
                                        selectedTab = 0
                                    }
                                },
                                onSearchClick = { selectedTab = 3 },
                                onRefreshCatalog = { viewModel.loadCatalog(forceRefresh = true) }
                            )
                        }
                        2 -> {
                            PublishDramaScreen(
                                viewModel = viewModel,
                                onNavigateToFeed = { selectedTab = 0 }
                            )
                        }
                        3 -> {
                            ExploreSearchScreen(
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                allDramas = allDramas,
                                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                onDramaClick = { drama -> viewModel.openDramaDetails(drama) }
                            )
                        }
                        4 -> {
                            MyListScreen(
                                viewModel = viewModel,
                                onPlayEpisode = { dramaId, epNum ->
                                    viewModel.playDramaEpisode(dramaId, epNum) {
                                        selectedTab = 0
                                    }
                                },
                                onDramaClick = { drama -> viewModel.openDramaDetails(drama) },
                                onExploreClick = { selectedTab = 1 },
                                onNavigateToUpload = { selectedTab = 2 }
                            )
                        }
                    }
                }
            }
        }
    }
}
