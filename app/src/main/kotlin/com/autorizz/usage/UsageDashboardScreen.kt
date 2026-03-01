package com.autorizz.usage

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autorizz.credits.CreditCalculator
import com.autorizz.credits.AiMode
import com.autorizz.credits.ui.SubscriptionPlanSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDashboardScreen(
    currentPlanId: String = "free",
    onBack: () -> Unit,
    viewModel: UsageDashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val creditBalance by viewModel.creditBalance.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    var showPlanSheet by remember { mutableStateOf(false) }

    // Open browser when checkout URL is available
    LaunchedEffect(purchaseState.checkoutUrl) {
        purchaseState.checkoutUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearPurchaseState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credits & Usage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPlanSheet = true },
                icon = { Icon(Icons.Default.ShoppingCart, null) },
                text = { Text("Upgrade Plan") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Balance card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = CreditCalculator.estimateCostDisplay(creditBalance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "credits remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // AI Mode info
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Modes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AiModeRow(AiMode.FAST, "1x")
                        AiModeRow(AiMode.THINKING, "5x")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Output tokens cost 3x input tokens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) } // FAB clearance
        }
    }

    if (showPlanSheet) {
        SubscriptionPlanSheet(
            currentPlanId = currentPlanId,
            onDismiss = { showPlanSheet = false },
            onSubscribe = { plan ->
                viewModel.subscribeToPlan(plan)
                showPlanSheet = false
            }
        )
    }

    // Loading indicator during checkout creation
    if (purchaseState.isLoading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Setting up subscription...")
                }
            }
        )
    }

    // Error dialog
    purchaseState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPurchaseState() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPurchaseState() }) {
                    Text("OK")
                }
            },
            title = { Text("Subscription Error") },
            text = { Text(error) }
        )
    }
}

@Composable
private fun AiModeRow(mode: AiMode, multiplier: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = multiplier,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = mode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
