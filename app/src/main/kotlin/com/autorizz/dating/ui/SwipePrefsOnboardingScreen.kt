package com.autorizz.dating.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.prefs.PreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun SwipePrefsOnboardingScreen(
    prefsRepo: PreferencesRepository,
    datingConfig: DatingConfig,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var ageMin by remember { mutableStateOf("") }
    var ageMax by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var dealBreakers by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("casual") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Tell AutoRizz what you're looking for.\nYou can refine these later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Age Range
            Text("Age range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ageMin,
                    onValueChange = { ageMin = it },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ageMax,
                    onValueChange = { ageMax = it },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(16.dp))

            // Gender
            Text("I'm interested in", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Gender preference") },
                placeholder = { Text("Women, Men, Everyone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Interests
            Text("Interests I like", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = interests,
                onValueChange = { interests = it },
                label = { Text("Interests") },
                placeholder = { Text("hiking, cooking, dogs, travel") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2
            )

            Spacer(Modifier.height(16.dp))

            // Deal-breakers
            Text("Deal-breakers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = dealBreakers,
                onValueChange = { dealBreakers = it },
                label = { Text("Deal-breakers") },
                placeholder = { Text("smoking, no bio, only group photos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2
            )

            Spacer(Modifier.height(16.dp))

            // Conversation Style
            Text("Conversation style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatingConfig.CONVERSATION_STYLES.forEach { style ->
                    FilterChip(
                        selected = style == selectedStyle,
                        onClick = { selectedStyle = style },
                        label = { Text(style.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (ageMin.isNotBlank()) prefsRepo.set("age_min", ageMin)
                        if (ageMax.isNotBlank()) prefsRepo.set("age_max", ageMax)
                        if (gender.isNotBlank()) prefsRepo.set("gender", gender)
                        if (interests.isNotBlank()) prefsRepo.set("interests", interests)
                        if (dealBreakers.isNotBlank()) prefsRepo.set("deal_breakers", dealBreakers)
                        datingConfig.conversationStyle = selectedStyle
                        datingConfig.datingOnboardingComplete = true
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Using AutoRizz", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = {
                scope.launch {
                    datingConfig.datingOnboardingComplete = true
                    onComplete()
                }
            }) {
                Text("Skip for now")
            }
        }
    }
}
