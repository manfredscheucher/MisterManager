package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import openfridge.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAssignmentsScreen(
    locationName: String,
    allArticles: List<Article>,
    initialAssignments: Map<Int, Int>,
    onSave: (updatedAssignments: Map<Int, Int>) -> Unit,
    onBack: () -> Unit
) {
    var currentAssignments by remember { mutableStateOf(initialAssignments.toMutableMap()) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasChanges by remember(currentAssignments) {
        derivedStateOf { currentAssignments != initialAssignments }
    }

    val sortedArticles = remember(allArticles, currentAssignments) {
        allArticles.sortedByDescending { article ->
            (currentAssignments[article.id] ?: 0) > 0
        }
    }

    val backAction = {
        if (hasChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackButtonHandler {
        backAction()
    }

    val saveAction = {
        val finalAssignments = currentAssignments.filterValues { it > 0 } // Remove zero amounts
        onSave(finalAssignments)
        onBack()
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(Res.string.form_unsaved_changes_title)) },
            text = { Text(stringResource(Res.string.form_unsaved_changes_message)) },
            confirmButton = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text(stringResource(Res.string.common_stay))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) {
                        Text(stringResource(Res.string.common_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        saveAction()
                        showUnsavedDialog = false
                    }) {
                        Text(stringResource(Res.string.common_yes))
                    }
                }
            },
            dismissButton = null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$locationName - Assignments") },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (allArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(Res.string.article_list_empty)) // Re-using for now, consider a specific string
            }
        } else {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                val state = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding(),
                    state = state,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(sortedArticles, key = { it.id }) { article ->
                        val assignedAmount = currentAssignments[article.id]

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(
                                article.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = assignedAmount?.toString() ?: "",
                                onValueChange = { textValue ->
                                    val numericValue = textValue.toIntOrNull()
                                    currentAssignments = currentAssignments.toMutableMap().apply {
                                        if (numericValue != null && numericValue >= 0) {
                                            this[article.id] = numericValue
                                        } else if (textValue.isEmpty()) {
                                            remove(article.id)
                                        }
                                    }
                                },
                                label = { Text(stringResource(Res.string.assignment_amount_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Divider(Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End // Save button to the right
                        ) {
                            TextButton(onClick = backAction) { Text(stringResource(Res.string.common_cancel)) }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = saveAction) { Text(stringResource(Res.string.common_save)) }
                        }
                    }
                }
            }
        }
    }
}
