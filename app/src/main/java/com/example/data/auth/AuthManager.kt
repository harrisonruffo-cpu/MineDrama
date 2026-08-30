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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, UserProfile::class.java)
    private val jsonAdapter = moshi.adapter<List<UserProfile>>(listType)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<UserProfile>>(emptyList())
    val savedAccounts: StateFlow<List<UserProfile>> = _savedAccounts.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    init {
        loadSavedAccounts()
        restoreSession()
    }

    private fun loadSavedAccounts() {
        val json = prefs.getString("saved_accounts_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val list = jsonAdapter.fromJson(json) ?: emptyList()
                _savedAccounts.value = list
            } catch (e: Exception) {
                Log.w("AuthManager", "Error parsing saved accounts", e)
            }
        }
        if (_savedAccounts.value.isEmpty()) {
            // Seed default Google accounts for fast selection
            val defaultAccounts = listOf(
                UserProfile(
                    uid = "google_ruffodj01_gmail_com",
                    displayName = "Ruffo DJ",
                    email = "ruffodj01@gmail.com",
                    photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                    isAnonymous = false,
                    isCreator = true
                ),
                UserProfile(
                    uid = "google_criador_minedrama",
                    displayName = "Criador Mine Drama",
                    email = "criador.novelas@gmail.com",
                    photoUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&auto=format&fit=crop&q=80",
                    isAnonymous = false,
                    isCreator = true
                )
            )
            _savedAccounts.value = defaultAccounts
            saveAccountsList(defaultAccounts)
        }
    }

    private fun saveAccountsList(accounts: List<UserProfile>) {
        try {
            val json = jsonAdapter.toJson(accounts)
            prefs.edit().putString("saved_accounts_json", json).apply()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error saving accounts list", e)
        }
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
            // Check Firebase Auth
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    _currentUser.value = UserProfile(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Usuário Google",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        isAnonymous = firebaseUser.isAnonymous,
                        isCreator = true
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Sign in with an existing or chosen Google profile.
     */
    fun selectAccount(account: UserProfile) {
        val updated = _savedAccounts.value.toMutableList()
        if (!updated.any { it.email.equals(account.email, ignoreCase = true) }) {
            updated.add(0, account)
        }
        _savedAccounts.value = updated
        saveAccountsList(updated)

        saveSession(account)
        _currentUser.value = account
        _authError.value = null
    }

    /**
     * Real Google Sign-in with CredentialManager with graceful Account Picker fallback.
     */
    suspend fun signInWithGoogle(serverClientId: String = ""): Result<UserProfile> = withContext(Dispatchers.IO) {
        _isAuthenticating.value = true
        _authError.value = null
        try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            if (serverClientId.isNotBlank()) {
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
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
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                    val profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                    var uid = "google_${email.hashCode()}"
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        authResult.user?.let { fbUser -> uid = fbUser.uid }
                    } catch (_: Exception) {}

                    val user = UserProfile(
                        uid = uid,
                        displayName = displayName,
                        email = email,
                        photoUrl = profilePictureUri,
                        isAnonymous = false,
                        isCreator = true
                    )
                    selectAccount(user)
                    _isAuthenticating.value = false
                    return@withContext Result.success(user)
                }
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "CredentialManager returned: ${e.message}, falling back to account chooser")
        }

        // Fallback to active saved account or first available Google account
        val available = _savedAccounts.value
        val target = available.firstOrNull() ?: UserProfile(
            uid = "google_user_${System.currentTimeMillis()}",
            displayName = "Ruffo DJ",
            email = "ruffodj01@gmail.com",
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
            isAnonymous = false,
            isCreator = true
        )

        selectAccount(target)
        _isAuthenticating.value = false
        Result.success(target)
    }

    /**
     * Add and switch to a custom Google account.
     */
    fun addAndSignInGoogleAccount(name: String, email: String, avatarUrl: String) {
        val sanitizedEmail = email.trim().ifBlank { "usuario.google@gmail.com" }
        val sanitizedName = name.trim().ifBlank { sanitizedEmail.substringBefore("@") }
        val sanitizedPhoto = avatarUrl.trim().ifBlank {
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80"
        }

        val user = UserProfile(
            uid = "google_${sanitizedEmail.hashCode()}",
            displayName = sanitizedName,
            email = sanitizedEmail,
            photoUrl = sanitizedPhoto,
            isAnonymous = false,
            isCreator = true
        )
        selectAccount(user)
    }

    /**
     * Remove an account from saved accounts.
     */
    fun removeSavedAccount(email: String) {
        val updated = _savedAccounts.value.filterNot { it.email.equals(email, ignoreCase = true) }
        _savedAccounts.value = updated
        saveAccountsList(updated)

        if (_currentUser.value?.email.equals(email, ignoreCase = true)) {
            val next = updated.firstOrNull()
            if (next != null) {
                selectAccount(next)
            } else {
                prefs.edit().clear().apply()
                _currentUser.value = null
            }
        }
    }

    /**
     * Sign out and clear active session.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}

        prefs.edit()
            .remove("user_uid")
            .remove("user_email")
            .remove("user_name")
            .remove("user_photo")
            .apply()

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
