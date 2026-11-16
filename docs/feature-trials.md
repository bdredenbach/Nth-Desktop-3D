# Nth Desktop 3D feature trials

The owner approved trying all eight ideas one at a time. Each feature gets its own installable APK and a user testing/revision round before starting the next. Keep changes easy to compare and retain the previous APK for rollback/reference. Do not start the next trial automatically before the owner responds to the current one.

| Trial | Feature | Status |
|---|---|---|
| 1 | Scene-matched lighting | Accepted by owner; 0.9.0 fresh defaults use strength 100 and rounded squares |
| 2 | Physical folder opening/closing | 0.9.0 implementation; awaiting owner phone review |
| 3 | Complete saved desktop presets | Queued |
| 4 | Placeable 3D versions of Crystal Cards | Queued |
| 5 | Scene creator motion/lighting and individual scene packs | Queued |
| 6 | Unified app/folder/settings/note/Nota search | Queued |
| 7 | Adjustable physical haptic feedback | Queued |
| 8 | Automatic graphics quality | Queued |

## Trial 1: accepted

The owner accepted the 0.8.9 phone result. Fresh initialization in 0.9.0 uses rounded-square app and folder shells, scene matching on and strength 100. Saved settings and the previous implicit appearance of existing installs are preserved. Both icon shape controls now live in Look & feel. Off or strength zero restores the neutral 0.8.8 materials.

Try Spatial Desktop, Aurora Veil and Parallax Lava, followed by a space or water scene. Inspect app and folder shells, an open folder, menus/cards and the optional dock. Aurora highlights should move gently with ambient animation; ambient off, reduced motion and battery saving keep the temporal sweep still. Tilt uses the current controls. Check that labels stay clear and fading-menu empty areas remain transparent.

This first trial matches the eleven built-in 3D scenes. Photo, custom layered and Android wallpapers retain neutral lighting. Scene matching uses bounded reflection maps and a shared scene-clock light model; it does not read back the full GPU framebuffer each frame.

## Trial 2: compare before advancing

Install 0.9.0 over the previous build. Under Look & feel → Folder opening animation choose **Physical folder**. Existing folder animation choices stay selected until changed. Tap the preview to replay the shared hinge and stagger phases. Real folders grow from the selected shell, turn a glass front down and fan their existing native controls into place. Back, outside tap and the close button reverse into the source shell. App launches, other panels and lifecycle changes cancel the transition safely.

Try a desktop folder, a drawer folder and a dock folder; check tapping apps, holding apps, scrolling a long folder and closing it. Press Back quickly during opening, then open another panel. Try reduced motion and Android animation scale off. Verify portrait/landscape, labels and the scene lighting at strength 100. The dedicated folder interaction suite passed on the GitHub Android 11 emulator. Broader gesture/setup/rotation suites still have failures; this is not a clean full regression pass. Phone appearance and performance remain unverified.

Wait for owner feedback before Trial 3. The owner especially likes Gravity Playground's current mechanics: keep its drag, movement, collisions, pinning and layout behavior intact.
