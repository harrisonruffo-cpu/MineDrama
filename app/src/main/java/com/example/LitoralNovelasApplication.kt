package com.example

import android.app.Application
import android.util.Log

class LitoralNovelasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Appwrite.init(this)
            Log.d("LitoralApp", "Aplicação inicializada com sucesso")
        } catch (e: Exception) {
            Log.e("LitoralApp", "Erro na inicialização da aplicação: ${e.message}")
        }
    }
}
