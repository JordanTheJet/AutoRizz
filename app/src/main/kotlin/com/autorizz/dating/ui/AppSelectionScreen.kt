package com.autorizz.dating.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autorizz.dating.DatingConfig

@Composable
fun AppSelectionScreen(
    datingConfig: DatingConfig,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val enabledApps = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize from config
    LaunchedEffect(Unit) {
        DatingConfig.ALL_APPS.forEach { app ->
            enabledApps[app] = datingConfig.isAppEnabled(app)
        }
    }

    val hasSelection = enabledApps.values.any { it }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Which apps do you use?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "AutoRizz will automate swiping and conversations on your selected apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            DatingConfig.ALL_APPS.forEach { app ->
                val installed = isAppInstalled(context, DatingConfig.APP_PACKAGES[app] ?: "")
                val checked = enabledApps[app] ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                app.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (installed) "Installed" else "Not installed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (installed)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                enabledApps[app] = enabled
                            },
                            enabled = installed
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    enabledApps.forEach { (app, enabled) ->
                        datingConfig.toggleApp(app, enabled)
                    }
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = hasSelection
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }

            if (!hasSelection) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select at least one app to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
