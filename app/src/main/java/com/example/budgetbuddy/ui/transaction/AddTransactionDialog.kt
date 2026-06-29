package com.example.budgetbuddy.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.budgetbuddy.data.model.Transaction
import java.time.LocalDate

val expenseCategories = listOf("Food", "Transportation", "Shopping", "Bills", "School", "Entertainment")
val incomeCategories = listOf("Salary", "Allowance", "Business")

val categoryIdMap = mapOf(
    "Food" to 1,
    "Transportation" to 2,
    "Shopping" to 3,
    "Bills" to 4,
    "School" to 5,
    "Entertainment" to 6,
    "Salary" to 7,
    "Allowance" to 8,
    "Business" to 9
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    existingTransaction: Transaction? = null,
    externalError: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, Int) -> Unit
) {
    val isEditMode = existingTransaction != null

    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(existingTransaction?.description ?: "") }
    var selectedType by remember { mutableStateOf(existingTransaction?.type ?: "expense") }
    var expanded by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val displayedError = localValidationError ?: externalError

    val categories = if (selectedType == "expense") expenseCategories else incomeCategories

    val initialCategoryName = existingTransaction?.let { tx ->
        categoryIdMap.entries.find { it.value == tx.categoryId }?.key
    } ?: categories.first()

    var selectedCategory by remember { mutableStateOf(initialCategoryName) }

    LaunchedEffect(selectedType) {
        if (selectedCategory !in categories) {
            selectedCategory = categories.first()
        }
    }

    val today = remember { LocalDate.now().toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edit Transaction" else "Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localValidationError = null
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = displayedError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "expense",
                        onClick = { selectedType = "expense" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = selectedType == "income",
                        onClick = { selectedType = "income" },
                        label = { Text("Income") }
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                displayedError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedAmount = amount.toDoubleOrNull()

                when {
                    description.isBlank() -> {
                        localValidationError = "Description cannot be empty."
                    }
                    parsedAmount == null -> {
                        localValidationError = "Enter a valid number for the amount."
                    }
                    parsedAmount <= 0.0 -> {
                        localValidationError = "Amount must be greater than ₱0.00."
                    }
                    else -> {
                        localValidationError = null
                        val categoryId = categoryIdMap[selectedCategory] ?: 1
                        val date = existingTransaction?.transactionDate ?: today
                        onConfirm(parsedAmount, description, selectedType, date, categoryId)
                    }
                }
            }) { Text(if (isEditMode) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}