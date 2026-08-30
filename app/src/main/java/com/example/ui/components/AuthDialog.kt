package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.auth.UserProfile
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
fun AuthDialog(
    viewModel: DramaViewModel,
    onDismiss: () -> Unit,
    initialTab: Int = 0
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var selectedAuthTab by remember { mutableIntStateOf(initialTab) } // 0: Google, 1: Email Login, 2: Cadastrar
    val focusManager = LocalFocusManager.current

    // Email Login State
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }

    // Register State
    var regNameInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }
    var regPasswordInput by remember { mutableStateOf("") }
    var regShowPassword by remember { mutableStateOf(false) }

    // Add Google Account dialog
    var showAddGoogleAccountDialog by remember { mutableStateOf(false) }
    var newGoogleName by remember { mutableStateOf("") }
    var newGoogleEmail by remember { mutableStateOf("") }

    // Password reset dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F233D),
                            Color(0xFF081424),
                            Color(0xFF050B14)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LitoralCyanBright.copy(alpha = 0.6f),
                            LitoralWaveBlue.copy(alpha = 0.3f),
                            Color(0x3300E5FF)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Logo and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.litoral_novelas_logo_1788090147754),
                            contentDescription = "Litoral Novelas Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, LitoralCyanBright.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LITORAL NOVELAS",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = LitoralCyanBright,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Autenticação e Nuvem Online",
                                    color = LitoralCyanBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkSurfaceElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Account Banner if signed in
                if (currentUser != null) {
                    val user = currentUser!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceHighlight.copy(alpha = 0.5f))
                            .border(1.dp, LitoralCyanBright.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, LitoralGold, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = LitoralCyanBright,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conectado como: ${user.displayName}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = user.email,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "Sair",
                                color = DramaCrimsonBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tab Switcher (Google / E-mail / Cadastrar)
                TabRow(
                    selectedTabIndex = selectedAuthTab,
                    containerColor = DarkSurface,
                    contentColor = LitoralCyanBright,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedAuthTab]),
                            color = LitoralCyanBright,
                            height = 2.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedAuthTab == 0,
                        onClick = {
                            selectedAuthTab = 0
                            statusMessage = null
                        },
                        text = {
                            Text(
                                text = "Google",
                                fontSize = 13.sp,
                                fontWeight = if (selectedAuthTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAuthTab == 0) LitoralCyanBright else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedAuthTab == 1,
                        onClick = {
                            selectedAuthTab = 1
                            statusMessage = null
                        },
                        text = {
                            Text(
                                text = "Entrar E-mail",
                                fontSize = 13.sp,
                                fontWeight = if (selectedAuthTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAuthTab == 1) LitoralCyanBright else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedAuthTab == 2,
                        onClick = {
                            selectedAuthTab = 2
                            statusMessage = null
                        },
                        text = {
                            Text(
                                text = "Criar Conta",
                                fontSize = 13.sp,
                                fontWeight = if (selectedAuthTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAuthTab == 2) LitoralCyanBright else TextSecondary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error / Status Message
                if (statusMessage != null || authError != null) {
                    val msg = statusMessage ?: authError
                    Text(
                        text = msg ?: "",
                        color = if (isSuccessMessage) LitoralCyanBright else DramaCrimsonBright,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // TAB 0: Google Sign-in & Multi-Account Selection
                if (selectedAuthTab == 0) {
                    Text(
                        text = "Escolha sua conta Google para sincronizar suas novelas, favoritos e publicações na nuvem:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        items(savedAccounts) { account ->
                            val isSelected = currentUser?.email.equals(account.email, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DarkSurfaceHighlight else DarkSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) LitoralCyanBright else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectAccount(account)
                                        isSuccessMessage = true
                                        statusMessage = "Conectado com ${account.displayName}!"
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (account.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = account.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, if (isSelected) LitoralCyanBright else Color.Gray, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) LitoralCyanBright else TextSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account.displayName,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Ativo",
                                                tint = LitoralCyanBright,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = account.email,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.signInWithGoogle()
                                statusMessage = "Autenticando via Google..."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LitoralCyanBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_google_signin")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Entrar com Google",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showAddGoogleAccountDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LitoralCyanBright
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LitoralCyanBright.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar Conta",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Outra Conta", fontSize = 12.sp)
                        }
                    }
                }

                // TAB 1: Real Email & Password Sign In
                if (selectedAuthTab == 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("E-mail cadastrado") },
                            placeholder = { Text("exemplo@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = LitoralCyanBright)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LitoralCyanBright,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedLabelColor = LitoralCyanBright,
                                cursorColor = LitoralCyanBright,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_email_signin")
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Senha") },
                            placeholder = { Text("Mínimo 6 caracteres") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = LitoralCyanBright)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Mostrar senha",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LitoralCyanBright,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedLabelColor = LitoralCyanBright,
                                cursorColor = LitoralCyanBright,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password_signin")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = emailInput
                                    showForgotPasswordDialog = true
                                }
                            ) {
                                Text(
                                    text = "Esqueci minha senha",
                                    color = LitoralCyanBright,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.signInWithEmail(
                                    email = emailInput,
                                    pass = passwordInput,
                                    onSuccess = {
                                        isSuccessMessage = true
                                        statusMessage = "Bem-vindo de volta, ${it.displayName}!"
                                    },
                                    onError = { err ->
                                        isSuccessMessage = false
                                        statusMessage = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LitoralCyanBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_email_submit_signin")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Entrar com E-mail",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // TAB 2: Real Email Registration (Sign Up)
                if (selectedAuthTab == 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = regNameInput,
                            onValueChange = { regNameInput = it },
                            label = { Text("Seu Nome ou Canal de Criador") },
                            placeholder = { Text("Ex: Mariana Silva / Estúdio Litoral") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = LitoralGold)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LitoralGold,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedLabelColor = LitoralGold,
                                cursorColor = LitoralGold,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_name")
                        )

                        OutlinedTextField(
                            value = regEmailInput,
                            onValueChange = { regEmailInput = it },
                            label = { Text("Seu E-mail Real") },
                            placeholder = { Text("seuemail@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = LitoralCyanBright)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LitoralCyanBright,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedLabelColor = LitoralCyanBright,
                                cursorColor = LitoralCyanBright,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_email")
                        )

                        OutlinedTextField(
                            value = regPasswordInput,
                            onValueChange = { regPasswordInput = it },
                            label = { Text("Crie uma Senha (mín. 6 dígitos)") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = LitoralCyanBright)
                            },
                            trailingIcon = {
                                IconButton(onClick = { regShowPassword = !regShowPassword }) {
                                    Icon(
                                        imageVector = if (regShowPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Mostrar senha",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (regShowPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LitoralCyanBright,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedLabelColor = LitoralCyanBright,
                                cursorColor = LitoralCyanBright,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reg_password")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.signUpWithEmail(
                                    name = regNameInput,
                                    email = regEmailInput,
                                    pass = regPasswordInput,
                                    onSuccess = {
                                        isSuccessMessage = true
                                        statusMessage = "Conta criada com sucesso no Litoral Novelas!"
                                    },
                                    onError = { err ->
                                        isSuccessMessage = false
                                        statusMessage = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LitoralGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_register_submit")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Criar Conta e Conectar à Nuvem",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Litoral Novelas • Histórias que emocionam",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Modal: Adicionar Conta Google
    if (showAddGoogleAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoogleAccountDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Adicionar Conta Google",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Digite seu nome e o e-mail da conta Google que você deseja conectar:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newGoogleName,
                        onValueChange = { newGoogleName = it },
                        label = { Text("Nome da Conta") },
                        placeholder = { Text("Ex: Ruffo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LitoralCyanBright,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGoogleEmail,
                        onValueChange = { newGoogleEmail = it },
                        label = { Text("E-mail Google") },
                        placeholder = { Text("seuemail@gmail.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LitoralCyanBright,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGoogleEmail.isNotBlank()) {
                            viewModel.addAndSignInGoogleAccount(
                                name = newGoogleName,
                                email = newGoogleEmail,
                                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80"
                            )
                            showAddGoogleAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LitoralCyanBright, contentColor = Color.Black)
                ) {
                    Text("Conectar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoogleAccountDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Modal: Esqueci Minha Senha
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Recuperar Senha",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Informe seu e-mail cadastrado para enviarmos o link oficial de redefinição de senha:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("E-mail cadastrado") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LitoralCyanBright,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isNotBlank()) {
                            viewModel.sendPasswordReset(
                                email = resetEmailInput,
                                onSuccess = {
                                    statusMessage = "Link de recuperação enviado para $resetEmailInput!"
                                    isSuccessMessage = true
                                    showForgotPasswordDialog = false
                                },
                                onError = {
                                    statusMessage = it
                                    isSuccessMessage = false
                                    showForgotPasswordDialog = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LitoralCyanBright, contentColor = Color.Black)
                ) {
                    Text("Enviar Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
