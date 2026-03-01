package com.autorizz.mode

import com.autorizz.proxy.ProxyProvider
import com.cellclaw.provider.CompletionRequest
import com.cellclaw.provider.CompletionResponse
import com.cellclaw.provider.FailoverEvent
import com.cellclaw.provider.Provider
import com.cellclaw.provider.ProviderInfo
import com.cellclaw.provider.ProviderManager
import com.cellclaw.provider.ProviderManagerContract
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mode-aware provider manager. In BYOK mode, delegates to CellClaw's
 * direct ProviderManager. In Pro mode, routes through the AutoRizz
 * server proxy.
 */
@Singleton
class AutoRizzProviderManager @Inject constructor(
    private val modeManager: ModeManager,
    private val directProviderManager: ProviderManager,
    private val proxyProvider: dagger.Lazy<ProxyProvider>
) : ProviderManagerContract {

    override fun activeProvider(): Provider {
        return when (modeManager.currentMode) {
            Mode.BYOK -> directProviderManager.activeProvider()
            Mode.PRO -> proxyProvider.get()
        }
    }

    override fun switchProvider(type: String) = directProviderManager.switchProvider(type)
    override fun activeType(): String = directProviderManager.activeType()
    override fun availableProviders(): List<ProviderInfo> = directProviderManager.availableProviders()
    override fun hasKey(type: String): Boolean = directProviderManager.hasKey(type)
    override fun setApiKey(type: String, apiKey: String) = directProviderManager.setApiKey(type, apiKey)
    override fun removeApiKey(type: String) = directProviderManager.removeApiKey(type)

    override val lastFailoverEvent: FailoverEvent?
        get() = when (modeManager.currentMode) {
            Mode.BYOK -> directProviderManager.lastFailoverEvent
            Mode.PRO -> null // Server handles routing, no client-side failover
        }

    override suspend fun completeWithFailover(request: CompletionRequest): CompletionResponse {
        return when (modeManager.currentMode) {
            Mode.BYOK -> directProviderManager.completeWithFailover(request)
            Mode.PRO -> proxyProvider.get().complete(request)
        }
    }
}
