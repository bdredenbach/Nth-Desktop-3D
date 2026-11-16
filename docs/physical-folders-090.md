# Physical folder trial — 0.9.0

## Initialization and settings

| Setting | Fresh initialization | Saved selection on upgrade |
|---|---|---|
| App icon shape | Rounded square (ID 0) | Preserved |
| Folder icon shape | Rounded square (ID 1) | Preserved |
| Match scene lighting | On | Preserved |
| Scene lighting strength | 100 | Preserved |

Folder icon shape is now in Look & feel beside app icon shape, including search and contextual settings navigation. Folder columns, content icon size, labels, title and radius remain in Folders. Enum IDs are unchanged. `VisualDefaults` only fills absent preferences once. Existing seeded/restored installs with absent folder-shape or lighting-strength keys retain their former implicit circle/65 appearance; explicit off/zero selections are retained.

## Animation

Choose Look & feel → Folder opening animation → Physical folder. It is appended as ID 7; previous effects keep IDs 0–6 and the existing Stack default/selection. Menu animations remain independent.

`FolderMotion` snapshots only the selected shell, up to 256 pixels per edge, and temporarily hides that source tile. The existing native folder panel expands from its position while the front turns around a bottom hinge and visible app tiles fan out in a short stagger. The shared `FolderPose` and `FolderLid` also drive the slower settings preview. Opening lasts 560 ms and closing up to 280 ms; an interrupted opening reverses from its current progress. At most 24 visible native tiles are animated. No canonical items, folder order, geometry, shortcuts or physics values are edited.

Desktop, drawer, dock and nested folder tiles carry stable item tags for locating the shell. Missing/offscreen sources use a bounded central fallback. Closing retains the last known source position if the source panel was removed, as when opening from the drawer. A source snapshot has no listeners, and its drawing view cannot receive input or accessibility focus. Existing native listeners handle taps and holds. Completion restores identity transforms. Cancellation restores the original source alpha, removes the drawing view and invalidates old completion callbacks before replacing a panel. Back before first layout cannot restart opening.

Back, outside tap and the close button request reverse close. App launching and other panel replacements retain immediate navigation. Pause, destroy, reduced motion and power saving dispose temporary motion. Android animation scale off skips it. The new trial is opt-in so existing selected effects remain available for comparison. Gravity Playground retains its existing physics and modal pause behavior.

## Validation scope

Local validation: Google/release APK builds, Google/release lint (zero errors, 83 warnings per variant), compiled native test APK, and 90 passing JUnit tests with zero failures/errors/skips. The ten new cases cover defaults/upgrade preservation and bounded folder phases. No shader or photographic assets changed in this trial; the unchanged shaders were covered by the preceding eleven-scene GLES check.

The dedicated `folders` Android smoke suite checks fresh/legacy defaults, settings placement, independent effects, real controls, reverse close, Back before layout and during motion, replacement panels, source visibility, unchanged canonical layout and reduced motion. It passed on the GitHub Android 11 emulator for feature commit d612d0b655056b10506947b31c626f94f7047ae6 (run 43918924329). Native screenshot capture was then added to that fixture. Older interaction tests were updated to recognize the selected default shape and expand Advanced before checking its controls. There is no accelerated local emulator. Phone review remains required for animation appearance, speed, rotation and touch feel.

Trial 3 stays queued until the owner accepts or revises this build.
