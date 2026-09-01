package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Drama
import com.example.ui.components.DramaCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DramaViewModel

@Composable
fun MyListScreen(
    viewModel: DramaViewModel,
    onDramaSelected: (Drama) -> Unit,
    modifier: Modifier = Modifier
) {
    val dramas by viewModel.allDramas.collectAsState()
    val favoriteIds by viewModel.favoriteDramaIds.collectAsState()

    val favoriteDramas = remember(dramas, favoriteIds) {
        dramas.filter { favoriteIds.contains(it.id) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Minha Lista & Favoritos",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (favoriteDramas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Você ainda não favoritou nenhuma novela.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dê dois toques no vídeo para curtir e salvar!", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoriteDramas) { drama ->
                    DramaCard(
                        drama = drama,
                        onClick = { onDramaSelected(drama) }
                    )
                }
            }
        }
    }
}
