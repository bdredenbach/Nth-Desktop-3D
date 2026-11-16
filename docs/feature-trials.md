# Nth Desktop 3D feature trials

The owner approved trying all eight ideas one at a time. Each feature gets its own installable APK and a user testing/revision round before starting the next. Keep changes easy to compare and retain the previous APK for rollback/reference. Do not start the next trial automatically before the owner responds to the current one.

| Trial | Feature | Status |
|---|---|---|
| 1 | Scene-matched lighting | Accepted by owner; 0.9.0 fresh defaults use strength 100 and rounded squares |
| 2 | Physical folder opening/closing | Accepted by owner; default for fresh initialization in 0.9.1 |
| 3 | Complete saved desktop presets | 0.9.1 implementation; awaiting owner phone review |
| 4 | Placeable 3D versions of Crystal Cards | 0.9.2 implementation with requested scene randomizer; awaiting phone review |
| 5 | Scene creator motion/lighting and individual scene packs | Queued |
| 6 | Unified app/folder/settings/note/Nota search | Queued |
| 7 | Adjustable physical haptic feedback | Queued |
| 8 | Automatic graphics quality | Queued |

## Trial 1: accepted

The owner accepted the 0.8.9 phone result. Fresh initialization in 0.9.0 uses rounded-square app and folder shells, scene matching on and strength 100. Saved settings and the previous implicit appearance of existing installs are preserved. Both icon shape controls now live in Look & feel. Off or strength zero restores the neutral 0.8.8 materials.

Try Spatial Desktop, Aurora Veil and Parallax Lava, followed by a space or water scene. Inspect app and folder shells, an open folder, menus/cards and the optional dock. Aurora highlights should move gently with ambient animation; ambient off, reduced motion and battery saving keep the temporal sweep still. Tilt uses the current controls. Check that labels stay clear and fading-menu empty areas remain transparent.

This first trial matches the eleven built-in 3D scenes. Photo, custom layered and Android wallpapers retain neutral lighting. Scene matching uses bounded reflection maps and a shared scene-clock light model; it does not read back the full GPU framebuffer each frame.

## Trial 2: accepted

Install 0.9.0 over the previous build. Under Look & feel → Folder opening animation choose **Physical folder**. Existing folder animation choices stay selected until changed. Tap the preview to replay the shared hinge and stagger phases. Real folders grow from the selected shell, turn a glass front down and fan their existing native controls into place. Back, outside tap and the close button reverse into the source shell. App launches, other panels and lifecycle changes cancel the transition safely.

Try a desktop folder, a drawer folder and a dock folder; check tapping apps, holding apps, scrolling a long folder and closing it. Press Back quickly during opening, then open another panel. Try reduced motion and Android animation scale off. Verify portrait/landscape, labels and the scene lighting at strength 100. The dedicated folder interaction suite passed on the GitHub Android 11 emulator. Broader gesture/setup/rotation suites still have failures; this is not a clean full regression pass. Phone appearance and performance remain unverified.

The owner accepted the folder animation and requested it as the initialization default. Version 0.9.1 uses Physical folder on fresh initialization and retains explicit choices and older implicit choices on upgrade. The shared Use 3D Stack everywhere button is removed. Look & feel is the first settings category.

## Trial 3: available for review

Use Settings → Desktop presets or Look & feel → Desktop presets. Save a setup, change its scene/layout/dock, then save another. Apply either preset after reviewing its summary. Check Cancel and Undo last preset change. Presets include layouts, folders, gestures, visual settings, dock contents, photo wallpaper, the applied custom scene and widget placements. Notes/card content and Android access stay shared. Check Update from current desktop, Rename, Delete, and returning after restarting the app. Widget and pinned-shortcut references are retained by saved presets and the durable previous-desktop snapshot.

The lighting revision maps the supplied screenshots to Parallax Lava (orange), Living Pond (turquoise), Crystal Atrium (light blue), Aurora Veil (green) and Asteroid Field (dark neutral crystal). Scene matching off/zero remains the neutral comparison. Phone appearance/performance requires review.

The owner requested the next queued item with a scene randomizer. Trial 4 is implemented in 0.9.2; presets remain available for phone review. The owner especially likes Gravity Playground's current mechanics: keep its drag, movement, collisions, pinning and layout behavior intact.


## Trial 4: compare before advancing

See [desktop cards and scene shuffle](cards-shuffle-092.md). Place Notes or Media on a desktop, edit/use its normal actions, hold and drag, resize from its edge grips, move to another desktop and remove it. Save a preset with cards and restore it. Try manual shuffle, restrict the selected scenes and enable an interval if desired. Automatic rotation begins off and waits for idle Home. Review phone appearance, touch behavior and performance before starting Trial 5.
