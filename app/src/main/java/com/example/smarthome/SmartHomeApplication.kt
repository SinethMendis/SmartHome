package com.example.smarthome

import android.app.Application
import android.util.Log
import com.example.smarthome.data.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SmartHomeApplication : Application() {
    val repository: FirestoreRepository by lazy {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        Log.d("MyDebug", "SmartHomeApplication Initializing: $uid")
        FirestoreRepository(FirebaseFirestore.getInstance(), uid)
    }
}
