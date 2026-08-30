package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class LitoralNovelasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            // Enable offline and persistent disk cache for Firestore
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Log.d("LitoralNovelasApp", "Firebase persistent cloud storage & Firestore initialized successfully")
        } catch (e: Exception) {
            Log.w("LitoralNovelasApp", "Firebase initialization note: ${e.message}")
        }
    }
}
