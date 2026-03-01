# Check Conversations
Check for new messages from matches and send replies.

## Trigger
check conversations

## Steps
1. List matches with status=conversing using dating.match.list
2. For each active conversation:
   a. Open the conversation in the dating app using app.launch and app.automate
   b. Read new messages using screen.read
   c. If there are new messages from the match:
      i. Log received messages using dating.convo.log with direction=received
      ii. Get conversation history using dating.convo.history for context
      iii. Generate a contextual reply that matches the user's conversation style
      iv. If the conversation suggests a date has been agreed upon (day, time, location confirmed), schedule it using dating.date.schedule
      v. Otherwise, type and send the reply using app.automate
      vi. Log the sent reply using dating.convo.log with direction=sent
   d. If match hasn't responded in 48+ hours, update status to stale using dating.match.update
3. Report: "Checked {n} conversations. Sent {replies} replies. {dates} dates scheduled."

## Tools
- dating.match.list
- dating.match.update
- dating.convo.log
- dating.convo.history
- dating.date.schedule
- dating.prefs.get
- app.launch
- app.automate
- screen.read
