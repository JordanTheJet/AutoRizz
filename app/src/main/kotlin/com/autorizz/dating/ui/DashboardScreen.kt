package com.autorizz.dating.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.date.DateRepository
import com.autorizz.dating.db.MatchEntity
import com.autorizz.dating.db.ScheduledDateEntity
import com.autorizz.dating.match.MatchStats
import com.autorizz.dating.match.MatchTracker
import com.autorizz.dating.swipe.SwipeEngine
import com.autorizz.dating.swipe.SwipeSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val matchTracker: MatchTracker,
    private val swipeEngine: SwipeEngine,
    private val dateRepo: DateRepository,
    val datingConfig: DatingConfig
) : ViewModel() {
    var matchStats by mutableStateOf<MatchStats?>(null)
        private set
    var swipeStats by mutableStateOf<Map<String, SwipeSessionResult>>(emptyMap())
        private set
    var upcomingDates by mutableStateOf<List<ScheduledDateEntity>>(emptyList())
        private set
    var recentMatches by mutableStateOf<List<MatchEntity>>(emptyList())
        private set

    suspend fun refresh() {
        matchStats = matchTracker.getMatchStats()
        swipeStats = swipeEngine.getSwipeStats()
        upcomingDates = dateRepo.getUpcoming()
        recentMatches = matchTracker.getNewMatches().take(5)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToDates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoRizz") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text("Agent") },
                    selected = false,
                    onClick = onNavigateToChat
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Matches") },
                    selected = false,
                    onClick = onNavigateToMatches
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Dates") },
                    selected = false,
                    onClick = onNavigateToDates
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Swiping")
                }
                OutlinedButton(
                    onClick = onNavigateToPreferences,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preferences")
                }
            }

            // Match Stats Card
            val stats = viewModel.matchStats
            if (stats != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Matches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("Total", stats.total.toString())
                            StatItem("New", stats.newCount.toString())
                            StatItem("Chatting", stats.conversingCount.toString())
                            StatItem("Dates", stats.dateScheduledCount.toString())
                        }
                        if (stats.total > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem("Hinge", stats.hingeCount.toString())
                                StatItem("Tinder", stats.tinderCount.toString())
                                StatItem("Bumble", stats.bumbleCount.toString())
                            }
                        }
                    }
                }
            }

            // Today's Swipe Stats
            val swipeStats = viewModel.swipeStats
            if (swipeStats.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Today's Swipes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        viewModel.datingConfig.enabledApps.forEach { app ->
                            val appStats = swipeStats[app]
                            if (appStats != null && appStats.profilesSeen > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        app.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${appStats.likes} likes / ${appStats.passes} passes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (swipeStats.values.all { it.profilesSeen == 0 }) {
                            Text(
                                "No swipes today. Say \"start swiping\" to begin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Upcoming Dates
            if (viewModel.upcomingDates.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Upcoming Dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        viewModel.upcomingDates.forEach { date ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    date.location ?: "TBD",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    formatDateTime(date.dateTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recent New Matches
            if (viewModel.recentMatches.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "New Matches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        viewModel.recentMatches.forEach { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${match.name} (${match.app})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                match.age?.let {
                                    Text(
                                        "Age $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
