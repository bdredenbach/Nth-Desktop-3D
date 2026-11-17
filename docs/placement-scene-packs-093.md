# Card placement and scene editor — version 0.9.3, Trial 5

## Discover recovery and drawer direction

The owner's 227346.png shows API 11 bound and attached, but no readiness callback through registration refresh and legacy reattachment. Final recovery now clears activity state and changes a client_session URI parameter before rebinding, so Android requests a fresh binding identity rather than reusing the same silent binding. Manual repair also creates a fresh session. Package/UID authority and v7/cv9 remain intact. The 15-second startup deadline and explicit-status/pause/cancel/visible safeguards remain. This cannot force-stop Google or guarantee recovery from a hung provider process; Pixel 10 / Google 17.74 needs phone confirmation.

App drawer settings now offer Vertical or Horizontal. Vertical remains the default. Horizontal uses whole viewport rows, swipe pages, a page label and accessible forward/back actions. Filtering resets to the first page; each rendered icon retains its full-catalog tap/menu/drag target. Rotation or keyboard viewport changes recompute capacity.

## Cards and app tiles

The owner's 227316.mp4 demonstrates a desktop card rejecting movement as occupied, and the drawer's Cards panel offering button-based placement. Cards now use the same continuous Android drag controller as drawer app shortcuts. Hold, then move to begin dragging; dwell at the left/right screen edge to change desktop, including creating a new desktop at the right edge. A stationary hold opens actions. Native child buttons and scrolling retain their normal behavior before a hold. The same drag works on cards opened through the app drawer's Cards action. Accessibility long-press opens the action menu.

Card shadows use the card's actual desktop spans, with the grab position preserved. Preview/drop plans reserve the whole rectangle and never place a card inside a folder. A card searches the current visible desktop grid for a genuinely free rectangle. If none fits, placement creates a new desktop without relocating the existing shortcuts. This also handles fragmented space that has enough total empty cells but no suitable rectangle. Cards are bounded to a usable viewport on the destination. At the twenty-four-desktop limit, failed placement retains the original contents. Canceled edge drags restore the original page and remove unused desktops created during that drag. Automatic scene shuffle pauses during a system drag.

App icon shape now offers **None**, appended after the existing three choices. It shows the app's own artwork without the launcher glass shell, cover or shell shadow; folder shapes and saved choices remain independent. Drawer, dock, settings and desktop-management previews use the same choice. Fresh initialization still uses rounded squares.

Hold a desktop app and choose **Resize** for a resizable app tile. Drag the full-edge grips; the enlarged tile remains a launchable app icon and label. The drawer app menu also offers Resize, placing a two-cell tile on a desktop before opening the grips. **Use normal shortcut** reverses the mode. These are resizable launch surfaces, not generated third-party live-data providers. Ordinary Gravity shortcuts retain their existing mechanics; explicit app tiles reserve rectangular space like cards/widgets. Copying or moving an app into a folder/dock returns it to a normal shortcut there.

Card and app-tile spans are canonical layout data retained by presets/backups. The backup writer is version 4 and accepts older versions 2 and 3. Older launchers should not restore a version 4 document. Existing app/folder/icon IDs and scene IDs are unchanged.

## Scene creator and separate scene packs

Each object offers **Still**, **Float**, **Orbit** or **Drift**, with independent amount and speed. Arrange objects while still in Objects; their animation plays in Depth & preview and the applied scene. Scene parallax remains independent. Animate scene objects can pause all object paths. Global ambient-off, reduced motion, background state and battery saving stop ambient paths and animated light effects. Paths are bounded and do not accumulate drift over a long session.

Depth & preview adds scene light strength, color and direction. The light shades object artwork while preserving cutout alpha and also tints caustics and shafts. Light strength zero preserves the earlier appearance. Existing scenes open with Still objects and zero added light. Custom-scene launcher glass keeps its existing neutral lighting policy.

Use **Export scene pack** in the creator to share the current draft, or Export my scene from the scene library/wallpaper settings for the applied scene. Android's document picker creates a `.nth3dscene` file. It contains one scene and its copied image bytes, layer geometry, motion and lighting. It contains no desktop layouts, notes, permissions or app state. Export uses an immutable private snapshot that survives Activity recreation; the saved bytes are read back and verified.

**Import scene pack** validates the complete file and image checksums before showing a review dialog. Cancel retains both draft and applied scene. Import replaces only the draft and opens it for preview; the active scene changes after **Apply scene**. Scene editor schema 2 still reads schema 1 scenes. No external image URLs, scripts or downloaded executable content are accepted.

## Verification

Local Google/release builds and lint pass (zero errors, 86 warnings per variant), all 113 JVM tests pass and the Android test APK compiles. Focused Android results are recorded here before delivery. Dedicated Placement checks exercise native drawer dragging, edge dwell/cancel, overflow on fragmented screens, capacity limits, app resize/tap/revert and None. Dedicated Packs checks exercise renderer pixels/freeze, scene schema/asset validation, native editing and Android document export/import/cancel. Phone appearance, real app behavior and performance remain for owner review. Trial 6 stays queued.
