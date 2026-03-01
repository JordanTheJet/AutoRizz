# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

AutoRizz is a prosumer fork of [CellClaw](https://github.com/jordanthejet/cellclaw), an autonomous AI agent for Android. AutoRizz adds cloud auth, credit-based usage, server-proxied LLM requests, and multi-backend support on top of CellClaw's 33-tool agent loop.

- **CellClaw code** lives in `com.cellclaw.*` — treat as upstream, minimize changes
- **AutoRizz code** lives in `com.autorizz.*` — all new backend/mode/proxy logic
- The fork's key change: extracting `ProviderManagerContract` interface from `ProviderManager` so AutoRizz can swap in its own mode-aware provider manager via Hilt

## Build Commands

```bash
# Set JAVA_HOME if not in PATH
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build specific flavor (supabase or cloudflare)
./gradlew assembleSupabaseDebug
./gradlew assembleCloudflareDebug

# Install on connected device
./gradlew installSupabaseDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedSupabaseDebugAndroidTest

# Clean build
./gradlew clean assembleSupabaseDebug
```

Requires JDK 21, Android SDK 35. `local.properties` must contain `SUPABASE_URL` and `SUPABASE_ANON_KEY` (or `CF_WORKER_URL` for cloudflare flavor).

## Supabase CLI

```bash
export SUPABASE_ACCESS_TOKEN="<pat>"

# Push database migrations
supabase db push

# Deploy edge functions
supabase functions deploy llm-proxy --no-verify-jwt
supabase functions deploy version-check --no-verify-jwt

# List secrets
supabase secrets list
```

Project is linked to ref `viuroypmbtfikmtisahx` (us-east-2).

## Architecture

### Fork Pattern (ProviderManagerContract)

The entire fork hinges on one interface substitution:

```
AgentLoop / SettingsViewModel inject ProviderManagerContract
    ↓
AutoRizzModule binds AutoRizzProviderManager as ProviderManagerContract
    ↓
AutoRizzProviderManager checks ModeManager.currentMode:
    BYOK → delegates to ProviderManager (direct API keys, CellClaw's original)
    PRO  → delegates to ProxyProvider (server proxy, credit deduction)
```

This means all CellClaw code that uses `ProviderManagerContract` automatically gets mode-aware routing without modification.

### Three-Tier Hilt Modules

1. **AppModule** (`com.cellclaw.di`) — CellClaw core: Room DB, 4 LLM providers, ToolRegistry with 34 tools
2. **AutoRizzModule** (`com.autorizz.di`) — Single binding: `AutoRizzProviderManager → ProviderManagerContract`
3. **BackendModule** (flavor source set) — 5 backend service bindings, different per flavor

Only one flavor's BackendModule compiles per build. The manifest points to `com.autorizz.AutoRizzApp` (extends `CellClawApp`, is the `@HiltAndroidApp` root).

### Backend Interface Layer

Five interfaces in `com.autorizz.backend` with flavor-specific implementations:

| Interface | Supabase impl | Used by |
|-----------|--------------|---------|
| `AuthService` | `SupabaseAuthService` | `AuthManager` |
| `CreditService` | `SupabaseCreditService` | `CreditManager` |
| `ProxyService` | `SupabaseProxyService` | `ProxyProvider` |
| `SyncService` | `SupabaseSyncService` | `SyncEngine` |
| `ProfileService` | `SupabaseProfileService` | Settings UI |

Flavor source sets: `app/src/supabase/`, `app/src/cloudflare/`, `app/src/pocketbase/`

### Agent Loop

`AgentLoop` runs a think→tool→observe cycle:
1. Builds `CompletionRequest` with system prompt, conversation history, and tool schemas
2. Calls `providerManager.activeProvider().complete(request)`
3. Parses response `ContentBlock`s (Text, ToolUse, ToolResult, Image)
4. If `stopReason == TOOL_USE`: checks approval policy → executes tools → adds results to history → loops
5. If `stopReason == END_TURN`: emits events, returns to IDLE
6. After tool-calling runs, auto-activates heartbeat monitoring

Key types in `com.cellclaw.provider`: `Provider` (interface with `complete`/`stream`), `CompletionRequest`, `CompletionResponse`, `Message`, `ContentBlock` (sealed class), `StopReason`.

### Heartbeat System

`HeartbeatManager` does periodic check-ins for long-running tasks. Backs off from 5s → 60s when idle, resets to 5s when the agent acts. Heartbeat exchanges that return `HEARTBEAT_OK` are pruned from conversation history to avoid context pollution.

### Approval System

`AutonomyPolicy` maps tools to `AUTO`/`ASK`/`DENY` per profile (Full Auto, Balanced, Cautious). `ApprovalQueue` uses `CompletableDeferred` to suspend the agent loop until the user responds.

## Key Conventions

- **Two packages, one module**: `com.cellclaw` (upstream) and `com.autorizz` (fork additions) coexist in the single `:app` module
- **Namespace is `com.cellclaw`**, applicationId is `com.autorizz` — `BuildConfig` is accessed as `com.cellclaw.BuildConfig`
- **Adding a tool**: Create in `com.cellclaw.tools`, inject via Hilt, register in `AppModule.provideToolRegistry()`, add approval policy in `AutonomyPolicy`
- **Adding a backend service**: Define interface in `com.autorizz.backend`, implement in each flavor source set, bind in each flavor's `BackendModule`
- **ProGuard**: Release builds use R8. kotlinx.serialization and Room entities have keep rules in `proguard-rules.pro`. Add keep rules for any new `@Serializable` classes
- **Supabase trigger functions** need `SET search_path = public` to work with Supabase's schema isolation
