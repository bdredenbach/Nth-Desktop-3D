# Changelog

## 0.3.1 — 2026-11-13

- Register every displayed/restored widget through AppWidgetHost.createView so initial content and live RemoteViews updates reach the desktop.
- Keep widget provider dimensions in pixels and send size options in dp; remove double density conversion.
- Give widgets, apps and folders one persisted cell grid. Migrate existing overlapping layouts without deleting items; keep dense desktops scrollable.
- Add direct widget hold/drag with a drop outline, nearby shortcut/folder reflow, rejection of widget-on-widget drops, and grid-aligned edge resizing.
- Preserve pending widget ID, page and configuration stage across Activity recreation; cancel/deallocate failed setup and use the host's configuration API.
- Add a separate-UID widget fixture and native content/update/gesture/lifecycle tests, plus exhaustive occupancy policy tests.
- Retain the permanent 0.3.0 test signing certificate; versionCode increases to 10078.

## 0.3.0 — 2026-11-13

- Establish a permanent, dedicated 3D test-update certificate with a private owner recovery backup.
- Read the version from committed version.properties for identical local and CI versioning; versionCode 10077 follows the latest debug build's 10076.
- Add signed release packaging that checks package, version, non-debuggable mode and the exact expected certificate before delivering an APK.
- Support verifying update compatibility against a previous APK and publish source/checksum metadata with signed releases.
- Retain the latest direct widget hold/drag and bounds fixes from d42f901.
- Moving from earlier debug certificates to this dedicated test key requires a one-time migration. The key and passwords are excluded from Git.

## 0.2.0 — 2026-11-11

- Replace the restored shortcut grid to avoid Android GridLayout's column-count exception on startup.
- Separate horizontal page gestures from vertical scroll intent; cancel pending background holds on movement and modal entry.
- Keep the desktop dock independent of keyboard insets and place panels inside the visible safe rectangle.
- Refresh the shell with a restrained graphite palette, raised icons, an airy clock, original line icons and a floating dock.
- Add scene preview cards and an always-available Try this scene button below the scrolling choices.
- Use measured pivots and gentler 3D menu rotations; correct crystal lighting normals and place the sculpture below the usual shortcut rows.
- Add keyboard/cutout geometry regression tests and a restored 24-shortcut/recreation Android test fixture.
- Preserve the application ID, layout schema and development signing certificate for an in-place update from the supplied 0.1.0 APK.

## 0.1.0

Initial development build: four GPU scenes, tilt parallax, perspective desktops,
animated menus, app discovery/launching, folders, persistent shortcuts and Nth tools.
