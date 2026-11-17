## 0.9.3 card placement and scene editor trial

Settings → App drawer → App drawer scrolling adds Vertical or Horizontal pages, with search, held drags and accessible page navigation. Vertical remains the default. Discover repair also refreshes a silent binding with a fresh connection session within the existing 15-second startup limit; real Google recovery still needs phone confirmation.

Hold and drag cards from the drawer or desktop. Dwell at a screen edge to change desktops; crowded screens overflow new card placement onto a new desktop. Cancel restores the source and removes unused drag-created screens. App icon shape adds None, and app hold menus add Resize for launchable app tiles with a normal-shortcut reset.

Scene creator objects gain Still, Float, Orbit and Drift, independent speed/amount, and adjustable light color/strength/direction. Import/export `.nth3dscene` packs separately from desktop backups. Imports open as drafts and require Apply to replace the active scene. See [placement and scene packs](docs/placement-scene-packs-093.md). Trial 6 remains queued for phone review.

## 0.9.2 desktop card and scene shuffle trial

Place movable, resizable Crystal Cards through Settings → Cards → Place card on desktop or Widgets → Add Crystal Card. Cards retain their real Notes, media, weather, calendar, photos, reading and app actions, and their geometry is saved in desktop presets/backups. Hold to move or open the resize/removal menu.

Settings → Scene randomizer adds selected-scene shuffle and optional timed rotation. Automatic shuffle starts off; enabling it rotates on idle Home and pauses for menus, apps, other wallpaper sources and battery saving. Shuffle now is also in Scene studio. See [cards and scene shuffle](docs/cards-shuffle-092.md) for behavior and validation. Trial 5 remains queued for phone feedback.

## 0.9.1 desktop preset trial

Save and restore named desktop setups through Settings → Desktop presets or the shortcut in Look & feel. Presets include scenes, appearance, layouts, folders, dock, gestures, wallpaper and widget placements, with persistent Undo. Notes/card content stay shared, and saved widget/shortcut references are preserved. See [desktop preset trial](docs/desktop-presets-091.md) for scope, limits and validation.

Look & feel is first in settings, Physical folder is the new initialization default, and the shared Stack reset is removed. Lava, Pond, Atrium, Aurora and Asteroid lighting follows the requested orange, turquoise, light blue, green and dark neutral palettes. Saved choices stay intact.

## 0.9.0 folder trial

Fresh initialization uses rounded-square app and folder shells, scene lighting on and strength 100. Both shape controls live in Look & feel; saved choices survive upgrades. Choose Look & feel → Folder opening animation → Physical folder to try the hinged glass front and staggered native icons, with reverse closing. Existing effects remain available. See [physical folder trial](docs/physical-folders-090.md) for behavior and verification limits and [feature queue](docs/feature-trials.md) for the one-at-a-time plan.

## 0.7.3 interaction update

Folders have their own opening animation under Settings → Look & feel. Menu and folder choices replay in the live preview; tap the preview to replay again. Settings → Gestures includes Home, swipes, double-tap swipes, two-finger movement, pinch and rotation, with launcher actions, installed apps and saved shortcuts. Notification shade, quick settings, recent apps and screen lock use the optional Nth gesture actions accessibility service; it does not retrieve window content or subscribe to events.

Swipe up starting over a desktop folder to use the assigned swipe-up action. Pull down at the top of the app drawer to close it. Hold and drag a desktop folder to an edge to change screens or create a screen on the right. Widget Padding is checked when content is expanded; existing visual settings are preserved during migration.

Nth Nota AI offers a three-dot command examples menu that inserts a request without executing it. Vague requests offer choices. The Nota page hides the redundant settings search and shortcut, and its preview retains its height after the keyboard closes.

Open Settings → Launcher → Setup & permissions to revisit optional access. Returning from Android stays on the current step, with Open App info available for restricted-setting recovery. Weather location remains opt-in; city entry still works without a location grant.

# Nth Desktop 3D

An Android Home launcher with tilt parallax, perspective desktop transitions,
animated Start menus and eleven original GPU scenes. Current test release: **0.9.3**.

Install the supplied signed release APK for continued updates. Version 0.3.2
uses the owner's replacement **R2** signing certificate. Moving from the lost
0.3.0 key or older debug builds requires a one-time reinstall. Keep the private
R2 signing backup on your phone and reattach it for future builds. See
[signed APK updates](docs/SIGNED-APK-UPDATES.md) for recovery and versioning rules.

Widgets now use Android's registered host views, share a placement grid with apps
and folders, and support direct hold/drag plus cell-aligned resizing. See
[widget placement](docs/widget-placement.md) for the interaction and validation scope.

Settings now use searchable categories, a live 3D preview and content-aware fading menu backgrounds. Nth Nota supports offline setting commands with Undo and optional cloud explanations. See [immersive settings](docs/immersive-settings.md) for controls, privacy and the remaining reference-video scope.

Nth Feed is now built into the launcher: publisher RSS/Atom headlines, local interests, saved headlines, offline cache and an internal publisher reader. Swipe right on Desktop 1. No companion APK is required. See [native feed](docs/native-feed.md) for sources and behavior.

The primary APK uses the built-in Google connector and enables app debugging. Google Discover is the default provider, with Nth Feed available in settings. Silent Google startup tries a refreshed window registration and a fresh legacy attachment within the original 15-second allowance. Once ready or visible, automatic recovery stops. Missing display confirmation keeps the connection and offers manual Retry or Keep feed open. The copyable connection report identifies the outcome; the production Google service still needs phone testing. See [Discover troubleshooting](docs/discover-troubleshooting.md).

Hold empty space or pinch empty space to manage desktop miniatures, reorder screens, add/remove empty screens and choose Home. Folder hold menus can move an app directly to another folder. Menu names wrap in full; desktop icons keep compact labels. See [desktop management](docs/desktop-management-072.md).

## Included

| Scene | Behavior |
| --- | --- |
| Spatial Desktop | Layered mountain scenery, a lit 3D crystal, raised shortcut planes |
| Gravity Playground | Bounded shortcut physics, tilt gravity, dragging/throwing, pinning |
| Holographic Depth | A shaded planet and star layers with different parallax depths |
| Living Pond | Photographic garden pond, detailed swimming koi, depth shadows and touch ripples |
| Crystal Atrium | Beveled glass monoliths, moving reflections and a perspective stone floor |
| Aurora Veil | Layered northern lights, mountain silhouettes and a reflected lake |
| Biolume Reef | Photo-textured jellyfish with independent swimming, bell pulses and underwater scenery |

Choose Cube, Carousel or Fold desktop transitions independently of Lift,
Orbit or Unfold menu animations. App discovery and launching use Android's
`LauncherApps` API, with component/profile identities. Shortcuts, folders,
per-scene options and notes persist locally.

Start includes installed apps, search and an Nth tools filter. Hold an app for
Open, Add to desktop/folder, App info or Android's uninstall flow. Desktop
shortcuts also support renaming, removing, moving between desktops and pinning
in Gravity Playground. Removing a shortcut never uninstalls the app.

The first built-in tools are Nth Notes, Calculator, Calendar, Scene studio,
3D settings, and shortcuts to Android device controls. Tap **All shortcuts**
to browse every desktop item without relying on motion or spatial positioning.
Gravity renders up to 18 tiles simultaneously; remaining items remain accessible
through that list. Each desktop supports up to 60 items.

## Visual refresh and stability (0.2.0)

The shell now uses a graphite palette, a lighter clock, original line icons,
individual raised app icons and a single floating dock. Scene studio has previews
rendered from the shipped scene shader. Menus use smaller, measured perspective
rotations and quiet action rows. Crystal normals follow the mesh rotation.

The desktop uses a deterministic measured shortcut plane instead of changing
Android GridLayout column counts after restoring children. Horizontal paging
locks out vertical scroll gestures. Keyboard insets resize panels independently
of the dock, and panel positions respect asymmetric cutouts.

## Use

1. Install the signed test APK and open Nth Desktop 3D.
2. Choose **Set as my Home app**, or explore before selecting a default.
3. Tilt gently for depth. Swipe the desktop or use the page arrows.
4. Open the **cube in the dock** for Start. Hold an app for its menu.
5. Open **Scenes** to change the environment; hold the taskbar for desktop options.

3D settings provides tilt on/off, depth strength, recentering, ambient motion,
reduced motion, menu and transition styles, fish count/speed, ripples and shadows.
Android's disabled-animation setting is respected. Battery saver pauses tilt
and physics and limits scenery rendering to 30 fps. Sensor listeners and rendering
stop when the Activity pauses. Feed and weather requests use network access; optional media and gesture services run only after access is explicitly granted in Android settings.

## Build and verify

Use JDK 17 or 21, Android SDK platform 35 and build tools 35.0.0. The wrapper pins
Gradle 8.11.1 and the project pins Android Gradle Plugin 8.9.2.

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Android 8.0/API 26 or later,
OpenGL ES 2.0. No third-party runtime dependencies. Unit tests cover bounded
physics, wrapped desktops, time-independent smoothing, arithmetic parsing, and
keyboard/cutout panel placement.
See `docs/verification.md` for runtime evidence and remaining device checks.

## Architecture

- `SceneSurface` owns the foreground-only GLES renderer, shader lifecycle and crystal mesh.
- `scene.frag` draws original procedural scenes, layered parallax and pond effects.
- `DesktopView` hosts accessible shortcut views, perspective transitions and physics.
- `MotionController` uses game rotation vector, gravity, or filtered accelerometer,
  remaps display axes and clamps motion relative to a neutral pose.
- `LauncherStore` owns one canonical, version-independent shortcut layout. Switching
  scenes does not replace layout data. Physics positions are additional normalized fields.
- `AppCatalog` discovers launchable components for accessible user profiles.
- `MainActivity` owns Start, animated panels, settings, folder actions and built-in tools.

Scenes combine GPU depth effects and a real lit mesh with perspective-transformed
native UI planes. This is a spatial interface, not VR or an engine that modifies
the contents of other Android apps. Native controls preserve text input, inverse
touch transforms and accessibility. Ordinary Android apps launch normally.

## Next development passes

This is the first working foundation. Additional Nth utility apps,
freely resizable multiple built-in windows, layered-photo imports,
and `.nthdesktop` compatibility have not been implemented. They require separate
integration and regression testing. A room/shelf layout and richer geometry are
future scene options. Physical Pixel 10 heat, battery, sensor feel and frame times
must be measured on-device; emulator tests do not establish those results.

Reference recordings inspired behavior only; their photos, characters, video clips,
brand assets and artwork are not bundled. Visuals and app icon are original code/vector
assets. The design reference mapping is in `docs/design-brief.txt`.

0.5.0 adds a clipped, fading settings list, eight-character one-line icon labels,
crystal cards and optional embedded Discover via Nth Discover Bridge.
See [the feature and integration notes](docs/settings-cards-discover.md).
