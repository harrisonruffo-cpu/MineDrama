package com.example

import android.app.Application
import android.util.Log

class LitoralNovelasApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Handler global para capturar exceções e evitar loop de crash do sistema
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LitoralApp", "Exceção não tratada na thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            Appwrite.init(this)
            Log.d("LitoralApp", "Aplicação inicializada com sucesso")
        } catch (e: Throwable) {
            Log.e("LitoralApp", "Erro na inicialização do Appwrite: ${e.message}", e)
        }
    }
}
