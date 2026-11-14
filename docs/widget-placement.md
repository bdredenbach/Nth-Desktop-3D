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

The Reference recording 226691.mp4 shows a temporary cell outline, direct
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

## Widget gallery and website shortcuts (0.3.2)

Hold empty desktop space and choose Widgets. Expand an app to see its supplied preview and cell size, or search by app/widget name. Previews use previewLayout on Android 12+, then previewImage, then a clearly labelled app-icon fallback. Browsing never allocates widget IDs or executes preview button actions. Tapping a preview starts the normal binding/configuration flow and places the real hosted widget on the current desktop.

Set Nth Desktop 3D as default Home to receive browser/app Add to Home screen requests. A confirmation shows the icon, editable name and destination desktop. Accepting stores the Android shortcut package, ID and profile serial plus a local icon cache. Tapping uses LauncherApps.startShortcut, so a saved webpage opens that page. Rename, move, folders and the shared widget occupancy grid work for these shortcuts. Cancellation does not pin or save anything; repeated requests update the existing identity. Removing one copy preserves other copies in folders/desktops. Legacy INSTALL_SHORTCUT broadcasts are not accepted.
