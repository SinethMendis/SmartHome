package com.example.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.data.FirestoreRepository
import com.example.smarthome.ui.SmartHomeNavGraph
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure user is authenticated anonymously and seed database
        lifecycleScope.launch {
            try {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    FirebaseAuth.getInstance().signInAnonymously().await()
                }
                
                val repository = FirestoreRepository(FirebaseFirestore.getInstance())
                repository.seedDatabase()
            } catch (e: Exception) {
                // Log or handle authentication/seeding error
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SmartHomeNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
