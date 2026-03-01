package com.autorizz.dating.ui

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
import com.autorizz.dating.date.DateRepository
import com.autorizz.dating.db.MatchEntity
import com.autorizz.dating.db.ScheduledDateEntity
import com.autorizz.dating.match.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatesViewModel @Inject constructor(
    private val dateRepo: DateRepository,
    private val matchRepo: MatchRepository
) : ViewModel() {
    var upcomingDates by mutableStateOf<List<DateWithMatch>>(emptyList())
        private set
    var pastDates by mutableStateOf<List<DateWithMatch>>(emptyList())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            upcomingDates = dateRepo.getUpcoming().map { date ->
                DateWithMatch(date, matchRepo.getById(date.matchId))
            }
            pastDates = dateRepo.getPast().map { date ->
                DateWithMatch(date, matchRepo.getById(date.matchId))
            }
        }
    }
}

data class DateWithMatch(
    val date: ScheduledDateEntity,
    val match: MatchEntity?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesScreen(
    onBack: () -> Unit,
    viewModel: DatesViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.upcomingDates.isEmpty() && viewModel.pastDates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No dates scheduled yet.\nStart swiping and chatting to get dates!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.upcomingDates.isNotEmpty()) {
                    item {
                        Text(
                            "Upcoming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(viewModel.upcomingDates) { item ->
                        DateCard(item)
                    }
                }

                if (viewModel.pastDates.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Past",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(viewModel.pastDates) { item ->
                        DateCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCard(item: DateWithMatch) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Date with ${item.match?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                DateStatusChip(item.date.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                formatDateTime(item.date.dateTime),
                style = MaterialTheme.typography.bodyMedium
            )

            item.date.location?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item.match?.app?.let {
                Text(
                    "Met on ${it.replaceFirstChar { c -> c.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.date.notes?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4
                )
            }
        }
    }
}

@Composable
private fun DateStatusChip(status: String) {
    val label = when (status) {
        "scheduled" -> "Scheduled"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        "no_show" -> "No Show"
        else -> status
    }
    SuggestionChip(onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
}
