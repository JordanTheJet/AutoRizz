package com.autorizz.dating.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.prefs.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository,
    val datingConfig: DatingConfig
) : ViewModel() {
    var prefs by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    init {
        viewModelScope.launch { prefs = prefsRepo.getAll() }
    }

    fun setPref(key: String, value: String) {
        viewModelScope.launch {
            prefsRepo.set(key, value)
            prefs = prefsRepo.getAll()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swipe Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            // Age Range
            SectionHeader("Age Range")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrefTextField(
                    label = "Min Age",
                    value = viewModel.prefs["age_min"] ?: "",
                    onValueChange = { viewModel.setPref("age_min", it) },
                    modifier = Modifier.weight(1f)
                )
                PrefTextField(
                    label = "Max Age",
                    value = viewModel.prefs["age_max"] ?: "",
                    onValueChange = { viewModel.setPref("age_max", it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Gender
            SectionHeader("Gender Preference")
            PrefTextField(
                label = "Gender",
                value = viewModel.prefs["gender"] ?: "",
                onValueChange = { viewModel.setPref("gender", it) },
                placeholder = "Women, Men, Everyone"
            )

            // Distance
            SectionHeader("Max Distance")
            PrefTextField(
                label = "Distance",
                value = viewModel.prefs["distance"] ?: "",
                onValueChange = { viewModel.setPref("distance", it) },
                placeholder = "e.g. 15 miles"
            )

            // Interests
            SectionHeader("Interests")
            PrefTextField(
                label = "Interests",
                value = viewModel.prefs["interests"] ?: "",
                onValueChange = { viewModel.setPref("interests", it) },
                placeholder = "hiking, cooking, dogs, travel",
                singleLine = false
            )

            // Deal-breakers
            SectionHeader("Deal-breakers")
            PrefTextField(
                label = "Deal-breakers",
                value = viewModel.prefs["deal_breakers"] ?: "",
                onValueChange = { viewModel.setPref("deal_breakers", it) },
                placeholder = "smoking, no bio, only group photos",
                singleLine = false
            )

            // Photo preferences
            SectionHeader("Photo Preferences")
            PrefTextField(
                label = "Photo preferences",
                value = viewModel.prefs["photo_preferences"] ?: "",
                onValueChange = { viewModel.setPref("photo_preferences", it) },
                placeholder = "at least 3 photos, clear face pic"
            )

            // Bio keywords
            SectionHeader("Bio Keywords (positive signals)")
            PrefTextField(
                label = "Bio keywords",
                value = viewModel.prefs["bio_keywords"] ?: "",
                onValueChange = { viewModel.setPref("bio_keywords", it) },
                placeholder = "adventurous, foodie, active"
            )

            // Vibe / Personality
            SectionHeader("Vibe / Personality")
            PrefTextField(
                label = "Vibe",
                value = viewModel.prefs["vibe"] ?: "",
                onValueChange = { viewModel.setPref("vibe", it) },
                placeholder = "witty, flirty, laid back"
            )

            // Conversation Style
            SectionHeader("Conversation Style")
            val styles = DatingConfig.CONVERSATION_STYLES
            val currentStyle = viewModel.datingConfig.conversationStyle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                styles.forEach { style ->
                    FilterChip(
                        selected = style == currentStyle,
                        onClick = { viewModel.datingConfig.conversationStyle = style },
                        label = { Text(style.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Date Preferences
            SectionHeader("Date Preferences")
            PrefTextField(
                label = "Date ideas",
                value = viewModel.prefs["date_preferences"] ?: "",
                onValueChange = { viewModel.setPref("date_preferences", it) },
                placeholder = "coffee dates, drinks, outdoor activities"
            )

            // Schedule Availability
            SectionHeader("Schedule Availability")
            PrefTextField(
                label = "Availability",
                value = viewModel.prefs["schedule_availability"] ?: "",
                onValueChange = { viewModel.setPref("schedule_availability", it) },
                placeholder = "Weekday evenings, weekend afternoons"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PrefTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    var text by remember(value) { mutableStateOf(value) }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        // Save on focus loss
        supportingText = null
    )

    // Save when text changes (debounced via LaunchedEffect)
    LaunchedEffect(text) {
        if (text != value && text.isNotBlank()) {
            kotlinx.coroutines.delay(500)
            onValueChange(text)
        }
    }
}
