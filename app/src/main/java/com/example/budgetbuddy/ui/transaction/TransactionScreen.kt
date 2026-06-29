package com.example.budgetbuddy.ui.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetbuddy.data.model.Transaction
import com.example.budgetbuddy.viewmodel.TransactionViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogError by remember { mutableStateOf<String?>(null) }

    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editDialogError by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.loadTransactions()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addDialogError = null
                showAddDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Long-press a transaction to edit or delete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet. Tap + to add one.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        transactionToEdit = transaction
                                        showEditDialog = false
                                        editDialogError = null
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = transaction.description.ifBlank { "No description" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = transaction.transactionDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${if (transaction.type == "income") "+" else "-"}₱%.2f".format(transaction.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (transaction.type == "income")
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }

        // Add dialog
        if (showAddDialog) {
            AddTransactionDialog(
                existingTransaction = null,
                externalError = addDialogError,
                onDismiss = {
                    showAddDialog = false
                    addDialogError = null
                },
                onConfirm = { amount, description, type, date, categoryId ->
                    addDialogError = null
                    viewModel.addTransaction(
                        amount = amount,
                        description = description,
                        type = type,
                        date = date,
                        categoryId = categoryId,
                        onBlocked = { error -> addDialogError = error },
                        onSuccess = { showAddDialog = false }
                    )
                }
            )
        }

        // Action sheet shown on long-press: choose Edit or Delete
        if (transactionToEdit != null && !showEditDialog) {
            val transaction = transactionToEdit!!
            AlertDialog(
                onDismissRequest = { transactionToEdit = null },
                title = { Text(transaction.description.ifBlank { "Transaction" }) },
                text = { Text("Choose an action for this transaction.") },
                confirmButton = {
                    TextButton(onClick = {
                        editDialogError = null
                        showEditDialog = true
                    }) {
                        Text("Edit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            transactionToDelete = transaction
                            transactionToEdit = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        // Edit dialog (reuses AddTransactionDialog pre-filled with existing data)
        if (transactionToEdit != null && showEditDialog) {
            val transaction = transactionToEdit!!
            AddTransactionDialog(
                existingTransaction = transaction,
                externalError = editDialogError,
                onDismiss = {
                    showEditDialog = false
                    transactionToEdit = null
                    editDialogError = null
                },
                onConfirm = { amount, description, type, date, categoryId ->
                    editDialogError = null
                    viewModel.updateTransaction(
                        id = transaction.id,
                        amount = amount,
                        description = description,
                        type = type,
                        date = date,
                        categoryId = categoryId,
                        onBlocked = { error -> editDialogError = error },
                        onSuccess = {
                            showEditDialog = false
                            transactionToEdit = null
                        }
                    )
                }
            )
        }

        // Delete confirmation
        transactionToDelete?.let { transaction ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Delete Transaction") },
                text = { Text("Delete \"${transaction.description.ifBlank { "this transaction" }}\"? This can't be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(transaction.id)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}