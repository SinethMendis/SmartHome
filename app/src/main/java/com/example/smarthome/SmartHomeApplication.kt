package com.example.smarthome

import android.app.Application
import com.example.smarthome.data.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore

class SmartHomeApplication : Application() {
    val repository: FirestoreRepository by lazy {
        FirestoreRepository(FirebaseFirestore.getInstance())
    }
}
