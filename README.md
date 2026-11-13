# Nth Desktop 3D

An Android Home launcher with tilt parallax, perspective desktop transitions,
animated Start menus and four original GPU scenes. Current test release: **0.3.0**.

Install the supplied signed release APK for continued updates. Version 0.3.0
establishes a permanent test certificate; moving from older debug builds requires
a one-time reinstall. See [signed APK updates](docs/SIGNED-APK-UPDATES.md) for
the private key recovery process, build command and versioning rules.

## Included

| Scene | Behavior |
| --- | --- |
| Spatial Desktop | Layered mountain scenery, a lit 3D crystal, raised shortcut planes |
| Gravity Playground | Bounded shortcut physics, tilt gravity, dragging/throwing, pinning |
| Holographic Depth | A shaded planet and star layers with different parallax depths |
| Living Pond | Procedural pebbles, swimming koi, floating leaves, shadows, touch ripples |

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
stop when the Activity pauses. No background service or network permission is used.

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

This is the first working foundation. External Android widget hosting, additional
Nth utility apps, freely resizable multiple built-in windows, layered-photo imports,
and `.nthdesktop` compatibility have not been implemented. They require separate
integration and regression testing. A room/shelf layout and richer geometry are
future scene options. Physical Pixel 10 heat, battery, sensor feel and frame times
must be measured on-device; emulator tests do not establish those results.

Reference recordings inspired behavior only; their photos, characters, video clips,
brand assets and artwork are not bundled. Visuals and app icon are original code/vector
assets. The design reference mapping is in `docs/design-brief.txt`.
