package com.example.fmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fmapp.ui.screens.AuthScreen
import com.example.fmapp.ui.theme.FmappTheme
import com.example.fmapp.viewmodels.AuthViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.example.fmapp.ui.screens.SimCardScreen
import com.example.fmapp.ui.screens.FinancialAccountScreen
import com.example.fmapp.ui.screens.TransactionScreen // Import

// Define screen states for basic navigation
enum class Screen {
    Auth,
    Dashboard,
    SimManagement,
    FinancialAccountManagement,
    TransactionManagement // Added
}

@Composable
fun MainAppContentPlaceholder(
    authViewModel: AuthViewModel,
    onNavigateToSimManagement: () -> Unit,
    onNavigateToFinancialAccountManagement: () -> Unit,
    onNavigateToTransactionManagement: () -> Unit // Added
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Welcome, ${currentUser?.email ?: "User"}!")
            Text("Main App Dashboard (Placeholder)")
            Button(onClick = onNavigateToSimManagement) { Text("Manage SIM Cards") }
            Button(onClick = onNavigateToFinancialAccountManagement) { Text("Manage Financial Accounts") }
            Button(onClick = onNavigateToTransactionManagement) { Text("View Transactions") } // Added Button
            Button(onClick = { authViewModel.signOut() }) { Text("Sign Out") }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FmappTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val currentUser by authViewModel.currentUser.collectAsState()
    // Determine initial screen based on current user state
    var currentScreen by rememberSaveable {
        mutableStateOf(if (authViewModel.currentUser.value == null) Screen.Auth else Screen.Dashboard)
    }

    // React to currentUser changes from AuthViewModel to switch between Auth and Dashboard
    LaunchedEffect(currentUser) {
        currentScreen = if (currentUser == null) Screen.Auth else Screen.Dashboard
    }

    // Further ensure that if somehow currentUser becomes null and screen isn't Auth, navigate to Auth
    if (currentUser == null && currentScreen != Screen.Auth) {
         LaunchedEffect(Unit) {
            currentScreen = Screen.Auth
         }
    }

    when (currentScreen) {
        Screen.Auth -> AuthScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { currentScreen = Screen.Dashboard },
            onRegisterSuccess = { currentScreen = Screen.Dashboard }
        )
        Screen.Dashboard -> MainAppContentPlaceholder(
            authViewModel = authViewModel,
            onNavigateToSimManagement = { currentScreen = Screen.SimManagement },
            onNavigateToFinancialAccountManagement = { currentScreen = Screen.FinancialAccountManagement },
            onNavigateToTransactionManagement = { currentScreen = Screen.TransactionManagement } // Navigate
        )
        Screen.SimManagement -> SimCardScreen(authViewModel = authViewModel)
        Screen.FinancialAccountManagement -> FinancialAccountScreen(authViewModel = authViewModel)
        Screen.TransactionManagement -> TransactionScreen(authViewModel = authViewModel) // Display screen
    }
}
