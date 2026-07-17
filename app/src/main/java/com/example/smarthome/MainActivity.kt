package com.example.smarthome

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.data.FirestoreRepository
import com.example.smarthome.ui.SmartHomeNavGraph
import com.example.smarthome.ui.auth.LoginScreen
import com.example.smarthome.ui.components.ErrorScreen
import com.example.smarthome.ui.components.LoadingScreen
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

sealed class AuthStatus {
    object Loading : AuthStatus()
    object Unauthenticated : AuthStatus()
    object Success : AuthStatus()
    data class Error(val message: String) : AuthStatus()
}

class MainActivity : ComponentActivity() {
    private var authStatus by mutableStateOf<AuthStatus>(AuthStatus.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial check for current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                seedIfReady(currentUser.uid)
            }
        } else {
            authStatus = AuthStatus.Unauthenticated
        }

        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                when (val status = authStatus) {
                    is AuthStatus.Loading -> LoadingScreen()
                    is AuthStatus.Unauthenticated -> LoginScreen(
                        onLoginSuccess = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                lifecycleScope.launch {
                                    seedIfReady(user.uid)
                                }
                            }
                        }
                    )
                    is AuthStatus.Error -> ErrorScreen(
                        message = status.message,
                        onRetry = {
                            authStatus = AuthStatus.Loading
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                lifecycleScope.launch { seedIfReady(user.uid) }
                            } else {
                                authStatus = AuthStatus.Unauthenticated
                            }
                        }
                    )
                    is AuthStatus.Success -> {
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
    }

    private suspend fun seedIfReady(uid: String) {
        try {
            Log.d("MyDebug", "User ID: $uid")
            val repository = FirestoreRepository(FirebaseFirestore.getInstance(), uid)
            repository.seedDatabase()
            authStatus = AuthStatus.Success
        } catch (e: Exception) {
            Log.e("MyDebug", "Seed Error", e)
            authStatus = AuthStatus.Error(e.message ?: "Initialization failed")
        }
    }
}
