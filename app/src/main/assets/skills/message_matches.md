# Message Matches
Send opening messages to new matches that haven't been messaged yet.

## Trigger
message matches

## Steps
1. List matches with status=new using dating.match.list
2. For each new match:
   a. Open the conversation in the dating app using app.launch and app.automate
   b. Read their profile details using screen.read for context
   c. Generate a personalized opening message based on their profile and the user's conversation style preferences
   d. Type and send the message using app.automate
   e. Log the sent message using dating.convo.log with direction=sent
   f. Update match status to conversing using dating.match.update
3. Report: "Sent opening messages to {n} matches"

## Tools
- dating.match.list
- dating.match.update
- dating.convo.log
- dating.prefs.get
- app.launch
- app.automate
- screen.read
