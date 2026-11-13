# Settings polish, crystal cards and Discover — 0.5.0

Owner references: 226825.mp4 (settings scroll overflow), 226826.png (long labels),
and 226768.mp4 (Cards and Google feed settings).

## Settings and labels

The settings list clips all drawing to its own viewport. Its first 28 dp use a
transparent alpha mask once scrolled, so rows disappear below the fixed preview
and cannot paint across the preview, search field or heading. Reduced motion
still applies; the fade itself is static transparency rather than a motion effect.

New/unset fading menu preferences are enabled, strength 90, feather 80 dp, and
empty-area opacity zero. Explicit saved values survive updates and restarts.
All home, drawer, folder and preview icon labels use the same formatter: eight
Unicode code points and `...` when the full name is longer. Labels reserve one
line regardless of name length; full names remain in stored identities, search,
menus and accessibility descriptions. No shortcut/folder is renamed by this change.

## Crystal cards

Cards use the current crystal material, highlights, shadows and subtle tilt.
Open Crystal cards from the app drawer, the Cards search action or Settings >
Cards. Choose Nth Feed as the feed provider for a left-hand cards page.
Visibility toggles are persistent and included in settings exports; card content
(photo grants, saved links, cached weather) is device-local and is not part of
settings-only exports.

| Card | Working source and actions |
| --- | --- |
| Media controls | Android active media sessions after explicit media access; track/artist and supported previous/play/pause/next actions, player opening |
| Nth Nota & AI | Nth Nota commands/cloud consent already implemented; installed ChatGPT/Gemini app links |
| Weather | City chosen by the user, Open-Meteo geocoding/current weather, timestamps, 30-minute cache, C/F and refresh; no invented weather or automatic location request |
| Calendar | Android Calendar Provider after read permission, next seven days, open/add event; Outlook data appears when that account syncs into Android calendars |
| Photos | Up to four explicitly chosen document-picker images, persisted read grants, open and remove |
| Reading | Saved http/https links with custom titles and removal |
| Notes | The existing saved Nth Notes content and editor |
| App cards | Installed Outlook, Drive, Dropbox, Discord and Twitch launch actions |

App launch links do not claim to fetch those providers' account libraries,
messages or cloud files. Provider-specific OAuth clients are not configured.

## Embedded Google Discover

With Feed enabled and Google Discover selected, a right swipe on Desktop 1
attempts the Google window-overlay protocol. The connection exchanges the real
window token/configuration, receives status and scroll callbacks, moves the home
surface with the overlay, and resets on close, pause, rotation or disconnect.
It never starts a separate Google activity as an undisclosed substitute.

The optional `Nth-Discover-Bridge-0.5.0.apk` is included in the download ZIP. Install
it alongside the launcher and keep the Google app enabled/updated, then use
Settings > Feed page > Reconnect Google Discover. No root or mobile plan is used.
The bridge has no launcher icon and exports only a signature-protected service.
Only apps signed by the owner's R2 key can use it. Its separate debuggable build
is intentional for third-party Google overlay compatibility; the launcher APK
remains a non-debuggable release. Both APKs use the owner's existing R2 key.

Without an accepted overlay connection the launcher shows setup, reconnect,
Open Google app and Crystal cards actions. Feed off prevents boundary opening.
The Google app controls its own feed styling and availability; Nth cannot restyle
Google's remote content into crystal cards. Nth's own cards use crystal surfaces.
Google can change this private protocol. The native fixture validates cross-UID
protocol/window behavior, not the content of an installed production Google app.

## Remaining reference-video boundaries

The concrete settings fixes, cards and overlay integration above are delivered.
This is not a claim that every Nova feature/account integration has been cloned.
Third-party icon packs, unlimited/custom dock pages, the complete Nova transition
library, accessibility-based system gestures and remote provider account APIs
remain outside this implementation. No unsupported integration is disguised as
an active account connection.
