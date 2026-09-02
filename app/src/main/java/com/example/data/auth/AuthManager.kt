package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("litoral_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val email = prefs.getString("user_email", null)
        if (email != null) {
            val name = prefs.getString("user_name", "Usuário") ?: "Usuário"
            val avatar = prefs.getString("user_avatar", "") ?: ""
            val coins = prefs.getInt("user_coins", 150)
            val isVip = prefs.getBoolean("user_is_vip", true)
            _currentUser.value = UserProfile(
                id = "user_${Math.abs(email.hashCode())}",
                name = name,
                email = email,
                avatarUrl = avatar,
                coinsBalance = coins,
                isVip = isVip
            )
        }
    }

    fun loginWithGoogle(
        name: String = "Usuário Google",
        email: String = "usuario@gmail.com",
        avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
    ) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_avatar", avatarUrl)
            .putInt("user_coins", 300)
            .putBoolean("user_is_vip", true)
            .apply()

        _currentUser.value = UserProfile(
            id = "google_${Math.abs(email.hashCode())}",
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            coinsBalance = 300,
            isVip = true
        )
    }

    fun login(name: String, email: String) {
        val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_avatar", avatar)
            .putInt("user_coins", 150)
            .putBoolean("user_is_vip", false)
            .apply()

        _currentUser.value = UserProfile(
            id = "user_${Math.abs(email.hashCode())}",
            name = name,
            email = email,
            avatarUrl = avatar,
            coinsBalance = 150,
            isVip = false
        )
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}

