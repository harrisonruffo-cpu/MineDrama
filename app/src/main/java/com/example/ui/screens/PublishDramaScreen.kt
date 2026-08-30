package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.DramaGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.DramaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishDramaScreen(
    viewModel: DramaViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val currentUser by viewModel.currentUser.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val allDramas by viewModel.allDramas.collectAsState()

    val uploadProgress by viewModel.uploadProgress.collectAsState()

    var selectedModeTab by remember { mutableIntStateOf(0) } // 0: Novo Drama, 1: Meus Vídeos / Renomear

    // Form state
    var dramaTitle by remember { mutableStateOf("") }
    var originalTitle by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DramaCategory.ROMANCE_CEO) }
    var posterUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80") }
    var episodeTitle by remember { mutableStateOf("Episódio 1 - A Chegada") }
    var videoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") }
    var durationSeconds by remember { mutableStateOf("90") }

    // Dialog state for renaming
    var dramaToRename by remember { mutableStateOf<Drama?>(null) }
    var newDramaNameInput by remember { mutableStateOf("") }
    var episodeToRename by remember { mutableStateOf<Pair<Drama, com.example.data.model.Episode>?>(null) }
    var newEpisodeNameInput by remember { mutableStateOf("") }
    var showStorageHelpDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Media pickers with direct Cloud Storage Uri readying
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            posterUrl = pickedUri.toString()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Imagem de capa selecionada para envio ao Cloud Storage!")
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            videoUrl = pickedUri.toString()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Vídeo MP4 pronto para upload online no Cloud Storage!")
            }
        }
    }

    // Sample Preset Covers
    val presetCovers = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80" to "Herdeira Elegante",
        "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80" to "CEO Corporativo",
        "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=800&auto=format&fit=crop&q=80" to "Mistério Urbano",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80" to "Amor Proibido",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80" to "Épico & Fantasia"
    )

    // Sample Preset Videos
    val presetVideos = listOf(
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" to "Trailer de Ação HD",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4" to "Cena Dramática CEO",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4" to "Romance & Suspense",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" to "Comédia & Leveza",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4" to "Épico Fantasia"
    )

    // Dialog for renaming drama
    if (dramaToRename != null) {
        val target = dramaToRename!!
        AlertDialog(
            onDismissRequest = { dramaToRename = null },
            title = { Text("Renomear Novela", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o novo título para '${target.title}':", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDramaNameInput,
                        onValueChange = { newDramaNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_drama_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DramaCrimsonBright,
                            unfocusedBorderColor = DarkSurfaceHighlight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDramaNameInput.isNotBlank()) {
                            viewModel.renameDrama(
                                dramaId = target.id,
                                newTitle = newDramaNameInput.trim(),
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Título da novela renomeado online!")
                                    }
                                    dramaToRename = null
                                },
                                onError = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { dramaToRename = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Dialog for renaming episode
    if (episodeToRename != null) {
        val (drama, ep) = episodeToRename!!
        AlertDialog(
            onDismissRequest = { episodeToRename = null },
            title = { Text("Renomear Vídeo / Episódio", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o novo título do episódio:", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newEpisodeNameInput,
                        onValueChange = { newEpisodeNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_episode_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DramaCrimsonBright,
                            unfocusedBorderColor = DarkSurfaceHighlight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEpisodeNameInput.isNotBlank()) {
                            viewModel.renameEpisode(
                                dramaId = drama.id,
                                episodeId = ep.id,
                                newTitle = newEpisodeNameInput.trim(),
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Vídeo renomeado online com sucesso!")
                                    }
                                    episodeToRename = null
                                },
                                onError = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { episodeToRename = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Dialog: Como Configurar Firebase Storage
    if (showStorageHelpDialog) {
        AlertDialog(
            onDismissRequest = { showStorageHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = DramaGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configurar Firebase Storage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Para armazenar vídeos MP4 e imagens na nuvem para todos os usuários online:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Ativar o Cloud Storage no Console:",
                        color = DramaGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Acesse console.firebase.google.com -> Selecione seu projeto -> No menu lateral clique em 'Storage' -> Clique no botão 'Começar' (Get started).",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "2. Configurar Regras de Segurança (Rules):",
                        color = DramaGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Na aba 'Rules' do Storage, defina para permitir uploads de criadores:\n\nrules_version = '2';\nservice firebase.storage {\n  match /b/{bucket}/o {\n    match /{allPaths=**} {\n      allow read, write: if true;\n    }\n  }\n}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "3. Arquivo google-services.json:",
                        color = DramaGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Baixe o arquivo 'google-services.json' em Configurações do Projeto Firebase e substitua o arquivo dentro da pasta /app do seu repositório antes de gerar o APK.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "O app possui fallback automático: seus vídeos salvam e reproduzem instantaneamente mesmo antes de ativar o bucket!",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStorageHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                ) {
                    Text("Entendido")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Publicar & Upload",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Publique seus vídeos e capas online para toda a comunidade",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showStorageHelpDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Guia do Firebase Storage",
                            tint = DramaGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DramaGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Criador", color = DramaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedModeTab,
                containerColor = DarkSurface,
                contentColor = DramaCrimsonBright,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedModeTab]),
                        color = DramaCrimsonBright,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedModeTab == 0,
                    onClick = { selectedModeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Novo Vídeo / Drama", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedModeTab == 1,
                    onClick = { selectedModeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gerenciar & Renomear", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (selectedModeTab == 0) {
                // Form Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "1. Título & Informações do Drama",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DramaGold
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = dramaTitle,
                                    onValueChange = { dramaTitle = it },
                                    label = { Text("Nome da Minissérie / Drama *") },
                                    placeholder = { Text("Ex: O Casamento Secreto do Bilionário") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_drama_title"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DramaCrimsonBright,
                                        unfocusedBorderColor = DarkSurfaceHighlight,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedLabelColor = DramaCrimsonBright,
                                        unfocusedLabelColor = TextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = synopsis,
                                    onValueChange = { synopsis = it },
                                    label = { Text("Sinopse / Descrição *") },
                                    placeholder = { Text("Breve resumo dramático e envolvente...") },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth().testTag("input_drama_synopsis"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DramaCrimsonBright,
                                        unfocusedBorderColor = DarkSurfaceHighlight,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedLabelColor = DramaCrimsonBright,
                                        unfocusedLabelColor = TextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Categoria:", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DramaCategory.values().filterNot { it == DramaCategory.TODAS || it == DramaCategory.EM_ALTA }.forEach { cat ->
                                        val isSelected = selectedCategory == cat
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedCategory = cat },
                                            label = { Text("${cat.iconEmoji} ${cat.displayName}", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DramaCrimson,
                                                selectedLabelColor = Color.White,
                                                containerColor = DarkSurface,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cover image section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "2. Capa da Novela (Poster)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DramaGold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(70.dp)
                                            .aspectRatio(0.72f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceHighlight)
                                    ) {
                                        AsyncImage(
                                            model = posterUrl,
                                            contentDescription = "Prévia da capa",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Button(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("pick_image_button")
                                        ) {
                                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Escolher da Galeria", fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Ou selecione uma capa pré-definida abaixo:", color = TextTertiary, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetCovers) { (url, label) ->
                                        val isChosen = posterUrl == url
                                        Box(
                                            modifier = Modifier
                                                .width(65.dp)
                                                .aspectRatio(0.72f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    width = if (isChosen) 2.dp else 1.dp,
                                                    color = if (isChosen) DramaCrimsonBright else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { posterUrl = url }
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (isChosen) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(DramaCrimsonBright),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Video and episode renaming section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "3. Vídeo e Título do Episódio (Renomear)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DramaGold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = episodeTitle,
                                    onValueChange = { episodeTitle = it },
                                    label = { Text("Nome do Vídeo / Episódio *") },
                                    placeholder = { Text("Ex: Episódio 1 - O Segredo Revelado") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_episode_title"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DramaCrimsonBright,
                                        unfocusedBorderColor = DarkSurfaceHighlight,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedLabelColor = DramaCrimsonBright,
                                        unfocusedLabelColor = TextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { videoPickerLauncher.launch("video/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("pick_video_button")
                                    ) {
                                        Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Selecionar Vídeo Local", fontSize = 12.sp, color = TextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Vídeo selecionado:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = videoUrl,
                                    color = DramaCrimsonBright,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Ou escolha um vídeo streaming de teste:", color = TextTertiary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(presetVideos) { (url, name) ->
                                        val isSelected = videoUrl == url
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { videoUrl = url },
                                            label = { Text(name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DramaGold,
                                                selectedLabelColor = Color.Black,
                                                containerColor = DarkSurface,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Upload Progress and Publishing Card
                    if (uploadProgress.isUploading) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, DramaGold, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            progress = { uploadProgress.progressPercent / 100f },
                                            modifier = Modifier.size(28.dp),
                                            color = DramaGold,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Upload Online para a Nuvem",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = uploadProgress.currentStep,
                                                color = DramaCrimsonBright,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = "${uploadProgress.progressPercent}%",
                                            color = DramaGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { uploadProgress.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = DramaGold,
                                        trackColor = DarkSurfaceHighlight
                                    )
                                }
                            }
                        }
                    }

                    // Publish Button
                    item {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (dramaTitle.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Por favor, preencha o título do drama.")
                                    }
                                    return@Button
                                }
                                if (episodeTitle.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Por favor, dê um nome ao vídeo / episódio.")
                                    }
                                    return@Button
                                }

                                viewModel.publishNewDrama(
                                    title = dramaTitle.trim(),
                                    originalTitle = originalTitle.trim(),
                                    synopsis = synopsis.ifBlank { "Minissérie dramática exclusiva publicada na Litoral Novelas." },
                                    category = selectedCategory,
                                    posterUrl = posterUrl,
                                    bannerUrl = posterUrl,
                                    episodeTitle = episodeTitle.trim(),
                                    videoUrl = videoUrl,
                                    durationSeconds = durationSeconds.toIntOrNull() ?: 120,
                                    onSuccess = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Vídeo e novela publicados com sucesso para todos os usuários online!")
                                        }
                                        // Reset fields
                                        dramaTitle = ""
                                        episodeTitle = "Episódio 1"
                                        onNavigateToFeed()
                                    },
                                    onError = { err ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                )
                            },
                            enabled = !isPublishing,
                            colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("publish_drama_button")
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Enviando para a Nuvem...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publicar Vídeo Agora (Online)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Management & Renaming Tab
                val userUid = currentUser?.uid
                val userDramas = allDramas.filter { it.authorId.isNotBlank() && it.authorId == userUid || it.id.startsWith("drama_user_") }

                if (userDramas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nenhum vídeo publicado por você ainda.",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Publique seu primeiro vídeo na aba 'Novo Vídeo / Drama' para poder renomear e gerenciar aqui.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { selectedModeTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                            ) {
                                Text("Publicar Agora")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(userDramas) { drama ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp)
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
                                        }

                                        // Rename drama button
                                        IconButton(
                                            onClick = {
                                                newDramaNameInput = drama.title
                                                dramaToRename = drama
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Renomear novela", tint = DramaCrimsonBright)
                                        }

                                        // Delete drama button
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteDrama(
                                                    dramaId = drama.id,
                                                    onSuccess = {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("Drama excluído com sucesso.")
                                                        }
                                                    },
                                                    onError = { err ->
                                                        coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                                    }
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = TextTertiary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Vídeos & Episódios:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    drama.episodes.forEach { ep ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DramaCrimsonBright, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ep.title,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            IconButton(
                                                onClick = {
                                                    newEpisodeNameInput = ep.title
                                                    episodeToRename = Pair(drama, ep)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Renomear vídeo", tint = TextSecondary, modifier = Modifier.size(16.dp))
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
