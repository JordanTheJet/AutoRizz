# Swipe Session
Run an automated swiping session across enabled dating apps.

## Trigger
start swiping

## Steps
1. Load user preferences using dating.prefs.get
2. Check which enabled apps have remaining daily likes
3. For each app with remaining likes:
   a. Launch the dating app using app.launch
   b. Navigate to the swipe/discover feed
   c. Set heartbeat context: "Swiping on {app_name}"
   d. Read the current profile using screen.read (and vision.analyze if bio/prompts are insufficient)
   e. Send profile data to the LLM along with user preferences to decide LIKE or PASS
   f. Execute the swipe decision using app.automate
   g. If a match popup appears, dismiss it and record the match using dating.match.record
   h. Handle any premium/paywall popups by dismissing them
   i. Repeat until daily limit is reached or no more profiles
   j. Move to the next enabled app
4. Report results: "Swiped on {n} profiles across {apps}. {likes} likes, {passes} passes."

## Tools
- dating.prefs.get
- dating.match.record
- app.launch
- app.automate
- screen.read
- screen.capture
- vision.analyze
- heartbeat.context
