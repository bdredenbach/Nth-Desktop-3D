# 0.9.2 — 2026-11-16

- Trial 4: place native Crystal Cards on desktop pages with real data/actions, hold-to-move, full-edge resize, page moves and removal. Keep card geometry in presets and version 3 backups while accepting version 2 backups and retaining shared content.
- Add selected-scene shuffle and optional timed rotation, off by default. A persistent shuffled bag avoids adjacent repeats; automatic changes wait for idle foreground Home and pause for menus, apps, other wallpaper sources and battery saving. No background wakeup is scheduled.
- Keep earlier rounded-square, scene-lighting and physical-folder defaults, saved choices and Gravity mechanics. Trial 5 remains queued for phone feedback.

# 0.8.9 — 2026-11-16

- Trial 1 of 8: match built-in scene light across icon/folder glass, cards, dock and menu surfaces. Use the current scene photograph/material as the optical reflection map and coordinate restrained colored light sweeps with the scene clock.
- Add Match scene lighting and Scene lighting strength in Look & feel, including local Nota commands. Off/zero restores neutral glass for comparison; settings participate in existing export/import and Undo.
- Use warm sunset/city/lava light, green/violet aurora light, aquatic tones and cool space/ice light. Preserve fading-menu opacity, text, native input and the existing material/shape/transparency controls.
- Freeze ambient light with ambient motion off, subtle lighting, reduced motion or battery saving; tilt follows existing motion controls. Keep photo/custom/Android wallpapers neutral in this first built-in-scene trial.
- Retain bounded shared reflection maps, 24 cached icon rasters and cached gradient transforms. Only foreground frames drive the light clock; detached surfaces are weakly observed.
- Add pure light-model/command tests and a native material, comparison, opacity and settings-round-trip check. Folder animations, presets, 3D widgets, scene-editor expansion, unified search, haptics and automatic quality remain queued for separate user-reviewed builds.

# 0.8.8 — 2026-11-16

- Rework Spatial Desktop as a photographic dusk mountain lake with independent near-shore parallax, subtle reflected water movement and a live refractive glass orb. Remove its original grid, procedural ridgelines and low-polygon crystal; keep the original Gravity background/mesh.
- Replace app/folder icon surfaces with cached optical glass shells: continuous rounded bevels, photographic environment reflections, shaped extrusion, tint and a shared glass cover over actual icon artwork. Apply all three shapes consistently on Home, in menus/drawers and in settings/desktop-management previews.
- Add subtle tilt-driven cover highlights that honor reduced motion, power saving and the lighting preference. Keep Gravity's drag/fling, forces, collision response, pinning, obstacles, save behavior and tile dimensions unchanged.
- Center one/two-item folder contents inside a quieter inset pocket. Preserve actual installed app artwork, folder contents and saved shape/material/transparency choices.
- Add material raster tests and extend scene checks to Spatial motion/freeze/tilt and round trips through its texture bank.

# 0.8.7 — 2026-11-16

- Replace City in Focus's procedural frame with photographic glasses and matching transparent lens masks; reduce city-background blur while keeping the same sharp city registered inside the moving lenses.
- Rebuild Aurora Veil with photographic alpine mountains and independently moving northern lights over stars. Keep the opaque terrain stationary during ambient sky motion.
- Render Holographic Depth as a rotating textured planet with directional sunlight, an ocean highlight and a thin atmosphere against a photographic starfield.
- Append Asteroid Field with five irregular, crater-textured rocks, independent drift/rotation and depth-sensitive parallax. Preserve all existing scene IDs and backup selections.
- Share four material slots between scene families within GLES2's eight-sampler budget. Restore the original aquatic/glass/ice/lava textures when returning to those scenes, including after context recreation.
- Refresh gallery thumbnails from the shipped shader. Extend native realism checks to all changed scenes and aquatic transitions; extend local GLES checks to actual landscape pixels, exact texture-bank round trips and independently animated aurora.

# 0.7.8

- Animate caustic light across pond stones and reef rocks, plus drifting jellyfish-scene light shafts and surface glints. Add Aquatic light intensity while retaining motion/pause controls.

- Add a three-step layered scene creator: gallery background, optional crystal/orb/star or transparent image objects, independent parallax depth and preview.
- Keep drafts separate from applied wallpapers and embed applied scenes/assets in desktop backups.
- Open Android wallpaper apps through a chooser; list installed live services with explicit component previews and system fallback. Forward desktop offsets and taps to live wallpapers.
- Replace right/bottom crystal outlines with rounded shaded shoulders across solid cards and translucent icon/material surfaces.

## 0.7.7 — 2026-11-15

- Replace settings-only export with a self-contained desktop backup containing folders and children, shortcut identities/order/positions, desktop names/Home/page order, settings, notes, a bounded photo wallpaper and widget placement/provider records. Verify saved bytes by reading them back. Validate all records before showing a restore confirmation; keep a one-step Undo and reject malformed/corrupt data before replacing contents.
- Keep legacy .nth3dsettings imports with an explicit settings-only warning. Old files cannot recover folders they never contained. After reinstalling, widget placements are queued for Android binding/configuration approval rather than trusting stale widget IDs; same-install valid bindings can be reused.
- Add Wallpaper settings: app-owned photo selection, Fill/Fit, dimming and gentle parallax, plus Android wallpaper and live-wallpaper pickers. Photo bytes travel in the backup. Hide the GL scene when a photo or Android wallpaper is active, and switch back to live scenery when choosing a scene.
- Refresh the Discover registration after launcher/provider updates and when Repair is selected; acknowledge successful update handshakes and make one bounded fresh connection after a dead binding. Keep paused retry behavior and all desktop data. Add a stale-registration cross-process fixture.
- Add focused native data-loss and wallpaper checks, including a >64 KiB folder backup, simulated reinstall, preserved identities/contents/photos/notes, invalid-input rejection, restore Cancel/Undo and widget reapproval metadata.

## 0.7.6 — 2026-11-15

- Rebuild Living Pond and Biolume Reef with generated photographic environments and detailed transparent animal textures. Koi follow smooth paths with body/tail strokes and fin motion; jellyfish swim independently with bell pulses and trailing-arm motion. Both retain tilt, ambient pause, power saving and reduced-motion support. Pond count, speed and touch ripples remain functional.
- Use 3D Stack for desktop, menu and folder effects on fresh initialization. Keep stored effect choices and provide a one-tap Use 3D Stack everywhere preset with one-step Undo.
- Place desktop, menu and folder effect selectors first in Look & feel, outside Advanced. Keep contextual routes from folders, scenes and desktop settings. The desktop Stack effect now includes perspective tilt, preserving its stored effect ID.
- Load and recycle bounded texture bitmaps once per GL context; use clamp/linear filtering and straight-alpha animal sampling. Scene thumbnails and settings previews show the same photographic content.
- Add native checks for fresh defaults, stored preferences, atomic preset/Undo, texture decoding/alpha, directly accessible effect choices and live scene screenshots. Extend GLES checks to pond pause/tilt, fish count and fish speed.

## 0.7.5 — 2026-11-15

- Replace desktop preview rims, drawer icon plinths, category tabs, Crystal cards and scene choices with an opaque polished crystal material: thick bevels, silver-blue reflections and subtle prism edges.
- Remove the highlighted drawer/footer text, About drawer tile, standalone folder Rename action, desktop-management instructions and redundant Scene studio buttons. Rename remains in the folder hold menu; About remains in settings.
- Selecting a scene applies it and returns to the desktop immediately.
- Add Crystal Atrium, Aurora Veil and Biolume Reef with native shader rendering, accelerometer depth, ambient movement and reduced-motion support. Append stable scene IDs and update persistence, settings and exact shader previews.
- Add a focused native crystal/scene suite independent of the settings/rotation suite, and shader checks for motion, pause and tilt.

## 0.7.3

- Independent folder opening effects and shared-pose animated menu/folder previews, replayed on selection or tapping the preview.
- Expanded Home/desktop gesture assignments, including two-finger movement/rotation, double-tap swipes, app and saved shortcut targets, and optional Android system actions.
- Swipe up over a folder opens the assigned action; top-of-drawer pull-down closes it. Desktop app/folder drags now use the same continuous native edge-paging path as drawer drags.
- Nth Nota AI has a scrollable examples menu that inserts commands, clearer vague-request choices, a smaller italic placeholder, and no duplicate shortcut/search field on its page. Preview height recovers after keyboard dismissal.
- Restore motion listeners and rendering on desktop/focus return. Widget Padding now expands content when selected; existing visual padding choices migrate without changing their appearance.
- Optional setup guide with default Home, media/system gesture access, App info recovery, calendar and approximate weather location. Returning from Android does not advance a step. Weather coordinates are used only after selecting Use my location.

# Changelog

## 0.7.2 — 2026-11-14

- Add a 3D desktop manager from an empty-space hold, an empty-space pinch or Home screen settings. Swipe miniatures, tap to open, mark Home with the house, add screens, drag screens to reorder, use accessible move arrows, and remove empty screens. Preserve shortcuts, folder contents, widget assignments, pattern flags and sorting Undo when indexes change. Home and Back return to the selected Home screen.
- Accept Google overlay progress during an active request before readiness arrives. Keep a visible feed steady through transient status changes, stop startup recovery on visibility/readiness, and retain the connection when display confirmation is missing. Offer Pause, Keep feed open, Retry and Close; real startup failures still use bounded recovery and retain a connection report. Use the completed overlay swipe protocol alongside the explicit open request.
- Add Move to folder immediately below Add to folder for apps held inside a folder. Transfer the original shortcut, check capacity before removing it, and merge an already-present app without duplicates. Show the destination folder afterward.
- Remove ellipsis punctuation from launcher menus. Show full, wrapping folder and drawer names and menu titles; retain compact eight-character desktop icon labels. Wrap labels in settings and desktop miniatures.
- Exercise native screen drag/reordering, Home selection, add/remove, pinch entry, metadata preservation, folder moves/capacity/deduplication, full labels, early Google progress, transient readiness, paused display confirmation and manual Retry, alongside prior regression.

## 0.7.1 — 2026-11-14

- Remove references to another launcher from current app text, accessibility descriptions and project documentation. Keep the category sorting feature under Nth Desktop wording.
- Keep drawer drags attached to the activity root when removing the drawer. Protect the initial hold from parent interception and restrict downward dismissal to empty drawer space, preserving scrolling from app icons.
- Retain the Google-enabled build and activity token. If the service sends no readiness response, refresh the window registration after four seconds, then make one fresh connection with the legacy attachment at eight seconds. Preserve modern lifecycle negotiation, ignore superseded callbacks, keep a strong current callback reference, and retain the original 15-second startup limit. Explicit permission/content errors and cancellation do not trigger recovery. The owner's 0.7.0 recording shows Google 17.74.18 connects silently; this is a recovery attempt, not proof of Google acceptance.
- Add Crystal app/folder icon shape controls and local Nth Nota navigation for shape, size and transition requests without a value. Understand large/smaller phrasing and preserve folder/drawer setting scope. Offer explicit desktop add and empty-only remove actions; keep at least one screen and shift widget/pattern page assignments safely.
- Show labelled sample icons in settings when the current desktop is empty or its icons are below the preview. Keep transition previews and their shared transforms.
- Extend native regression to the recorded downward drawer gesture, empty previews, shape/Nota controls, page removal preservation, silent window recovery and delayed superseded Google callbacks.

## 0.7.0 — 2026-11-14

- Promote the owner-phone-verified Google connector to the default provider. Ship the Google-enabled build under the normal app name with the existing R2 key; this build deliberately enables app debugging, as the successful 0.6.2 Discover Test did. Preserve feed-off and Google-app choices.
- Make widget resize borders and corners easy to grab, keep the last valid snapped rectangle when another widget blocks a resize, and add a persistent Padding toggle to the widget hold menu. Report the actual padded or unpadded content size to providers through public APIs.
- Add optional Auto pick and sort with a layout preview, twenty reference folder names, local Android category/name/package hints and four-icon folder previews. Add the staggered arrangement on a new desktop, keep existing screens and offer Undo.
- Drag apps and folders directly from the drawer after holding; a stationary hold retains the menu. Hold at an edge to change desktops or create a new one, with a 24-screen limit and cancel rollback. Keep folder identity and children when moving it.
- Add system, sensor, portrait and landscape Home rotation choices. Preserve layout through orientation changes.
- Expand desktop transitions to Cube, Carousel, Fold, Slide, Card stack, Tablet, Revolving door, Flip, Flip vertical, In and out, Accordion, Glass, Wipe, Zoom fade, Throw and None. Drive real pages and a clipped, replayable preview from shared transforms; honor Reduced motion.

## 0.6.2 — 2026-11-13

- Preserve the activity token in the launcher window attributes sent to Google. Do not replace it with the view-window token, which belongs to Android subwindows.
- Report the window contract and label attachment as a submitted request rather than a remote acknowledgement. Diagnose a missing activity token separately.
- Strengthen the separate-UID overlay fixture to expose API 11 and create a normal application window using the activity token; a panel window previously accepted the wrong token. Retain legacy and failure-path coverage.
- Keep the existing R2 signing identity, one-app normal/Discover Test variants and saved launcher data. Actual Google Discover readiness still needs the owner-phone check.

## 0.6.1 — 2026-11-13

- Negotiate the Google overlay service API, use legacy window/lifecycle methods only for older APIs, and send modern activity state once for current services.
- Allow 15 seconds for cold startup and 8 seconds for display after readiness. Add a cancelable, nonmodal connection indicator and guard canceled/stale callbacks.
- Preserve distinct missing-app, disabled-service, permission, null-binding, readiness, display and remote-error outcomes. Add local copyable reports with app/build versions, negotiated API and bounded connection events.
- Add an explicitly labelled Discover Test build (debuggable), with the same package/version code and owner signing identity as the normal release. The release stays non-debuggable. Actual Google acceptance must be checked on the owner's Pixel; a protocol fixture is not a production Google test.
- Exercise a delayed service beyond the former 2.5-second limit, strict modern and legacy protocols, null binding, silent/not-ready/display timeouts, report copying and cancel behavior. Verify release/test/release in-place installation preserves desktop layout and notes.

## 0.6.0 — 2026-11-13

- Build Nth Feed into the main APK: RSS/Atom headlines, publisher attribution, local interests, unread/recent ordering, search, saved headlines, hide/restore, custom HTTPS sources and an offline cache.
- Add a publisher WebView reader with visible origin, feed return, reload and optional browser opening. Saved headlines do not claim to be downloaded full articles.
- Use crystal news cards with subtle tilt, and retain the existing crystal tools. Switch the former default Google provider to Nth Feed once; preserve feed-off and explicit Google-app choices.
- Expose provider selection in settings. Keep direct Google overlay support experimental with an explicit fallback; stop depending on or shipping a companion APK. The main release remains non-debuggable and uses the same R2 key.
- Test RSS/Atom parsing, bounds, hostile XML, native refresh, partial errors, offline cache, saved stories, source toggles, interests, search and the internal reader alongside the launcher regression.

## 0.5.0 — 2026-11-13

- Clip settings scrolling to its viewport and fade rows at the preview boundary.
- Default new fading-menu settings to enabled, 90 percent opacity over content and an 80 dp fade width; preserve explicitly saved values.
- Keep every icon label on one line with eight Unicode code points followed by dots for longer names. Retain full identities for search, launch, rename and accessibility.
- Add configurable crystal cards for media controls, Nth Nota, weather, synced calendars, chosen photos, reading links, notes and installed app links. Expose cards through app search and the Nth Feed page.
- Connect the first-page right swipe to the Google overlay protocol, including a separately signed optional Nth Discover Bridge, live connection status, reconnection and setup fallback. Feed enablement takes priority over infinite paging at the left boundary.
- Exercise card documents, fade/alignment pixels and a real separate-UID overlay fixture in the native regression. Keep the R2 signing identity and increase the version code to 10081.

## 0.4.0 — 2026-11-13

- Replace the long settings list and duplicate Crystal sections with searchable categories and a live, draggable 3D preview. Preserve the page and values across recreation and rotation.
- Render launcher menu material only over the visible desktop icons, folders and widgets beneath it, with animated feathering into transparent empty space. Provide opacity, fade width and empty-area controls plus a fixed-glass fallback.
- Add real icon size/label/color, folder/drawer grid, dock visibility/appearance, layout lock, new-app placement, infinite paging and empty-space gesture settings. Keep widgets and shortcuts in the existing occupancy grid.
- Integrate Nth Nota with bounded offline setting commands, settings navigation, app opening, exact grid-layout Undo and an optional consent-based connection to the existing Nth Nota cloud gateway. Cloud prose never executes settings.
- Add versioned settings-only export/restore using Android document contracts; validate imports before applying and exclude notes, desktop data, widget bindings, signing keys and cloud consent.
- Keep the R2 signing identity; versionCode increases to 10080 for an in-place update.

## 0.3.2 — 2026-11-13

- Replace the system widget list with a searchable gallery grouped by app, with provider preview layouts/images and the actual desktop grid size before selection. Browse/cancel without allocating a widget ID. Render preview rows on demand with a bounded cache.
- Receive Android pin-shortcut requests (including browser webpage shortcuts), ask before adding, and retain package/shortcut/profile identity, chosen name and icon across restarts. Open the shortcut target through LauncherApps rather than opening the app homepage.
- Preserve shortcut identities when copied into folders; support rename, move, removal, duplicate requests and unavailable profiles. Add an explicit Add apps desktop action.
- Extend separate-UID native tests for the preview gallery, selection/cancellation, browser-style requests, persistence, target launch, folder copies and ordinary app shortcuts.
- Increase versionCode to 10079. With owner authorization, replace the lost 0.3.0 signing key with the R2 key and supply a private phone recovery backup alongside the APK. This transition requires one reinstall; future updates reuse R2.

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
