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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Seed database
        val repository = FirestoreRepository(FirebaseFirestore.getInstance())
        lifecycleScope.launch {
            repository.seedDatabase()
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
