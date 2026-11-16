# Desktop presets — Trial 3, version 0.9.1

Look & feel is now the first category in 3D settings. Physical folder is the fresh initialization default. The old Use 3D Stack everywhere button and helper are removed; desktop, menu and folder effects stay independent. Existing explicit selections and older implicit Stack selections are preserved on upgrade.

## Scene palettes

| Supplied screenshot | Scene | Lighting direction |
|---|---|---|
| Lava | Parallax Lava | Orange and molten amber |
| Koi | Living Pond | Turquoise water |
| Blue glass panes | Crystal Atrium | Light blue sky |
| Northern lights | Aurora Veil | Green |
| Asteroids | Asteroid Field | Dark neutral crystal with clear highlights |

Both primary/secondary hues and glass body tones are adjusted for these five scenes. Fading menus retain their exact opacity mask, and labels retain their existing colors. White optical highlights remain visible. Off or strength zero restores neutral glass. Other scene palettes remain unchanged, including the city tones of City in Focus. No shader or photographic asset changes are made.

## User flow

Open Settings → Desktop presets, or the shortcut at the top of Look & feel. Save current desktop asks for a name. Each saved entry shows its scene, desktop/folder/shortcut/widget counts and date. Tap an entry to review an apply summary, then Continue or Cancel. The previous setup is saved before applying; Undo last preset change survives an app restart. An entry's three-dot menu contains Apply, Rename, Update from current desktop and Delete, with confirmation for update/delete. Saved files stay on the device. To make a portable copy, apply the preset and export the current desktop through Backup & restore.

## Snapshot scope

- Included: all desktop pages/names, Home/current page, app/folder identities/order/coordinates, Gravity pin/position state, fitted grid information, dock slots, gesture targets, visual and launcher settings, app-owned photo bytes, applied layered scene and widget provider/profile/geometry/padding records.
- Shared: notes, card content, Android permissions/consent and installed app/profile availability. Switching presets does not restore old copies of personal content. Android wallpaper mode uses the current system wallpaper; another app's wallpaper state is not captured.
- Widget bindings and Android shortcut pins referenced by any saved preset or the Undo snapshot are retained, including when an active placement is removed. Available bindings are reused on apply. Removed providers/profiles require reconnection through Restore widgets. Retired, inactive widget bindings are cleaned up only after their final saved/Undo/active reference is gone.

## Storage and failure handling

`DesktopPresets` uses a private atomic index and immutable, generated-ID JSON blobs, with the existing validated desktop-backup schema. Names are normalized Unicode labels and never paths. Each blob has a size and SHA-256 digest; the entire document/assets are validated before an apply dialog. New data is written before atomically replacing its index reference, and old blobs are pruned afterward. A malformed collection is kept, not replaced with an empty collection. A damaged preset does not mutate the live desktop.

Limits are twelve named presets, sixteen MB per desktop document, sixty-four MB for referenced preset/Undo documents and one MB for metadata. Save/update cannot overwrite another name accidentally. The current setup must be durably recorded before applying another one. Apply reuses the backup validator while preserving notes and old widget bindings. A failed apply attempts rollback and retains the previous snapshot for recovery. Background file work is serialized. Leaving a settings page disposes its callbacks/dialogs; an abandoned preparation cannot apply later into another page. Applying a preset clears stale in-memory settings Undo; the preset page owns its durable Undo.

Deleting/replacing entries schedules only known retired widget IDs for cleanup. Active, pending, other-preset and Undo references protect them. A malformed index conservatively preserves widget bindings and shortcut pins.

## Verification

The local checks cover Google/release builds and lint, JVM tests, and Android-test compilation. Native `presets` smoke covers fresh defaults, complete asset/layout/settings round trips, shared-content preservation, real widget binding retention/cleanup, pinned-shortcut references, restart/Undo, corrupted-file rejection, name/capacity limits, native Save/Cancel/Apply/Undo, settings ordering, removed Stack option and the five palette captures. The `folders` suite separately exercises physical opening/closing. Results and remaining phone limitations are recorded with the delivered APK; a clean full regression result is not implied by these focused checks. Previous broad gesture/setup/rotation and emulator failures remain outside this trial unless reproduced and fixed explicitly.

Trial 4 stays queued for owner review of Trial 3.
