# Feed Page — implementation specification

Status: approved default and implementation plan; runtime and APK verification pending.

## User-selected defaults
- Feed page: enabled
- Preferred provider: Google Discover companion integration, when supported
- Fallback provider: launch the installed Google app's Discover entry point, when available
- Gesture: rightward swipe from the leftmost home screen (edge swipe enabled)
- Feed-specific 3D transition: disabled; do not rotate, zoom, or tilt the feed on entry
- Crystal Glass desktop preset remains independent and unchanged

## Settings UX
Create a **Feed Page** section containing:
- Enable Feed Page (toggle, default on)
- Provider (Google Discover [default], Google App, Nth Feed [future], Off)
- Edge swipe (toggle, default on)
- 3D transition (toggle, default off)
- Companion status and setup help (installed / unavailable / incompatible)
- Test feed button; graceful messaging when no compatible provider is available

Persist preferences per user; never reset on upgrade. If Google Discover companion is unavailable, retain the preferred choice while using the fallback; do not silently change the saved selection.

## Integration architecture
Define FeedProvider interface with availability check, open action, and human-readable status. Use a DiscoverCompanionProvider only after a compatibility probe and successful handshake; do not assume another launcher’s companion is interoperable with Nth Desktop 3D. Google's native Discover integration is not a general public third-party launcher API. Do not promise embedded feed support until proven on target Android versions.

Implement GoogleAppFallbackProvider using only documented, resolvable package/intent entry points and Android package visibility declarations as required. Avoid brittle private intents where possible. If Google app unavailable, offer Play Store/setup guidance rather than crashing.

## Gesture behavior
Recognize the swipe only from the leftmost home page, using direction/velocity thresholds and touch-slop. Respect Android system back gestures and accessibility. Suppress triggering while dragging icons, scrolling a widget, interacting with an open window, or opening a context menu. Do not launch repeatedly from the same swipe. Return via normal Android navigation or an explicit close affordance.

## Tests
- Companion available / unavailable / incompatible
- Google app installed / disabled / absent
- Portrait / landscape; system navigation gesture conflicts
- First page vs subsequent pages; folders and widgets
- 3D transition disabled while other Crystal Glass effects remain enabled
- Cold start, resume, repeated swipes, no ANRs
- User-selected provider persists across restarts and upgrades
- Pixel 10 device verification

## Delivery order
1. Persisted Feed Page settings and provider abstraction
2. Edge-swipe gesture with robust conflict handling
3. Google app fallback and failure UX
4. Investigate companion protocol and compatibility on real device
5. Integrate companion only if demonstrably supported
6. Regression test and produce APK
