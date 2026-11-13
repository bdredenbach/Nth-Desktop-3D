# Native Nth Feed — 0.6.0

Install only Nth Desktop 3D. Swipe right on Desktop 1, or use Settings > Feed page > Open Nth Feed. Nth Feed is the new default, independent of Google and of the former optional companion. The one-time migration changes an unset or former Google-overlay default; feed enabled/off and the explicit Google-app provider remain intact. Later provider selections are preserved.

## Working controls

- News: attributed crystal headline cards, excerpts, publication dates, publisher reader, Save and story menu (more of this topic or hide).
- Saved: up to 200 saved headlines. Headlines remain available offline; full publisher pages require a connection and may enforce their own access rules.
- Sources: enable/remove publishers, add a named HTTPS RSS/Atom URL, choose one of seven interests, restore hidden stories and reset reading preferences. Up to 12 sources are supported.
- Refresh: automatically when opening a cache older than 15 minutes, or manually. Fetches run while the feed is open. Partial successes update their own publisher; failed publishers retain cached entries. Failures are explicit. No background polling or paid feed API is used.
- Local ordering: chosen interests first, then unread stories, then publication time. This is a simple local ranking system, not Google's personalized Discover engine.
- Search: title, excerpt, topic and source in the cached feed or saved list. Each screen renders at most 60 matches; up to 240 recent entries are cached. No server-side search or reading-history upload.
- Reader: publisher pages inside Android WebView, origin shown, back/feed return, reload and optional external browser. File/content access and mixed HTTP content are disabled, third-party cookies are disabled, and no JavaScript interface is exposed. HTTPS navigation only. The publisher controls its own content, cookies, advertising and sign-in/paywall.
- Crystal cards: existing media, weather, calendar, photos, notes and reading controls remain accessible from the feed and launcher.

## Sources and attribution

Initial sources are BBC News World, BBC News Technology and NASA's public RSS feed:

- https://feeds.bbci.co.uk/news/world/rss.xml
- https://feeds.bbci.co.uk/news/technology/rss.xml
- https://www.nasa.gov/feed/

Publisher names and original links remain visible. This is a personal feed reader; it does not redistribute a full article archive or claim affiliation. Added sources must provide RSS or Atom at the given URL. Invalid feeds and inaccessible URLs display a refresh failure. RSS/Atom parser limits: 2 MB, 60 items per source, bounded text/depth, no DTDs/external entities, HTTPS story links only. Publisher page rendering is separate from feed parsing.

## Optional Google provider

Settings > Feed provider also offers experimental direct Google Discover, separate Google App, and Off. Production Google must be installed and must accept the private overlay connection for direct Discover. The separate-UID native fixture proves protocol/window behavior, not that a production Google release accepts the signed launcher. No helper installation is requested or shipped. Nth Feed remains fully usable without it.

## Data and updates

Sources, interests, reading flags, hidden stories, saved headlines and cache are device-local in the app's private preferences. They are not part of a settings-only export. No Google login, mobile plan or extra account is required. Existing layouts, widget bindings, notes and R2 signing identity remain unchanged. Update the 0.5.0 R2 installation in place.
