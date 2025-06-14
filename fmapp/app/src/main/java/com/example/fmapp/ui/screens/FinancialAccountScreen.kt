package com.example.fmapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fmapp.data.local.AccountType
import com.example.fmapp.data.local.FinancialAccount
import com.example.fmapp.data.local.SimCard
import com.example.fmapp.viewmodels.AuthViewModel
import com.example.fmapp.viewmodels.FinancialAccountViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay // Added for error auto-clear
import java.text.SimpleDateFormat
import java.util.Locale // Corrected import for Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAccountScreen(
    authViewModel: AuthViewModel = viewModel(),
    financialAccountViewModel: FinancialAccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) {
        financialAccountViewModel.setCurrentUserId(currentUser?.id)
    }

    LaunchedEffect(Unit) {
        financialAccountViewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val accounts by financialAccountViewModel.financialAccounts.collectAsState()
    val simCards by financialAccountViewModel.allSimCards.collectAsState()
    val error by financialAccountViewModel.error.collectAsState()
    val isLoading by financialAccountViewModel.isLoading.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Financial Account")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Manage Financial Accounts", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) {
                    delay(3000) // Use kotlinx.coroutines.delay
                    financialAccountViewModel.clearError()
                }
            }

            if (isLoading && accounts.isEmpty()) { // Initial loading state
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (accounts.isEmpty() && currentUser != null) { // User logged in but no accounts
                Text("No financial accounts added yet. Click '+' to add.")
            } else if (currentUser == null) { // User not logged in
                 Text("Please log in to manage financial accounts.")
            }
            else {
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    items(accounts) { acc ->
                        FinancialAccountItem(acc, onDelete = { financialAccountViewModel.deleteFinancialAccount(acc) })
                        Divider()
                    }
                }
            }
        }

        if (showDialog) {
            AddFinancialAccountDialog(
                allSimCards = simCards,
                onDismiss = { showDialog = false },
                onConfirm = { name, identifier, type, balance, simId ->
                    financialAccountViewModel.addFinancialAccount(name, identifier, type, balance, simId)
                    showDialog = false
                },
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun FinancialAccountItem(account: FinancialAccount, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(account.accountName, style = MaterialTheme.typography.titleMedium)
            Text("ID: ${account.accountIdentifier}", style = MaterialTheme.typography.bodyMedium)
            Text("Type: ${account.accountType.name.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
            Text("Initial Balance: ${"%.2f".format(account.initialBalance)} ETB", style = MaterialTheme.typography.bodySmall)
            Text("Date Added: ${dateFormat.format(account.dateAdded)}", style = MaterialTheme.typography.bodySmall)
            account.linkedSimId?.let { Text("Linked SIM ID: $it", style = MaterialTheme.typography.bodySmall) }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Account")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancialAccountDialog(
    allSimCards: List<SimCard>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, AccountType, String, Int?) -> Unit,
    isLoading: Boolean
) {
    var accountName by remember { mutableStateOf("") }
    var accountIdentifier by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }

    var accountTypeExpanded by remember { mutableStateOf(false) }
    var selectedAccountType by remember { mutableStateOf(AccountType.BANK_ACCOUNT) }

    var simCardExpanded by remember { mutableStateOf(false) }
    var selectedSimCard by remember { mutableStateOf<SimCard?>(null) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Financial Account") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) { // Changed to Column for proper layout of multiple fields
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Name*") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = accountIdentifier, onValueChange = { accountIdentifier = it }, label = { Text("Account Identifier* (e.g., Acc No.)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance (ETB)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Account Type Dropdown
                Box (modifier = Modifier.fillMaxWidth()){
                    OutlinedTextField(
                        value = selectedAccountType.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type*") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Type", Modifier.clickable { accountTypeExpanded = !accountTypeExpanded }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = accountTypeExpanded, onDismissRequest = { accountTypeExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(text = { Text(type.name.replace('_', ' ')) }, onClick = {
                                selectedAccountType = type
                                accountTypeExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // SIM Card Link Dropdown (Optional)
                Box (modifier = Modifier.fillMaxWidth()){
                    OutlinedTextField(
                        value = selectedSimCard?.simNickname ?: "None (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to SIM Card (Optional)") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select SIM", Modifier.clickable { simCardExpanded = !simCardExpanded }) },
                         modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = simCardExpanded, onDismissRequest = { simCardExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                        DropdownMenuItem(text = { Text("None") }, onClick = {
                            selectedSimCard = null
                            simCardExpanded = false
                        })
                        allSimCards.forEach { sim ->
                            DropdownMenuItem(text = { Text("${sim.simNickname} (${sim.phoneNumber})") }, onClick = {
                                selectedSimCard = sim
                                simCardExpanded = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(accountName, accountIdentifier, selectedAccountType, initialBalance, selectedSimCard?.id)
                },
                enabled = !isLoading && accountName.isNotBlank() && accountIdentifier.isNotBlank() && initialBalance.isNotBlank()
            ) {
                if(isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") } }
    )
}
