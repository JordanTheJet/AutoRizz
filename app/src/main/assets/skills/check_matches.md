# Check Matches
Check for new matches across all enabled dating apps.

## Trigger
check matches

## Steps
1. For each enabled dating app:
   a. Launch the app using app.launch
   b. Navigate to the matches/inbox screen
   c. Read the screen using screen.read to identify new unread matches
   d. For each new match found, record it using dating.match.record with name, app, and profile summary
2. List all recorded matches using dating.match.list with status=new
3. Report: "{n} new matches since last check" with names and which apps they're from

## Tools
- dating.match.record
- dating.match.list
- app.launch
- app.automate
- screen.read
