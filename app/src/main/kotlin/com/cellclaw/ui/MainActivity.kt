package com.cellclaw.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autorizz.auth.SignInScreen
import com.autorizz.auth.SignUpScreen
import com.autorizz.credits.PurchaseService
import com.autorizz.mode.AutoRizzConfig
import com.autorizz.mode.ModeManager
import com.autorizz.mode.Mode
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.prefs.PreferencesRepository
import com.autorizz.dating.ui.*
import com.autorizz.onboarding.OnboardingScreen
import com.cellclaw.config.AppConfig
import com.cellclaw.provider.ProviderManager
import com.cellclaw.service.CellClawService
import com.cellclaw.service.overlay.OverlayService
import com.cellclaw.ui.screens.*
import com.cellclaw.ui.theme.CellClawTheme
import com.cellclaw.voice.ShakeDetector
import com.cellclaw.voice.VoiceActivationHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appConfig: AppConfig
    @Inject lateinit var providerManager: ProviderManager
    @Inject lateinit var purchaseService: PurchaseService
    @Inject lateinit var cellBreakConfig: AutoRizzConfig
    @Inject lateinit var modeManager: ModeManager
    @Inject lateinit var voiceActivationHandler: VoiceActivationHandler
    @Inject lateinit var shakeDetector: ShakeDetector
    @Inject lateinit var creditService: com.autorizz.backend.CreditService
    @Inject lateinit var creditManager: com.autorizz.credits.CreditManager
    @Inject lateinit var datingConfig: DatingConfig
    @Inject lateinit var prefsRepo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle debug setup via adb: am start -n com.cellclaw/.ui.MainActivity
        //   --es provider "openrouter" --es api_key "sk-or-..." --es model "google/gemini-2.5-flash"
        intent?.let { handleSetupIntent(it) }

        // Handle deep links (e.g. autorizz://purchase-success)
        intent?.let { handleDeepLink(it) }

        // Start foreground service to maintain network access when backgrounded
        if (appConfig.isSetupComplete) {
            startService()
            // Start overlay if enabled and permission granted
            if (appConfig.overlayEnabled && Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, OverlayService::class.java))
            }
            // Register hotkey voice activation listener
            voiceActivationHandler.register()
        }

        // Refresh credit balance from server on app start
        if (cellBreakConfig.isLoggedIn) {
            lifecycleScope.launch {
                val userId = cellBreakConfig.userId ?: return@launch
                creditService.getBalance(userId)
                    .onSuccess { creditManager.setBalance(it) }
            }
        }

        val pendingMessage = intent?.getStringExtra("message")

        setContent {
            CellClawTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDest = when {
                        appConfig.isSetupComplete && datingConfig.datingOnboardingComplete -> "dashboard"
                        appConfig.isSetupComplete -> "app_selection"
                        cellBreakConfig.onboardingComplete -> "setup"
                        else -> "onboarding"
                    }

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onGetStarted = {
                                    modeManager.switchMode(Mode.PRO)
                                    cellBreakConfig.onboardingComplete = true
                                    navController.navigate("signin") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("signin") {
                            SignInScreen(
                                onBack = {
                                    navController.navigate("onboarding") {
                                        popUpTo("signin") { inclusive = true }
                                    }
                                },
                                onSignInSuccess = {
                                    navController.navigate("setup") {
                                        popUpTo("signin") { inclusive = true }
                                    }
                                },
                                onNavigateToSignUp = {
                                    navController.navigate("signup")
                                }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                onBack = { navController.popBackStack() },
                                onSignUpSuccess = {
                                    navController.navigate("setup") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("setup") {
                            SetupScreen(
                                onComplete = {
                                    appConfig.isSetupComplete = true
                                    startService()
                                    navController.navigate("app_selection") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("app_selection") {
                            AppSelectionScreen(
                                datingConfig = datingConfig,
                                onContinue = {
                                    navController.navigate("swipe_prefs_setup") {
                                        popUpTo("app_selection") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("swipe_prefs_setup") {
                            SwipePrefsOnboardingScreen(
                                prefsRepo = prefsRepo,
                                datingConfig = datingConfig,
                                onComplete = {
                                    navController.navigate("dashboard") {
                                        popUpTo("swipe_prefs_setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToPreferences = { navController.navigate("preferences") },
                                onNavigateToMatches = { navController.navigate("matches") },
                                onNavigateToDates = { navController.navigate("dates") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("preferences") {
                            PreferencesScreen(onBack = { navController.popBackStack() })
                        }
                        composable("matches") {
                            MatchesScreen(onBack = { navController.popBackStack() })
                        }
                        composable("dates") {
                            DatesScreen(onBack = { navController.popBackStack() })
                        }
                        composable("chat") {
                            ChatScreen(
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToSkills = { navController.navigate("skills") },
                                onNavigateToGuide = { navController.navigate("guide") },
                                onNavigateToApprovals = { navController.navigate("approvals") },
                                initialMessage = pendingMessage
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToAppAccess = { navController.navigate("app_access") }
                            )
                        }
                        composable("app_access") {
                            AppAccessScreen(onBack = { navController.popBackStack() })
                        }
                        composable("skills") {
                            SkillsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("approvals") {
                            ApprovalScreen(onBack = { navController.popBackStack() })
                        }
                        composable("guide") {
                            GuideScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    private fun handleSetupIntent(intent: Intent) {
        val provider = intent.getStringExtra("provider") ?: return
        val apiKey = intent.getStringExtra("api_key") ?: return
        val model = intent.getStringExtra("model")

        providerManager.switchProvider(provider)
        providerManager.setApiKey(provider, apiKey)
        if (model != null) {
            appConfig.model = model
        }
        appConfig.isSetupComplete = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "autorizz") return

        when (uri.host) {
            "purchase-success" -> {
                Log.d("MainActivity", "Purchase success deep link received")
                lifecycleScope.launch {
                    purchaseService.refreshAfterPurchase()
                }
            }
            "purchase-cancelled" -> {
                Log.d("MainActivity", "Purchase cancelled")
            }
        }
    }

    private fun startService() {
        val intent = Intent(this, CellClawService::class.java).apply {
            action = CellClawService.ACTION_START
        }
        startForegroundService(intent)
    }
}
