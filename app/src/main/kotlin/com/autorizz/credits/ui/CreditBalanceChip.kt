package com.autorizz.credits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autorizz.credits.CreditCalculator

@Composable
fun CreditBalanceChip(
    balance: Long,
    onClick: () -> Unit = {}
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = "${CreditCalculator.estimateCostDisplay(balance)} credits",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
fun LowCreditBanner(
    balance: Long,
    onBuyCredits: () -> Unit,
    onSwitchToBYOK: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (balance <= 0)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (balance <= 0) "Credits depleted" else "Credits running low (${CreditCalculator.estimateCostDisplay(balance)} left)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (balance <= 0)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBuyCredits,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Buy Credits")
                }

                OutlinedButton(
                    onClick = onSwitchToBYOK,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enter API Key")
                }
            }

            if (balance <= 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your conversations and data are safe and will remain synced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
