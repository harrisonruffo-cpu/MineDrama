package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isAnonymous: Boolean = false,
    val isCreator: Boolean = true,
    val publishedCount: Int = 0
)

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mine_drama_auth", Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val uid = prefs.getString("user_uid", null)
        val email = prefs.getString("user_email", null)
        val name = prefs.getString("user_name", null)
        val photo = prefs.getString("user_photo", null)

        if (uid != null && email != null) {
            _currentUser.value = UserProfile(
                uid = uid,
                displayName = name ?: email.substringBefore("@"),
                email = email,
                photoUrl = photo ?: "",
                isAnonymous = false,
                isCreator = true
            )
        } else {
            // Check Firebase Auth if already signed in
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    _currentUser.value = UserProfile(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Usuário Mine Drama",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        isAnonymous = firebaseUser.isAnonymous,
                        isCreator = true
                    )
                }
            } catch (_: Exception) {
                // Firebase Auth not initialized
            }
        }
    }

    /**
     * Real Google Sign-in with CredentialManager.
     * Allows selecting any Google account on the device or adding a new account.
     */
    suspend fun signInWithGoogle(serverClientId: String = ""): Result<UserProfile> = withContext(Dispatchers.IO) {
        _isAuthenticating.value = true
        _authError.value = null
        try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Using dummy or actual server client ID; setFilterByAuthorizedAccounts(false) ensures all accounts can be selected
            val validClientId = serverClientId.ifBlank { "369710844550-placeholder.apps.googleusercontent.com" }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(validClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                    val profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                    // Try Firebase Auth link if available
                    var uid = "google_${email.hashCode()}"
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        authResult.user?.let { fbUser ->
                            uid = fbUser.uid
                        }
                    } catch (e: Exception) {
                        Log.w("AuthManager", "FirebaseAuth signInWithCredential optional fallback: ${e.message}")
                    }

                    val user = UserProfile(
                        uid = uid,
                        displayName = displayName,
                        email = email,
                        photoUrl = profilePictureUri,
                        isAnonymous = false,
                        isCreator = true
                    )
                    saveSession(user)
                    _currentUser.value = user
                    _isAuthenticating.value = false
                    return@withContext Result.success(user)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("AuthManager", "Invalid Google ID token response", e)
                }
            }

            _isAuthenticating.value = false
            _authError.value = "Não foi possível autenticar com a conta Google selecionada."
            return@withContext Result.failure(Exception("Não foi possível autenticar"))
        } catch (e: Exception) {
            Log.e("AuthManager", "Google sign-in failed or cancelled", e)
            _isAuthenticating.value = false
            val msg = if (e.message?.contains("cancelled", ignoreCase = true) == true) {
                "Login cancelado pelo usuário."
            } else {
                "Erro ao conectar com a Conta Google: ${e.localizedMessage ?: "Tente novamente"}"
            }
            _authError.value = msg
            return@withContext Result.failure(e)
        }
    }

    /**
     * Fast Quick Sign-in with custom Google profile (or mock creator account).
     */
    fun signInWithCustomProfile(name: String, email: String, avatarUrl: String) {
        val user = UserProfile(
            uid = "user_${email.hashCode()}_${System.currentTimeMillis()}",
            displayName = name.ifBlank { email.substringBefore("@") },
            email = email,
            photoUrl = avatarUrl,
            isAnonymous = false,
            isCreator = true
        )
        saveSession(user)
        _currentUser.value = user
    }

    /**
     * Sign out and clear credentials to allow selecting another account.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("AuthManager", "Error clearing credential state: ${e.message}")
        }
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}

        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveSession(user: UserProfile) {
        prefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .putString("user_photo", user.photoUrl)
            .apply()
    }
}
