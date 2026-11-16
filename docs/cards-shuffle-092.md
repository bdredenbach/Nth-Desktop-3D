# Desktop Crystal Cards and scene shuffle — Trial 4, version 0.9.2

The owner requested the next queued trial together with a scene randomizer. The prior preset feature remains available for phone review. Trial 5 stays queued until feedback on this APK.

## Desktop cards

Open 3D settings → Cards → Place card on desktop, or Widgets → Add Crystal Card. Choose Media controls, Nth Nota AI, Weather, Calendar, Photos, Reading, Notes or App cards. The same working native card content and actions are reused in a single-card desktop surface. Permission and empty states remain explicit; media controls require active Android media sessions, calendar requires calendar access, and weather requires a chosen city or optional approximate location.

Tap controls normally and scroll inside a long card. Hold, then drag to move the card; hold and release for Open card, Refresh, Resize, Move to another desktop or Remove card. Resize uses the existing full-edge widget-style grips and collision planner. Cards reserve their whole rectangle against Android widgets and other cards. App shortcuts can reflow into free cells. One card of each type can be placed per desktop. Removing a surface keeps shared notes, links and photos. The card switches to a full panel through Open card; its placement does not depend on the panel visibility toggle.

Cards use scene-matched glass, shadows and bounded tilt. Reduced motion/battery saving suppress their local tilt. Leaving a desktop, covering it with a launcher panel or leaving the Activity pauses observers; replacing/removing its view disposes callbacks, pending location requests and workers. Saved photos and network results cannot update a disposed view. No new Android widget provider or background service is introduced.

Canonical page items hold card identity, type, coordinates and spans. Reordering desktops, desktop backups and saved presets retain them. Notes and other personal card data stay shared across presets. The backup writer is version 3; this version still reads version 2 backups. Older launchers should not be used to restore version 3 documents containing cards. Native desktop management and settings previews draw lightweight representations without starting card data services. Gravity mechanics retain their existing integration and use the same rectangular obstacle reservation as Android widgets.

## Scene randomizer

Open 3D settings → Scene randomizer or Scene studio → Scene randomizer settings. **Shuffle 3D scene now** is also in Scene studio and desktop actions. Manual shuffle switches to a built-in scene. Select any nonempty subset of the eleven built-in scenes. Automatic shuffle is off by default, with a one-hour interval ready if enabled. Available intervals are 15/30 minutes and 1/3/6/24 hours.

Shuffle uses a saved shuffled bag: selected scenes appear once per cycle and adjacent cycles avoid an immediate repeat when more than one scene is selected. A single selected scene stays stable. Selection, interval and remaining bag survive restarting the app. A manual scene/source/configuration change starts a fresh interval; preset/backup restore clears a stale deadline and begins a fresh interval for the restored scene.

Automatic changes run only on visible, focused, idle Home, after a five-second return/touch grace period. Menus, other apps, desktop drag/resize/page transitions, setup, widget requests, photo/custom/Android wallpapers and battery saving pause automatic changes. When returning after a missed deadline, at most one scene changes and a new interval starts. No alarm, job, foreground service or background wakeup is scheduled. Scene selection retains launcher placements and uses the existing scene-lighting pipeline.

## Verification

The dedicated `cards` native suite covers real placement controls, note editing and refresh, native pointer hold/drag/edge resize, page disposal/recreation, backup/preset geometry and shared data, invalid records, previous backup compatibility, selected-scene cycles, interval deadlines, pause rules and a single overdue shuffle on idle Home. JVM tests cover selected-scene bags and rectangular card collision/reflow. Google/release builds and lint passed locally (zero errors, 85 warnings per variant), all 108 JVM tests passed, and the Android-test APK compiled. Observed native results are recorded below before delivery. Phone appearance, permissions with the owner's real apps and performance still require review.
