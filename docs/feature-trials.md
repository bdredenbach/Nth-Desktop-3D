# Nth Desktop 3D feature trials

The owner approved trying all eight ideas one at a time. Each feature gets its own installable APK and a user testing/revision round before starting the next. Keep changes easy to compare and retain the previous APK for rollback/reference. Do not start the next trial automatically before the owner responds to the current one.

| Trial | Feature | Status |
|---|---|---|
| 1 | Scene-matched lighting | 0.8.9 implementation; awaiting owner phone review |
| 2 | Physical folder opening/closing | Queued |
| 3 | Complete saved desktop presets | Queued |
| 4 | Placeable 3D versions of Crystal Cards | Queued |
| 5 | Scene creator motion/lighting and individual scene packs | Queued |
| 6 | Unified app/folder/settings/note/Nota search | Queued |
| 7 | Adjustable physical haptic feedback | Queued |
| 8 | Automatic graphics quality | Queued |

## Trial 1: compare before advancing

Install 0.8.9 over 0.8.8 using the existing R2 certificate. Open Settings → Look & feel → Match scene lighting. The default is on, strength 65. Off or strength zero restores the neutral 0.8.8 materials. Adjust strength to taste; the existing material, transparency and icon/folder shape controls remain available.

Try Spatial Desktop, Aurora Veil and Parallax Lava, followed by a space or water scene. Inspect app and folder shells, an open folder, menus/cards and the optional dock. Aurora highlights should move gently with ambient animation; ambient off, reduced motion and battery saving keep the temporal sweep still. Tilt uses the current controls. Check that labels stay clear and fading-menu empty areas remain transparent.

This first trial matches the eleven built-in 3D scenes. Photo, custom layered and Android wallpapers retain neutral lighting. Scene matching uses bounded reflection maps and a shared scene-clock light model; it does not read back the full GPU framebuffer each frame.

Keep or revise this trial after the owner's feedback, then begin Trial 2. The owner especially likes Gravity Playground's current mechanics: keep its drag, movement, collisions, pinning and layout behavior intact.
