package com.example.fmapp.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fmapp.data.local.*
import com.example.fmapp.viewmodels.AccountWithBalance
import com.example.fmapp.viewmodels.AuthViewModel
import com.example.fmapp.viewmodels.TransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    authViewModel: AuthViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) {
        transactionViewModel.setCurrentUserId(currentUser?.id)
    }

    LaunchedEffect(Unit) {
        transactionViewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val accountsWithBalances by transactionViewModel.accountsWithBalances.collectAsState()
    val transactions by transactionViewModel.transactions.collectAsState()
    val error by transactionViewModel.error.collectAsState()
    // val isLoading by transactionViewModel.isLoading.collectAsState() // Use for overall screen loading if needed

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var selectedAccountForFilter by remember { mutableStateOf<AccountWithBalance?>(null) }

    LaunchedEffect(selectedAccountForFilter){
        transactionViewModel.selectAccount(selectedAccountForFilter?.account?.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    // OCR Button - Placeholder for now
                    IconButton(onClick = { /* TODO: Implement OCR Image Picker */ Toast.makeText(context, "OCR not yet implemented", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = "Scan Receipt (OCR)")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTransactionDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Account Balances", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AccountBalancesRow(accountsWithBalances, selectedAccountForFilter) { accWithBalance ->
                 selectedAccountForFilter = if(selectedAccountForFilter == accWithBalance) null else accWithBalance
            }
            Spacer(Modifier.height(16.dp))

            Text(if(selectedAccountForFilter != null) "Transactions for ${selectedAccountForFilter!!.account.accountName}" else "All Transactions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) {
                    delay(3000)
                    transactionViewModel.clearError()
                }
            }

            if (currentUser == null) {
                Text("Please log in to view transactions.")
            } else if (transactions.isEmpty()) {
                Text(if (selectedAccountForFilter != null) "No transactions for this account yet." else "No transactions recorded yet.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(transactions) { trx ->
                        TransactionItem(trx, onDelete = { transactionViewModel.deleteTransaction(trx) })
                        Divider()
                    }
                }
            }
        }

        if (showAddTransactionDialog) {
            AddTransactionDialog(
                accounts = accountsWithBalances.map { it.account }, // Pass List<FinancialAccount>
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { affectedAccId, amount, type, desc, payer, payee, ref, isInternal, counterAccId, date ->
                    transactionViewModel.addTransaction(affectedAccId, amount, type, desc, payer, payee, ref, isInternal, counterAccId, date)
                    showAddTransactionDialog = false
                },
                viewModel = transactionViewModel // Pass ViewModel for isLoading state
            )
        }
    }
}

@Composable
fun AccountBalancesRow(
    accountsWithBalances: List<AccountWithBalance>,
    selectedAccount: AccountWithBalance?,
    onAccountSelected: (AccountWithBalance) -> Unit
) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        if (accountsWithBalances.isEmpty()){
            Text("No accounts available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            accountsWithBalances.forEach { accWithBalance ->
                Card(
                    modifier = Modifier.padding(end = 8.dp).clickable { onAccountSelected(accWithBalance) },
                    border = if (selectedAccount == accWithBalance) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = if (selectedAccount == accWithBalance) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(accWithBalance.account.accountName, style = MaterialTheme.typography.labelLarge)
                        Text("%.2f ETB".format(accWithBalance.currentBalance), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}


@Composable
fun TransactionItem(transaction: TransactionRecord, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val amountColor = if (transaction.transactionType == TransactionType.INCOME_CREDIT) Color(0xFF006400) /* Dark Green */ else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.descriptionNotes ?: (if(transaction.isInternalTransfer) "Internal Transfer" else "Transaction"), style = MaterialTheme.typography.titleSmall)
            Text(
                "${if (transaction.transactionType == TransactionType.INCOME_CREDIT) "+" else "-"} ${"%.2f".format(transaction.amount)} ${transaction.currency}",
                style = MaterialTheme.typography.bodyMedium,
                color = amountColor
            )
            Text("Date: ${dateFormat.format(transaction.transactionDate)}", style = MaterialTheme.typography.bodySmall)
            transaction.referenceNumber?.takeIf { it.isNotBlank() }?.let { Text("Ref: $it", style = MaterialTheme.typography.bodySmall) }
            if (transaction.isInternalTransfer) {
                 Text("Internal Transfer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Transaction")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<FinancialAccount>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, TransactionType, String?, String?, String?, String?, Boolean, Int?, Date) -> Unit,
    viewModel: TransactionViewModel // For isLoading state
) {
    var affectedAccount by remember { mutableStateOf<FinancialAccount?>(accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE_DEBIT) }
    var description by remember { mutableStateOf("") }
    var payerSender by remember { mutableStateOf("") }
    var payeeReceiver by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var isInternalTransfer by remember { mutableStateOf(false) }
    var counterpartyAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    var transactionDate by remember { mutableStateOf(Date()) }

    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { time = transactionDate }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            transactionDate = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (accounts.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cannot Add Transaction") },
            text = { Text("Please add a financial account first.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Transaction") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                var affectedAccountMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = affectedAccountMenuExpanded, onExpandedChange = {affectedAccountMenuExpanded = !affectedAccountMenuExpanded}, modifier = Modifier.fillMaxWidth()) {
                     OutlinedTextField(
                        value = affectedAccount?.accountName ?: "Select Account*",
                        onValueChange = {}, readOnly = true,
                        label = { Text("From/To Account*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = affectedAccountMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = affectedAccountMenuExpanded, onDismissRequest = { affectedAccountMenuExpanded = false }) {
                        accounts.forEach { acc -> DropdownMenuItem(text = { Text(acc.accountName) }, onClick = { affectedAccount = acc; affectedAccountMenuExpanded = false }) }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (ETB)*") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type*: ", style = MaterialTheme.typography.bodyMedium) // Adjusted style
                    FilterChip(selected = transactionType == TransactionType.EXPENSE_DEBIT, onClick = { transactionType = TransactionType.EXPENSE_DEBIT }, label = {Text("Expense")}, modifier = Modifier.padding(horizontal=4.dp))
                    FilterChip(selected = transactionType == TransactionType.INCOME_CREDIT, onClick = { transactionType = TransactionType.INCOME_CREDIT }, label = {Text("Income")}, modifier = Modifier.padding(horizontal=4.dp))
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description/Notes") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transactionDate),
                    onValueChange = {}, readOnly = true,
                    label = { Text("Transaction Date*") },
                    trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.CalendarToday, "Select Date") } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                var showAdvanced by remember { mutableStateOf(false) }
                TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if(showAdvanced) "Hide Advanced" else "Show Advanced") }
                if(showAdvanced) {
                    OutlinedTextField(value = payerSender, onValueChange = { payerSender = it }, label = { Text("Payer/Sender (Raw)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = payeeReceiver, onValueChange = { payeeReceiver = it }, label = { Text("Payee/Receiver (Raw)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = referenceNumber, onValueChange = { referenceNumber = it }, label = { Text("Reference Number") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isInternalTransfer, onCheckedChange = { isInternalTransfer = it; if(!it) counterpartyAccount = null }) // Clear counterparty if not internal
                        Text("Internal Transfer?")
                    }
                    if (isInternalTransfer) {
                        Spacer(Modifier.height(8.dp))
                        var counterPartyMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = counterPartyMenuExpanded, onExpandedChange = {counterPartyMenuExpanded = !counterPartyMenuExpanded}, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = counterpartyAccount?.accountName ?: "Select Counterparty Account*",
                                onValueChange = {}, readOnly = true,
                                label = { Text("To/From Other Account*") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = counterPartyMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = counterPartyMenuExpanded, onDismissRequest = { counterPartyMenuExpanded = false }) {
                                accounts.filter { it.id != affectedAccount?.id }.forEach { acc ->
                                    DropdownMenuItem(text = { Text(acc.accountName) }, onClick = { counterpartyAccount = acc; counterPartyMenuExpanded = false })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    affectedAccount?.let {
                        onConfirm(it.id, amount, transactionType, description.ifBlank { null }, payerSender.ifBlank { null }, payeeReceiver.ifBlank { null }, referenceNumber.ifBlank { null }, isInternalTransfer, if(isInternalTransfer) counterpartyAccount?.id else null, transactionDate)
                    } ?: Toast.makeText(context, "Please select an account", Toast.LENGTH_SHORT).show()
                },
                enabled = !isLoading && affectedAccount != null && amount.isNotBlank() && (if(isInternalTransfer) counterpartyAccount!=null else true)
            ) {
                if(isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") } }
    )
}
