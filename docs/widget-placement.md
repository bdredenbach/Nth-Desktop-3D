# Widgets and shared grid placement (0.3.1)

The previous desktop instantiated AppWidgetHostView directly and called setAppWidget.
That did not register the view with AppWidgetHost and did not obtain the current
RemoteViews from Android. A widget could be bound and recorded but stay blank.
LauncherWidgetHost now creates the custom view through its onCreateView override;
DesktopView always uses AppWidgetHost.createView. Provider updates and restored
widgets use that same path. Provider failures produce a visible retry/remove
placeholder and a diagnostic log, rather than silently disappearing.

AppWidgetProviderInfo dimensions supplied by Android are already pixels. Grid
span calculation uses them directly. Only the updateAppWidgetSize options are
converted to dp. Target cell sizes and provider resize limits are respected.

The Nova reference recording 226691.mp4 shows a temporary cell outline, direct
hold/drag, and resize grips. This implementation uses the same interaction pattern:

- Tap normally to interact with the provider's content.
- Hold the widget itself, then move to drag; hold and release without moving for
  its available actions. There is no separate drag holder.
- During dragging, show a cell outline and grid marks. Move displaced app/folder
  shortcuts to nearby free cells. Keep other widgets fixed.
- Invalid widget-on-widget/out-of-bounds drops restore the prior layout.
- Resize shows edge grips for the provider's supported axes. Changes snap to
  cells and reserve the entire rectangle before moving displaced shortcuts.

DesktopGrid is the pure occupancy policy. Apps/folders persist cellX/cellY in
LauncherStore, independently of Gravity Playground's physics x/y. Widgets persist
cell coordinates and spans keyed by their Android appWidgetId. Legacy normalized
shortcut and pixel widget positions are converted and repaired deterministically.
Normal desktops share one scrollable workspace, so dense layouts do not stack
items or silently put them outside an unscrollable screen. Newly added widgets
are revealed immediately. Gravity's moving shortcuts are kept out of widget
rectangles by the physics collision step.

Pending selection tracks the original page, ID and picker/bind/configuration
stage in preferences. Cancelled/failed configuration deallocates its ID and never
commits an unusable widget. Reconfiguration uses the host API too. Listening
follows onStart/onStop, and host view references are released on rebuild/destroy.

Tests use widgetfixture, an independently installed Android package/UID with real
RemoteViews, a PendingIntent click, mandatory configuration and external updates.
It is never bundled in the launcher APK. The native scenario bypasses the system
picker UI by delivering a bound selection result; it tests the real AppWidget
service, configuration activity, content delivery, native gestures, restoration,
rotation and removal. Physical Pixel 10/provider-specific behavior still needs
device testing. Check the exact source commit's Android build run for results.

Primary references:
- https://developer.android.com/develop/ui/views/appwidgets/host
- https://developer.android.com/reference/android/appwidget/AppWidgetHost
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/appwidget/AppWidgetProviderInfo.java
