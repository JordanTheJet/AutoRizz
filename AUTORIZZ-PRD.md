# AutoRizz — Product Requirements Document

## Autonomous Dating App Agent for Android

**Base**: CellBreak prosumer layer → CellClaw open-source core
**Repo**: [github.com/jordanthejet/autorizz](https://github.com/jordanthejet/autorizz)
**Target Apps**: Hinge, Tinder, Bumble
**Backend**: Supabase (own project — separate from CellBreak / Tian)
**Model**: BYOK (free) or Pro Credits — same architecture as CellBreak, own deployment
**Platform**: Android (minSDK 26, targetSDK 35)
**Language**: Kotlin 2.1 / Jetpack Compose / Hilt
**Last Updated**: 2026-02-28

---

## 1. Overview

AutoRizz is a CellBreak fork purpose-built for dating app automation. It takes the user's preferences (who they want to match with), auto-swipes across Hinge, Tinder, and Bumble, carries on conversations with matches to schedule dates, and adds confirmed dates to the user's calendar with context notes. It does all of this while avoiding premium paywalls on each platform.

AutoRizz inherits everything from CellBreak — the CellClaw agent loop, 33 tools, accessibility-based screen control, heartbeat system, LLM providers, approval system, plus CellBreak's BYOK/Pro dual-mode system, Supabase auth, credit packs, cloud sync, and usage dashboard. On top of that foundation, it adds dating-specific workflows, preference management, match tracking, conversation strategies, and date scheduling.

**Product Spectrum:**

```
CellClaw (Open-Source)    CellBreak (Prosumer)    AutoRizz (Dating)       Tian (Consumer)
─────────────────────     ──────────────────      ─────────────────       ───────────────
BYOK only                 BYOK or Credits         BYOK or Credits         Managed only
No accounts               Account optional        Account optional        Account required
Local only                Local or Cloud           Local or Cloud          Cloud-first
General-purpose agent     General-purpose agent   Dating-specialized      General-purpose agent
Developer audience        Early adopters          Dating app users        Everyone
Free                      Free + credit packs     Free + credit packs     Subscription tiers
```

---

## 2. Relationship to CellClaw & CellBreak

AutoRizz is a **vertical fork** of CellBreak — it inherits everything and adds a dating-specific layer.

```
┌──────────────────────────────────────────────────────────────┐
│                        AutoRizz                               │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               CellBreak (Prosumer)                       │ │
│  │                                                          │ │
│  │  ┌───────────────────────────────────────────────────┐   │ │
│  │  │          CellClaw Core (Open-Source)               │   │ │
│  │  │   Agent Loop, 33 Tools, Providers, Accessibility, │   │ │
│  │  │   Approval, Memory, Skills, Heartbeat, Overlay    │   │ │
│  │  └───────────────────────────────────────────────────┘   │ │
│  │                                                          │ │
│  │  + Optional Auth (Supabase)                              │ │
│  │  + Optional Cloud Sync                                   │ │
│  │  + Credit System + LLM Proxy                             │ │
│  │  + Usage Dashboard                                       │ │
│  │  + Mode Manager (BYOK ↔ Pro)                             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  + Swipe Preferences Engine                                   │
│  + Per-App Automation Strategies (Hinge / Tinder / Bumble)   │
│  + Match Detection & Tracking                                 │
│  + AI Conversation Manager (goal: schedule a date)            │
│  + Date Scheduling + Calendar Integration                     │
│  + Anti-Pay / Paywall Avoidance                               │
│  + Dating-Specific UI (Preferences, Matches, Dates, Stats)    │
│  + Dating Data Cloud Sync (Pro mode)                          │
└──────────────────────────────────────────────────────────────┘
```

| Direction | What Flows |
|-----------|-----------|
| CellClaw → CellBreak → AutoRizz | All core improvements — tools, providers, agent loop, accessibility, auth, sync, credits |
| AutoRizz → CellBreak | Nothing — AutoRizz is a downstream vertical fork |

---

## 3. User Modes (Inherited from CellBreak)

AutoRizz inherits CellBreak's dual-mode system. Users choose during onboarding.

### 3.1 BYOK Mode (Free)

- User provides their own API key (Anthropic, OpenAI, Google, OpenRouter)
- All data stays local (Room DB) — matches, conversations, preferences, dates
- No account, no cloud sync, no credit system
- Full access to all features
- Zero cost beyond what the user pays their AI provider directly

### 3.2 Pro Mode (Credit Packs)

- User creates an account via AutoRizz's own Supabase project (email, Google, or Apple sign-in)
- LLM requests route through AutoRizz's own server-side proxy using our API keys
- Credits consumed per request (same pack structure and pricing as CellBreak, own credit pool)
- Dating data (matches, conversations, preferences, dates) syncs to cloud
- Cross-device sync — check your matches from any device
- 50K welcome credits on first sign-up
- **Separate from CellBreak** — AutoRizz accounts, credits, and data are fully independent. Users who also use CellBreak have separate accounts and balances.

### 3.3 Credit Impact for Dating

Dating automation is LLM-intensive. Each swipe decision, conversation message, and profile analysis costs credits. Approximate costs per session:

| Action | Approximate Credits (Sonnet 4.6) |
|--------|----------------------------------|
| Single swipe decision (text-only profile) | ~2,000 credits |
| Single swipe decision (with vision analysis) | ~8,000 credits |
| Full Hinge session (8 likes + passes) | ~30K–80K credits |
| Full Tinder session (100 swipes) | ~200K–400K credits |
| Single conversation reply | ~3,000 credits |
| 10-message conversation to schedule a date | ~30K credits |

**Recommendation for Pro users:** Use Standard-tier models (Haiku, GPT-4.1 Mini, Gemini Flash) for swipe decisions (1x multiplier) and Advanced-tier models (Sonnet, GPT-4.1) for conversation messages where quality matters more.

---

## 4. Target Apps

| App | Package Name | Swipe Mechanic | Messaging Rules | Free Tier Constraints |
|-----|-------------|----------------|-----------------|----------------------|
| **Hinge** | `co.hinge.app` | Like/Skip cards with optional comment on specific prompts/photos | Either party can message first | 8 free likes/day, no rewinds |
| **Tinder** | `com.tinder` | Right swipe (like) / Left swipe (pass) on full-screen cards | Either party messages first after match | ~100 right swipes/12hrs, 1 free boost/month |
| **Bumble** | `com.bumble.app` | Right swipe (like) / Left swipe (pass) | Women must message first (within 24hrs), then either party | ~25 right swipes/day, limited backtracks |

### App Selection

User selects which apps to automate during setup. Can enable/disable per-app at any time. AutoRizz only automates apps the user has installed and logged into — it does not create dating profiles.

---

## 5. Core Workflow

AutoRizz operates as a 5-phase pipeline. Each phase runs autonomously using CellClaw's agent loop and heartbeat system.

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  1. PREFS    │───▸│  2. SWIPE    │───▸│  3. MATCH    │───▸│  4. CONVO    │───▸│  5. DATE     │
│              │    │              │    │              │    │              │    │              │
│ User defines │    │ Auto-swipe   │    │ Detect new   │    │ AI messages  │    │ Schedule +   │
│ who to match │    │ on selected  │    │ matches via  │    │ with goal of │    │ add to       │
│ with         │    │ apps         │    │ notification │    │ scheduling   │    │ calendar     │
│              │    │              │    │ + screen     │    │ a date       │    │              │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### Phase 1: Preferences

User configures who they want to swipe on. Preferences are stored in Room DB (BYOK) or synced to Supabase (Pro) and provided to the LLM as context during swipe decisions.

| Preference | Type | Example |
|-----------|------|---------|
| **Age range** | Min/max integers | 22–30 |
| **Distance** | Max miles/km | Within 15 miles |
| **Gender** | Selection | Women, Men, Everyone |
| **Interests** | Free-text keywords | "hiking, cooking, dogs, travel" |
| **Deal-breakers** | Free-text negatives | "smoking, no bio, only group photos" |
| **Photo preferences** | Free-text | "at least 3 photos, must have a clear face pic" |
| **Bio keywords** | Positive signals | "adventurous, foodie, active" |
| **Vibe / Personality** | Free-text | "witty, flirty, laid back" — used for conversation tone |
| **Conversation style** | Selection | Casual, Flirty, Direct, Funny |
| **Date preferences** | Free-text | "coffee dates, drinks, outdoor activities" |
| **Schedule availability** | Day/time ranges | "Weekday evenings, weekend afternoons" |

Preferences are injected into the system prompt so the LLM can make swipe decisions and conversation choices that align with the user's taste.

### Phase 2: Swiping

AutoRizz opens each enabled dating app and swipes through profiles using CellClaw's accessibility-based screen control.

**How it works:**

1. Launch the dating app via `app.launch`
2. Read the current profile card via `screen.read` + `screen.capture`
3. Send profile data (name, age, bio, prompt answers, photo descriptions) to the LLM along with user preferences
4. LLM decides: **Like** or **Pass**
   - On Hinge: If liking, LLM can optionally generate a comment on a specific prompt/photo
5. Execute the swipe via `app.automate` (tap like/pass button, or swipe gesture)
6. Handle popups (premium upsells, "it's a match" screens, rate limit warnings)
7. Continue until daily free limit is reached or no more profiles
8. Move to next enabled app
9. Heartbeat system keeps the loop alive across the full session

**Swipe Decision Context (sent to LLM):**

```
You are swiping on {app_name} for {user_name}.

Their preferences:
- Age range: {age_range}
- Interests: {interests}
- Deal-breakers: {deal_breakers}
- Photo preferences: {photo_prefs}
- Bio keywords: {bio_keywords}

Current profile:
- Name: {name}, Age: {age}
- Bio: {bio}
- Prompts: {prompts}
- Photos: {photo_descriptions from vision.analyze}

Decide: LIKE or PASS. If LIKE on Hinge, optionally write a comment.
Explain your reasoning briefly.
```

**Cost Optimization:** For swipe decisions, AutoRizz defaults to a Standard-tier model (cheaper). Vision analysis is only invoked when the text-based profile is insufficient (no bio, no prompts). Pro users can configure this in settings.

### Phase 3: Match Detection

AutoRizz monitors for new matches through two channels:

1. **Notification listener** — CellClaw's `CellClawNotificationListener` catches match notifications from Hinge/Tinder/Bumble. Triggers match processing immediately.
2. **Periodic check** — During heartbeat polling, check each app's match/inbox screen for new unread matches.

When a new match is detected:
- Record in `matches` table (name, app, timestamp, profile summary)
- Screenshot the match profile for reference
- Sync to cloud if Pro mode
- Move to Phase 4

### Phase 4: Conversation

AutoRizz manages conversations with the goal of scheduling an in-person date.

**Conversation Strategy:**

1. **Opening message** — LLM generates an opener based on the match's profile, using the user's configured conversation style. References something specific from their profile (prompt answer, photo, bio detail).
2. **Ongoing messages** — LLM reads incoming messages via `screen.read`, generates contextual replies that:
   - Match the user's configured vibe/personality
   - Gradually steer toward suggesting a date
   - Stay natural — don't rush, don't be robotic
   - Mirror message length and energy of the match
3. **Date proposal** — When conversation has enough rapport (LLM judgment), suggest meeting up based on user's date preferences and schedule availability
4. **Logistics** — If match agrees, work out day/time/location

**Conversation Rules:**

- Never reveal it's an AI
- Keep messages concise (1–3 sentences typical)
- Ask questions to keep conversation going
- Respond within a configurable window (default: 5–30 min delay to appear natural)
- If match stops responding for 48hrs, move on (mark as stale)
- If match unmatches, record and clean up
- Bumble-specific: If user is set as the woman-first party, AutoRizz sends the first message. If not, it waits for the match to message first within 24hrs.

**Cost Optimization:** Conversation messages use an Advanced-tier model by default (quality matters for natural-sounding messages). Users can downgrade in settings.

**Message Timing:**

To appear natural, AutoRizz does not respond instantly. Configurable delay range:

| Setting | Default | Description |
|---------|---------|-------------|
| `min_reply_delay` | 5 min | Minimum wait before replying |
| `max_reply_delay` | 30 min | Maximum wait before replying |
| `active_hours` | 8am–11pm | Only send messages during these hours |

### Phase 5: Date Scheduling

When the LLM detects that a date has been agreed upon (day, time, and location confirmed), it:

1. Checks for calendar conflicts via `calendar.query`
2. Creates a calendar event via CellClaw's `calendar.create` tool
3. Populates the event with:

| Field | Content |
|-------|---------|
| **Title** | "Date with {match_name}" |
| **Date/Time** | Agreed upon time |
| **Location** | Agreed upon venue/area |
| **Description** | Notes including: which app you matched on, a summary of their profile (age, bio highlights, key interests), conversation highlights, any topics to bring up or avoid |

4. Updates the match record status to `DATE_SCHEDULED`
5. Sends the user a notification: "Date scheduled with {name} — {day} at {location}"
6. Syncs date record to cloud if Pro mode

---

## 6. Anti-Pay Strategy

AutoRizz maximizes free-tier usage and actively avoids triggering paywalls or spending money on dating apps.

### General Principles

| Principle | Implementation |
|-----------|----------------|
| **Rate-limit awareness** | Track swipe counts per app per day. Stop before hitting the paywall trigger. |
| **Popup dismissal** | Detect and dismiss premium upsell modals (Gold, Platinum, Premium, Boost prompts) via accessibility tree pattern matching |
| **No premium taps** | Never tap on "See Who Liked You", "Super Like", "Boost", "Spotlight", or any paid feature buttons |
| **Free daily maximums** | Use all free likes/swipes but never exceed into paid territory |
| **Rewind avoidance** | Never attempt to rewind/undo a swipe (paid feature on all 3 apps) |

### Per-App Limits

| App | Free Swipes/Day | Strategy |
|-----|----------------|----------|
| **Hinge** | 8 likes | Use all 8 strategically — LLM picks the best 8 profiles from what's available. Send a comment on likes when possible (higher match rate, free feature). |
| **Tinder** | ~100 right swipes / 12hrs | Swipe through quota, stop when "out of likes" screen appears. Dismiss all Gold/Platinum popups. |
| **Bumble** | ~25 right swipes / day | Use full daily quota. Dismiss Boost/Premium prompts. Never tap "Backtrack" (paid). |

### Paywall Detection

AutoRizz identifies paywall screens via `screen.read` accessibility tree patterns:

- "Get Premium" / "Get Gold" / "Get Platinum" / "Upgrade" buttons
- "Out of likes" / "Come back later" / "No more swipes" messages
- "Super Like" / "Boost" / "Spotlight" / "Rose" upsell cards
- Blur overlays covering "who liked you" sections
- Price strings (`$`, `per month`, `free trial`)

When detected: dismiss the modal (tap X, tap outside, press back) and stop swiping on that app for the cooldown period.

---

## 7. App-Specific Automation Strategies

### 7.1 Hinge

**Navigation Flow:**
```
Launch → Discover feed → Read card → Like (+ optional comment) or Skip → Next card
                                   → Match popup → Dismiss → Continue
```

**UI Elements (Accessibility Tree):**
- Profile cards with name, age, location, prompts, photos
- "Like" button (heart icon) — can like specific prompts/photos
- "Skip" button (X icon)
- Comment input field (appears after tapping like on a prompt/photo)
- Match celebration screen with "Send a message" or "Keep playing"

**Hinge-Specific Features:**
- **Comment on likes**: When liking, tap a specific prompt or photo first, then write a short comment. This is free and significantly increases match rate.
- **Prompt-based profiles**: Hinge profiles have 3 prompts (e.g., "A life goal of mine", "I'm looking for"). LLM uses these for swipe decisions and openers.
- **Standouts**: Skip the Standouts section entirely (roses cost money).
- **Most Compatible**: Swipe on daily "Most Compatible" suggestion (free).

### 7.2 Tinder

**Navigation Flow:**
```
Launch → Swipe deck → Read card → Swipe right (like) or left (pass) → Next card
                                → "It's a Match!" screen → Dismiss or Message → Continue
```

**UI Elements (Accessibility Tree):**
- Full-screen profile cards with name, age, distance, bio, interests
- Swipe gestures: right = like, left = pass
- Like button (green heart), Pass button (red X)
- "It's a Match!" overlay with "Send a Message" and "Keep Swiping"
- Premium upsell modals (Gold, Platinum, frequent)

**Tinder-Specific Features:**
- **Swipe gestures**: Use `app.automate` swipe actions rather than button taps for more natural behavior
- **Bio-light profiles**: Many Tinder profiles have minimal bios. LLM relies more on `vision.analyze` for photo-based decisions.
- **Aggressive upsells**: Tinder shows premium popups frequently. AutoRizz dismisses these immediately.
- **Top Picks**: Skip entirely (paid feature).
- **Super Like**: Never use (paid, 1 free/week is not worth the detection risk).

### 7.3 Bumble

**Navigation Flow:**
```
Launch → Swipe deck → Read card → Swipe right (like) or left (pass) → Next card
                                → Match → Wait for message (if applicable) → Respond
```

**UI Elements (Accessibility Tree):**
- Profile cards with name, age, bio, prompts, badges (verified, interests)
- Swipe gestures or tap-based like/pass
- Match screen
- Inbox with message threads
- 24hr timer on matches (women must message first)

**Bumble-Specific Features:**
- **Women-first messaging**: On Bumble, women must send the first message within 24 hours or the match expires. If the user's gender setting means they message first, AutoRizz sends an opener immediately after matching. If not, it waits for the match to message first within 24hrs.
- **Prompt answers**: Bumble profiles include prompts similar to Hinge. Use for personalized openers.
- **Badges/Interests**: Bumble shows interest badges (e.g., "Hiking", "Foodie"). Factor into swipe decisions.
- **Backtrack**: Never tap (paid feature).
- **Compliments**: Skip (paid feature on free tier).
- **24hr expiry**: Track match timestamps. If user needs to message first and hasn't, prioritize that match before it expires.

---

## 8. New Tools (on top of CellClaw's 33)

AutoRizz adds 8 dating-specific tools:

### Preference Tools

| Tool | Approval | Description |
|------|----------|-------------|
| `dating.prefs.set` | No | Set or update swipe preferences (age, interests, deal-breakers, etc.) |
| `dating.prefs.get` | No | Retrieve current swipe preferences |

### Match Tools

| Tool | Approval | Description |
|------|----------|-------------|
| `dating.match.record` | No | Record a new match (name, app, profile summary, timestamp) |
| `dating.match.list` | No | List matches with filters (app, status, date range) |
| `dating.match.update` | No | Update match status (new, conversing, date_scheduled, stale, unmatched) |

### Conversation Tools

| Tool | Approval | Description |
|------|----------|-------------|
| `dating.convo.log` | No | Log a message sent or received (match_id, direction, content, timestamp) |
| `dating.convo.history` | No | Retrieve conversation history for a match |

### Date Tools

| Tool | Approval | Description |
|------|----------|-------------|
| `dating.date.schedule` | Yes | Schedule a date — creates calendar event + updates match status. Requires approval so user confirms the date details. |

All new tools are read/write to Room DB (BYOK) or synced via Supabase (Pro). The actual app automation (swiping, typing messages) still uses CellClaw's existing `app.launch`, `app.automate`, `screen.read`, `screen.capture`, `vision.analyze`, and `calendar.create` tools.

---

## 9. Memory & State

### New Room DB Tables

**`swipe_preferences`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER PK | Auto-increment |
| `key` | TEXT | Preference key (age_min, age_max, interests, etc.) |
| `value` | TEXT | Preference value |
| `updated_at` | INTEGER | Timestamp |

**`matches`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER PK | Auto-increment |
| `remote_id` | TEXT | Supabase UUID (Pro mode sync, null in BYOK) |
| `name` | TEXT | Match name |
| `app` | TEXT | hinge / tinder / bumble |
| `age` | INTEGER | Nullable |
| `bio_summary` | TEXT | LLM-generated summary of profile |
| `profile_screenshot` | TEXT | File path to screenshot |
| `status` | TEXT | new / conversing / date_scheduled / date_completed / stale / unmatched |
| `matched_at` | INTEGER | Timestamp |
| `last_message_at` | INTEGER | Timestamp of last message |
| `synced_at` | INTEGER | Last cloud sync timestamp (Pro mode) |

**`conversation_messages`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER PK | Auto-increment |
| `remote_id` | TEXT | Supabase UUID (Pro mode sync) |
| `match_id` | INTEGER FK | References matches.id |
| `direction` | TEXT | sent / received |
| `content` | TEXT | Message text |
| `timestamp` | INTEGER | When sent/received |

**`scheduled_dates`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER PK | Auto-increment |
| `remote_id` | TEXT | Supabase UUID (Pro mode sync) |
| `match_id` | INTEGER FK | References matches.id |
| `date_time` | INTEGER | Scheduled date/time |
| `location` | TEXT | Venue or area |
| `calendar_event_id` | TEXT | Android calendar event URI |
| `notes` | TEXT | Where you met, profile summary, conversation highlights |
| `status` | TEXT | scheduled / completed / cancelled / no_show |

**`swipe_sessions`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER PK | Auto-increment |
| `app` | TEXT | hinge / tinder / bumble |
| `started_at` | INTEGER | Session start |
| `ended_at` | INTEGER | Session end |
| `profiles_seen` | INTEGER | Count of profiles viewed |
| `likes` | INTEGER | Right swipes / likes |
| `passes` | INTEGER | Left swipes / passes |
| `hit_limit` | BOOLEAN | Whether free limit was reached |

### Swipe Tracking

AutoRizz tracks daily swipe counts per app to stay within free-tier limits:

| App | Daily Limit | Reset Logic |
|-----|------------|-------------|
| Hinge | 8 likes | Resets at midnight local time |
| Tinder | ~100 likes | Resets ~12hrs after first swipe |
| Bumble | ~25 likes | Resets at midnight local time |

### Cloud Sync (Pro Mode)

In Pro mode, all data syncs to AutoRizz's own Supabase project. The SyncEngine (forked from CellBreak) handles these entity types:

| Entity Type | Sync Direction | Conflict Resolution |
|-------------|---------------|---------------------|
| `swipe_preferences` | Bidirectional | Latest-write-wins |
| `matches` | Bidirectional | Latest-write-wins |
| `conversation_messages` | Bidirectional | Append-only (messages never edited) |
| `scheduled_dates` | Bidirectional | Latest-write-wins |
| `swipe_sessions` | Push-only (device → cloud) | No conflict (device-originated) |

---

## 10. Server-Side Infrastructure (Own Supabase Project)

AutoRizz runs its own Supabase project — separate from CellBreak and Tian. Same architecture (auth, PostgreSQL, edge functions, realtime), own deployment. This means:

- **Own user table** — AutoRizz accounts are independent from CellBreak/Tian
- **Own credit pool** — Separate Stripe integration, own credit packs, own revenue
- **Own LLM proxy** — Edge function with AutoRizz's own API keys for each provider
- **Own rate limits** — Dating automation has different usage patterns than general-purpose agents
- **Independent scaling** — AutoRizz traffic doesn't affect CellBreak/Tian and vice versa
- **Clean separation** — Can sell, shut down, or pivot AutoRizz without touching other products

The client-side code (ModeManager, CreditManager, SyncEngine, ProxyProvider) is forked from CellBreak and pointed at AutoRizz's Supabase URL.

### Database Schema

```sql
-- ============================================
-- CORE TABLES (forked from CellBreak schema)
-- ============================================

-- User profiles (credits instead of subscriptions)
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id),
    display_name TEXT,
    credit_balance BIGINT DEFAULT 50000,  -- welcome bonus
    total_credits_purchased BIGINT DEFAULT 0,
    referral_code TEXT UNIQUE DEFAULT gen_random_uuid()::TEXT,
    referred_by UUID REFERENCES auth.users(id),
    auto_refill_enabled BOOLEAN DEFAULT false,
    auto_refill_pack TEXT,
    auto_refill_threshold BIGINT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Credit transactions
CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,  -- 'purchase', 'usage', 'bonus', 'referral', 'refund'
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    description TEXT,
    stripe_payment_id TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Credit packs catalog
CREATE TABLE credit_packs (
    id TEXT PRIMARY KEY,  -- 'starter', 'builder', 'power'
    name TEXT NOT NULL,
    credits BIGINT NOT NULL,
    price_usd REAL NOT NULL,
    stripe_price_id TEXT NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Agent conversations & messages (from CellClaw/CellBreak)
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Semantic memory
CREATE TABLE memory_facts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    category TEXT,
    confidence REAL DEFAULT 1.0,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Usage logs
CREATE TABLE usage_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    model TEXT NOT NULL,
    input_tokens INTEGER NOT NULL,
    output_tokens INTEGER NOT NULL,
    credits_used BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- DATING-SPECIFIC TABLES
-- ============================================

-- Swipe preferences
CREATE TABLE swipe_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, key)
);

-- Matches
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    app TEXT NOT NULL,  -- hinge / tinder / bumble
    age INTEGER,
    bio_summary TEXT,
    status TEXT NOT NULL DEFAULT 'new',
    matched_at TIMESTAMPTZ NOT NULL,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Conversation messages (dating app messages, not agent messages)
CREATE TABLE match_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    direction TEXT NOT NULL,  -- sent / received
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Scheduled dates
CREATE TABLE scheduled_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    date_time TIMESTAMPTZ NOT NULL,
    location TEXT,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'scheduled',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Swipe session logs
CREATE TABLE swipe_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    app TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    profiles_seen INTEGER DEFAULT 0,
    likes INTEGER DEFAULT 0,
    passes INTEGER DEFAULT 0,
    hit_limit BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- ROW LEVEL SECURITY
-- ============================================
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE memory_facts ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE swipe_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_dates ENABLE ROW LEVEL SECURITY;
ALTER TABLE swipe_sessions ENABLE ROW LEVEL SECURITY;
-- All policies: auth.uid() = user_id
```

### LLM Proxy (Edge Function)

Same architecture as CellBreak's proxy — a Supabase Edge Function that:
1. Validates JWT
2. Checks credit balance
3. Routes to provider based on model prefix (`claude-*`, `gpt-*`, `gemini-*`, `openrouter/*`)
4. Forwards request with AutoRizz's own server-side API keys
5. Streams response back
6. Calculates credit cost and deducts atomically

Same credit pack pricing and model multipliers as CellBreak (see CellBreak PRD Section 4).

---

## 11. UI — New Screens

AutoRizz adds 4 screens on top of CellBreak's existing UI:

| Screen | Description |
|--------|-------------|
| **PreferencesScreen** | Set swipe preferences — age range slider, interest tags, deal-breaker list, conversation style picker, date preferences, schedule availability |
| **DashboardScreen** | Overview: matches by app, active conversations, upcoming dates, today's swipe stats (likes used/remaining per app), credit balance (Pro) |
| **MatchesScreen** | List of all matches with status badges (new, conversing, date scheduled, stale). Tap to see profile summary + conversation history. |
| **DatesScreen** | Upcoming and past dates with calendar links and notes |

### Setup Flow (extends CellBreak's onboarding)

AutoRizz extends CellBreak's onboarding with 2 additional steps at the end:

```
1. Welcome Screen
   "AutoRizz — AI that gets you dates."

2. Choose Your Path (inherited from CellBreak)
   ┌─────────────────────────┐  ┌─────────────────────────┐
   │     BYOK Mode           │  │     Pro Mode             │
   │                         │  │                         │
   │  Bring your own API key │  │  We handle the AI       │
   │  Everything stays local │  │  Buy credits, we route  │
   │  No account needed      │  │  Cloud sync included    │
   │  100% free              │  │  50K free credits       │
   │                         │  │  to start               │
   │     [Select]            │  │     [Select]            │
   └─────────────────────────┘  └─────────────────────────┘

3a/3b. BYOK or Pro path setup (same as CellBreak)

4. Permissions (same as CellBreak/CellClaw)

5. App Selection (AutoRizz-specific)
   → Which dating apps to automate (shows installed: Hinge, Tinder, Bumble)
   → Must have at least one selected

6. Swipe Preferences (AutoRizz-specific)
   → Age range, gender, interests, deal-breakers
   → Conversation style (Casual / Flirty / Direct / Funny)
   → Date preferences

7. Ready
   → Dashboard opens
   → "Tap 'Start Swiping' to begin, or configure more preferences in Settings"
   → Pro users see credit balance
```

### Settings Screen Additions

```
Settings (extends CellBreak settings)
├── Mode: [BYOK / Pro]              ← inherited from CellBreak
├── (If BYOK) API Keys              ← inherited
├── (If Pro) Account                 ← inherited
├── (If Pro) Credits                 ← inherited
├── (If Pro) Cloud Sync              ← inherited
├── Provider & Model                 ← inherited
│   ├── Swipe Model                  ← NEW: model for swipe decisions (default: Standard tier)
│   └── Conversation Model           ← NEW: model for messages (default: Advanced tier)
├── Autonomy Level                   ← inherited
├── Dating Settings                  ← NEW section
│   ├── Swipe Preferences            → PreferencesScreen
│   ├── Enabled Apps                  → Toggle Hinge/Tinder/Bumble
│   ├── Conversation Style            → Casual/Flirty/Direct/Funny
│   ├── Reply Delay Range             → Min/max delay sliders
│   ├── Active Hours                  → Time range picker
│   ├── Auto-Message Matches          → Toggle (auto-send openers vs manual)
│   └── Date Approval                 → Always ask / Auto-schedule
├── Tools                            ← inherited
├── Skills                           ← inherited
└── About
```

---

## 12. Skills (Markdown-Defined Workflows)

AutoRizz ships with pre-built skills using CellClaw's skill system:

### `swipe_session.md`

```
Trigger: "start swiping", "go swipe", "rizz mode"

1. Load user preferences from dating.prefs.get
2. Check swipe counts — which apps have remaining likes
3. For each enabled app with remaining likes:
   a. Launch the app
   b. Navigate to swipe deck
   c. Read profile → decide → swipe → repeat
   d. Stop when daily limit reached or no more profiles
4. Report results: "Swiped on {n} profiles across {apps}. {likes} likes, {passes} passes."
```

### `check_matches.md`

```
Trigger: "check matches", "any new matches", "rizz report"

1. For each enabled app:
   a. Launch and navigate to matches/inbox
   b. Screen read for new matches
   c. Record any new matches
2. Report: "{n} new matches since last check" with names and apps
```

### `message_matches.md`

```
Trigger: "message matches", "start conversations", "cook"

1. List matches with status = new (no conversation yet)
2. For each, open the conversation in the app
3. Generate and send an opening message based on their profile
4. Log the message and update match status to conversing
```

### `check_convos.md`

```
Trigger: "check conversations", "any replies"

1. List matches with status = conversing
2. For each, open the conversation in the app
3. Read new messages
4. Generate and send replies
5. If date is confirmed, trigger date scheduling
```

---

## 13. Autonomy & Heartbeat

### Heartbeat-Powered Sessions

Swiping and conversation monitoring are long-running tasks. AutoRizz uses CellClaw's heartbeat system to power these:

**Swipe Session Heartbeat:**
```
Trigger swipe skill → heartbeat.context("Swiping on Tinder") → 5s polling
  → Read next profile → Swipe → reset to 5s
  → No more profiles or limit reached → heartbeat stops
```

**Conversation Monitor Heartbeat:**
```
Trigger convo check → heartbeat.context("Monitoring match conversations") → 30s polling
  → Check for new messages → Reply if needed → back off
  → No new messages → backs off (60s, 120s, ... 5min max)
  → New message arrives → reset to 30s
```

### Dating-Specific Autonomy Profile

AutoRizz adds a `DATING` autonomy profile alongside CellClaw's existing three (FULL_AUTO, BALANCED, CAUTIOUS):

| Action | Auto-Approve? |
|--------|--------------|
| Read profiles / screens | Yes |
| Swipe (like/pass) | Yes — user set preferences |
| Open conversations | Yes |
| Send messages | Configurable — auto or ask-each-time |
| Schedule a date | Always ask — user confirms details |
| Dismiss premium popups | Yes |
| Launch dating apps | Yes |
| All other CellClaw tools | Follows selected base profile (FULL_AUTO / BALANCED / CAUTIOUS) |

---

## 14. Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        AutoRizz (Kotlin)                          │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    CellBreak Layer                           │ │
│  │  ModeManager | CellBreakProviderManager | ProxyProvider     │ │
│  │  SyncEngine | CreditManager | Auth (Supabase)               │ │
│  │                                                              │ │
│  │  ┌───────────────────────────────────────────────────────┐  │ │
│  │  │                  CellClaw Core                         │  │ │
│  │  │  AgentLoop | ToolRegistry | Providers | Accessibility  │  │ │
│  │  │  Approval | Memory | Heartbeat | Overlay | Skills      │  │ │
│  │  └───────────────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐      │
│  │ Preferences  │  │ SwipeEngine  │  │ ConvoManager      │      │
│  │ Engine       │  │              │  │                    │      │
│  │ - Prefs UI   │  │ - Per-app    │  │ - Reply generation │      │
│  │ - Prefs DB   │  │   strategies │  │ - Timing delays    │      │
│  │ - LLM inject │  │ - Rate limit │  │ - Date detection   │      │
│  └──────────────┘  │   tracking   │  │ - Convo logging    │      │
│                     │ - Paywall    │  └───────────────────┘      │
│  ┌──────────────┐  │   dismissal  │  ┌───────────────────┐      │
│  │ MatchTracker │  └──────────────┘  │ DateScheduler     │      │
│  │              │                     │                    │      │
│  │ - Detection  │                     │ - Calendar create  │      │
│  │ - Recording  │                     │ - Notes generation │      │
│  │ - Status mgmt│                     │ - Status tracking  │      │
│  │ - Cloud sync │                     │ - Cloud sync       │      │
│  └──────────────┘                     └───────────────────┘      │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Room Database (SQLite)                          │ │
│  │  messages | memory_facts | scheduled_tasks                  │ │
│  │  + matches | conversation_messages | swipe_preferences      │ │
│  │  + scheduled_dates | swipe_sessions                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Supabase (Own Project — Pro Mode Only)           │ │
│  │  Auth | Core tables | Dating tables                         │ │
│  │  LLM Proxy | Realtime Sync | Credit System                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### Module Structure

```
autorizz-android/
├── cellclaw/                  # CellClaw open-source core (git submodule)
├── cellbreak/                 # CellBreak prosumer layer (git submodule)
│   └── (auth, sync, proxy, credits, mode, usage)
├── app/                       # AutoRizz app module
│   └── src/main/kotlin/com/autorizz/
│       ├── dating/
│       │   ├── prefs/         # PreferencesEngine, PreferencesRepository
│       │   ├── swipe/         # SwipeEngine, SwipeStrategy, per-app strategies
│       │   │   ├── HingeStrategy.kt
│       │   │   ├── TinderStrategy.kt
│       │   │   └── BumbleStrategy.kt
│       │   ├── match/         # MatchTracker, MatchRepository
│       │   ├── convo/         # ConvoManager, ReplyGenerator, TimingEngine
│       │   ├── date/          # DateScheduler, DateRepository
│       │   └── paywall/       # PaywallDetector, PopupDismisser
│       ├── tools/             # 8 new dating tools
│       ├── ui/                # PreferencesScreen, DashboardScreen, MatchesScreen, DatesScreen
│       ├── sync/              # Dating entity sync adapters (extends CellBreak SyncEngine)
│       └── di/                # Hilt module for dating components
└── build.gradle.kts
```

---

## 15. Security & Privacy

| Concern | Approach |
|---------|----------|
| **Dating data sensitivity** | BYOK: all data local in Room DB. Pro: synced to Supabase with RLS (user can only access their own data). |
| **Profile screenshots** | Stored on-device only. Never uploaded to cloud (even in Pro mode). |
| **Conversation content** | BYOK: sent to LLM only. Pro: sent to LLM + synced to Supabase (encrypted at rest). |
| **API keys** | Encrypted via CellClaw's `SecureKeyStore` (AES-256-GCM, Android Keystore). |
| **Server security** | Inherited from CellBreak: JWT auth, RLS, TLS 1.3, rate limiting. |
| **Account credentials** | AutoRizz never handles dating app login credentials. User must be already logged in. |
| **Data export** | Pro users can export all dating data (GDPR-style portability). |
| **Account deletion** | Pro users can delete account + all server-side dating data. |
| **No telemetry** | No analytics or tracking in either mode (inherited from CellBreak). |

---

## 16. Known Limitations

| Limitation | Impact | Mitigation |
|-----------|--------|------------|
| **UI fragility** | Dating apps update their UIs frequently, breaking accessibility tree patterns | Version-aware strategies with fallback heuristics. Quick patches when apps update. |
| **Free-tier limits are low** | 8 likes/day on Hinge limits volume | Quality over quantity — LLM picks the best profiles. Users who want more volume need premium (manual). |
| **Bumble women-first rule** | If user is not the one who messages first, must wait for match to message | Monitor for incoming messages via heartbeat. Can't control whether match messages. |
| **Photo-heavy profiles** | Vision analysis adds latency and cost per swipe | Cache photo analysis results. Option to skip vision and decide on text-only signals. |
| **App rate limiting / bans** | Apps may detect automation patterns and restrict accounts | Human-like delays between actions, randomized timing, no impossibly fast swipes. |
| **Accessibility tree text-only** | Can't read images without `vision.analyze` (costs extra tokens / credits) | Use vision selectively — only when bio/prompts are insufficient for a decision. |
| **No iOS** | Android only (CellClaw limitation) | n/a |
| **Message quality** | AI-generated messages may feel off to some matches | User reviews conversation history and can take over any conversation manually at any time. |
| **Calendar conflicts** | Date may be scheduled during an existing event | Check calendar for conflicts before confirming a time (use `calendar.query`). |
| **Credit burn rate** | Heavy swiping with vision can burn through credits quickly (Pro mode) | Default to Standard-tier models for swipes, show credit estimates before sessions. |

---

## 17. Success Metrics

### Product Metrics

| Metric | Target (3 months post-launch) |
|--------|-------------------------------|
| Total installs | 10,000 |
| BYOK → Pro conversion | 25% |
| Swipe sessions per user per week | 5+ |
| Matches per user per week | 3+ |
| Conversations that reach date scheduling | 15% |
| Dates actually scheduled per user per month | 2+ |
| User retention (30-day) | 40% |

### Business Metrics

| Metric | Target |
|--------|--------|
| Revenue (monthly, 3 months in) | $8K MRR from credit packs |
| Average revenue per Pro user | $15/month |
| Gross margin on credits | > 40% |

---

## 18. Future Enhancements

- **Date follow-up** — After a date, prompt user for how it went, update notes, decide whether to continue
- **Analytics dashboard** — Match rate by app, swipe-to-match ratio, conversation-to-date conversion rate
- **Profile optimization** — Suggest bio/prompt improvements based on what gets matches
- **Photo advice** — Suggest profile photo improvements based on match rates
- **Multi-language support** — Generate messages in the match's language
- **Location-aware date suggestions** — Suggest venues near both parties based on location data
- **Voice note support** — Generate and send voice notes on Hinge (when supported)
- **Multiple dating personas** — Different conversation styles for different apps
- **Scheduled swipe sessions** — "Swipe at 9pm every day" via CellClaw's `schedule.manage`
- **Instagram/Snapchat handoff** — Detect when a match wants to move to another platform, handle the transition
- **Hinge roses (if free)** — Monitor for any free rose promotions and use them
- **Date reminders** — Send reminder notifications before scheduled dates with outfit/topic suggestions

---

## 19. Dependencies

Inherits all CellBreak + CellClaw dependencies. No new external dependencies.

| Component | Source | Purpose |
|-----------|--------|---------|
| Agent Loop | CellClaw | Orchestrates all automation |
| 33 Tools | CellClaw | Screen control, calendar, notifications, etc. |
| Heartbeat | CellClaw | Long-running swipe and convo sessions |
| Skills | CellClaw | Pre-built dating workflow triggers |
| Accessibility | CellClaw | App automation via screen control |
| ModeManager | CellBreak | BYOK ↔ Pro switching |
| CellBreakProviderManager | CellBreak | Mode-aware LLM routing |
| SyncEngine | Forked from CellBreak | Cloud sync for dating data (Pro) — points to AutoRizz's own Supabase |
| CreditManager | Forked from CellBreak | Credit tracking and deduction (Pro) — own credit pool |
| Supabase Auth | Own Supabase project | User accounts (Pro) — independent from CellBreak/Tian |
| Room DB | CellClaw | Local persistence for all dating data |
| WorkManager | CellClaw | Scheduled swipe sessions |

---

## Appendix: Relationship to Other Products

| Project | Relationship |
|---------|-------------|
| **CellClaw** | Open-source foundation. AutoRizz inherits all core systems via CellBreak. |
| **CellBreak** | Direct parent. AutoRizz forks CellBreak's client-side code (BYOK/Pro modes, credits, auth, sync, proxy) but runs its own Supabase project and LLM proxy. |
| **Tian** | Consumer product. AutoRizz is a parallel vertical — not a predecessor to Tian. |
| **PerfectCell OS** | Custom ROM. AutoRizz would benefit from pre-granted permissions (no A11y setup friction). |
