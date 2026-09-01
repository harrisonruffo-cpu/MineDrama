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
            val coins = prefs.getInt("user_coins", 100)
            _currentUser.value = UserProfile(
                id = "user_${email.hashCode()}",
                name = name,
                email = email,
                coinsBalance = coins
            )
        }
    }

    fun login(name: String, email: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", name)
            .putInt("user_coins", 150)
            .apply()

        _currentUser.value = UserProfile(
            id = "user_${email.hashCode()}",
            name = name,
            email = email,
            coinsBalance = 150
        )
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}
