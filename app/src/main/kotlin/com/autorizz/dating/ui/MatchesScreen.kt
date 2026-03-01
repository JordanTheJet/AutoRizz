package com.autorizz.dating.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorizz.dating.convo.ConversationRepository
import com.autorizz.dating.db.ConversationMessageEntity
import com.autorizz.dating.db.MatchEntity
import com.autorizz.dating.match.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchRepo: MatchRepository,
    private val convoRepo: ConversationRepository
) : ViewModel() {
    var matches by mutableStateOf<List<MatchEntity>>(emptyList())
        private set
    var selectedMatch by mutableStateOf<MatchEntity?>(null)
        private set
    var conversation by mutableStateOf<List<ConversationMessageEntity>>(emptyList())
        private set
    var filterStatus by mutableStateOf<String?>(null)
        private set
    var filterApp by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            matches = when {
                filterStatus != null -> matchRepo.getByStatus(filterStatus!!)
                filterApp != null -> matchRepo.getByApp(filterApp!!)
                else -> matchRepo.getAll()
            }
        }
    }

    fun setFilter(status: String? = null, app: String? = null) {
        filterStatus = status
        filterApp = app
        refresh()
    }

    fun selectMatch(match: MatchEntity) {
        selectedMatch = match
        viewModelScope.launch {
            conversation = convoRepo.getHistory(match.id)
        }
    }

    fun clearSelection() {
        selectedMatch = null
        conversation = emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onBack: () -> Unit,
    viewModel: MatchesViewModel = hiltViewModel()
) {
    val selected = viewModel.selectedMatch

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: "Matches") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selected != null) viewModel.clearSelection() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selected != null) {
            // Match Detail View
            MatchDetailView(
                match = selected,
                conversation = viewModel.conversation,
                modifier = Modifier.padding(padding)
            )
        } else {
            // Match List View
            Column(modifier = Modifier.padding(padding)) {
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewModel.filterStatus == null && viewModel.filterApp == null,
                        onClick = { viewModel.setFilter() },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = viewModel.filterStatus == "new",
                        onClick = { viewModel.setFilter(status = "new") },
                        label = { Text("New") }
                    )
                    FilterChip(
                        selected = viewModel.filterStatus == "conversing",
                        onClick = { viewModel.setFilter(status = "conversing") },
                        label = { Text("Chatting") }
                    )
                    FilterChip(
                        selected = viewModel.filterStatus == "date_scheduled",
                        onClick = { viewModel.setFilter(status = "date_scheduled") },
                        label = { Text("Dates") }
                    )
                }

                if (viewModel.matches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No matches yet. Say \"start swiping\" to begin.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(viewModel.matches) { match ->
                            MatchListItem(
                                match = match,
                                onClick = { viewModel.selectMatch(match) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchListItem(match: MatchEntity, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(match.name, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                "${match.app.replaceFirstChar { it.uppercase() }}${match.age?.let { " | Age $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            StatusBadge(match.status)
        }
    )
    HorizontalDivider()
}

@Composable
private fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "new" -> MaterialTheme.colorScheme.primary to "New"
        "conversing" -> MaterialTheme.colorScheme.tertiary to "Chatting"
        "date_scheduled" -> MaterialTheme.colorScheme.secondary to "Date Set"
        "date_completed" -> MaterialTheme.colorScheme.outline to "Dated"
        "stale" -> MaterialTheme.colorScheme.outlineVariant to "Stale"
        "unmatched" -> MaterialTheme.colorScheme.error to "Unmatched"
        else -> MaterialTheme.colorScheme.outline to status
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
private fun MatchDetailView(
    match: MatchEntity,
    conversation: List<ConversationMessageEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("App: ${match.app.replaceFirstChar { it.uppercase() }}")
                    match.age?.let { Text("Age: $it") }
                    Text("Status: ${match.status}")
                    Text("Matched: ${formatDateTime(match.matchedAt)}")
                    match.bioSummary?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (conversation.isNotEmpty()) {
            item {
                Text("Conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(conversation) { msg ->
                val isUser = msg.direction == "sent"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (isUser) "You" else match.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            item {
                Text(
                    "No messages yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
