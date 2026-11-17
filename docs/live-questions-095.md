# Opt-in live questions — 0.9.5

Use Settings → Search & Nth Nota AI → Cloud questions for Nth Nota AI, or the same switch on Ask Nota. Consent defaults off and is excluded from backups and presets; existing explicit choices survive upgrades. Typing/searching only prepares a draft. Send is required. Turning consent off, Stop, leaving the screen or lifecycle pause cancels requests and discards late answers.

## Coverage

- Ask “Who do the Buffalo Bills play next?” or include NFL/NBA/WNBA/NHL/MLB and a team name. The client resolves the team from live catalogs and directly selects the earliest future, scheduled, non-canceled fixture; the language model does not guess the opponent/date. Shared nicknames such as Giants ask for clarification. Include the league or full name. Regular-season, relevant NFL preseason and postseason schedules are checked. Local time/timezone and source link are shown; unconfirmed dates/times stay unconfirmed. Stale provider timestamps are rejected.
- General factual questions such as “What causes a rainbow?” retrieve up to three Wikipedia introduction excerpts. The existing Nth Nota service summarizes only that supplied evidence. A reply must contain valid source IDs and exact supporting text present in an excerpt. Unverifiable model output is replaced by a refusal and the real source links. A newly retrieved page is not automatically proof of a current claim.
- Writing/brainstorming replies are labeled as having no checked live facts. Launcher explanations use current setting values and cannot execute changes.
- This release does not include full web search, current news, live scores, weather or market prices. Those questions offer **Search the web**, which opens the user's browser only on tap. Missing fixtures, unsupported sources, offline access and provider/AI quota failures do not fall back to a confident answer from model memory. A source excerpt may be incomplete or out of date; source checking reduces errors, but does not make AI infallible.

## Data and privacy

All lookups run only after Send with Cloud questions enabled. The public source receives the question/search terms or team lookup; the existing Nth Nota gateway receives the question and relevant public excerpts for factual summaries. Current launcher setting values are included only for launcher-settings questions. Notes, cards, app lists and desktop contents are not appended. The existing editable Nota draft remains on the device. No new query history/cache or background lookup is added. Existing providers may log their requests; this client does not control provider retention.

Source links come from validated provider responses/fixed schedule pages, never generated model URLs. Wikipedia links include contributor/license attribution and page revision time where available. Requests use HTTPS, fixed hosts, bounded sizes/timeouts, no redirects and consent checks at transport and delivery boundaries. No keys are shipped and the shared Nth Nota service is unchanged. Public provider APIs and data availability can change.

## Checks

The dedicated `live` Android suite uses fixed provider fixtures rather than depending on online availability. It checks exact-minute timestamps, completed/canceled/postponed fixtures, postseason/preseason selection, unconfirmed times, ambiguous nicknames, absent games, stale schedules, valid/fabricated source quotes, AI quota fallback, no setting-context leakage in factual prompts, no network while off, in-flight opt-out cancellation, native Send/source links and offline local commands. JVM tests check routing and URL safety. Build/lint/unit and focused device results are recorded before delivery. Trial 7 haptics remains queued.

## Phone tests

1. With the toggle off, send the Bills question: it should explain how to opt in. “Turn off tilt” should still work.
2. Enable the switch and send “Who do the Buffalo Bills play next?” Verify the opponent/date/time in the linked schedule. Try “Who do the Giants play next?” and then specify a league.
3. Ask “What causes a rainbow?” Open the linked source and compare the explanation. Ask a current-weather question: expect the unsupported-source explanation and manual web search.
4. Start a reply and tap Stop, switch consent off, leave the page or put the app in the background. A late reply should not replace the screen.
5. Try offline. Expect a clear unavailable message, while local launcher commands keep working.
