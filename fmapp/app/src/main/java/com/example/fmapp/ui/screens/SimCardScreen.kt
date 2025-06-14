package com.example.fmapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fmapp.data.local.SimCard
import com.example.fmapp.viewmodels.AuthViewModel // To get user ID
import com.example.fmapp.viewmodels.SimCardViewModel
import kotlinx.coroutines.delay // For auto-clearing error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimCardScreen(
    authViewModel: AuthViewModel = viewModel(), // To observe user
    simCardViewModel: SimCardViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    // Pass user ID to SimCardViewModel when available
    LaunchedEffect(currentUser) {
        simCardViewModel.setCurrentUserId(currentUser?.id)
    }

    val simCards by simCardViewModel.simCards.collectAsState()
    val error by simCardViewModel.error.collectAsState()
    val isLoading by simCardViewModel.isLoading.collectAsState() // For specific add/delete ops

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add SIM Card")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Manage SIM Cards", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) { // Auto clear error after a delay
                    delay(3000)
                    simCardViewModel.clearError()
                }
            }

            // This isLoading is for the list itself, separate from add/delete isLoading
            val isListLoading = simCardViewModel.simCards.collectAsState().value.isEmpty() && currentUser != null && simCardViewModel.isLoading.collectAsState().value
            // More accurate list loading: check if currentUserId is set and simCards is empty during initial phase.
            // The provided SimCardViewModel doesn't have a specific "list is loading" state distinct from add/delete.
            // We'll assume that if userId is set, and simCards flow is empty initially, it might be loading.
            // A simpler check: if simCards is empty and there's no error, and user is logged in.

            if (currentUser == null) {
                 Text("Please log in to manage SIM cards.")
            } else if (isListLoading && simCards.isEmpty()) { // Simplified initial loading check
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (simCards.isEmpty()) {
                Text("No SIM cards added yet. Click the '+' button to add one.")
            } else {
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    items(simCards) { sim ->
                        SimCardItem(sim, onDelete = { simCardViewModel.deleteSimCard(sim) })
                        Divider()
                    }
                }
            }
        }

        if (showDialog) {
            AddSimCardDialog(
                onDismiss = { showDialog = false },
                onConfirm = { phoneNumber, nickname, provider, registeredName ->
                    simCardViewModel.addSimCard(phoneNumber, nickname, provider, registeredName)
                    showDialog = false
                },
                isLoading = isLoading // Pass the ViewModel's isLoading for add operation
            )
        }
    }
}

@Composable
fun SimCardItem(sim: SimCard, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sim.simNickname, style = MaterialTheme.typography.titleMedium)
            Text(sim.phoneNumber, style = MaterialTheme.typography.bodyMedium)
            Text(sim.telecomProvider, style = MaterialTheme.typography.bodySmall)
            sim.officialRegisteredName?.let {
                Text("Registered: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete SIM Card")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSimCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit,
    isLoading: Boolean
) {
    var phoneNumber by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var registeredName by remember { mutableStateOf("") } // Optional

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New SIM Card") },
        text = {
            Column {
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone Number*") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Nickname*") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Telecom Provider*") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = registeredName, onValueChange = { registeredName = it }, label = { Text("Official Registered Name (Optional)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(phoneNumber, nickname, provider, registeredName.ifBlank { null })
                },
                enabled = !isLoading && phoneNumber.isNotBlank() && nickname.isNotBlank() && provider.isNotBlank()
            ) {
                if(isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
