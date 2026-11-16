# Scene-matched lighting, 0.8.9

SceneLight is the pure-Java profile/phase model for all eleven built-in environments. SceneLighting is the per-activity controller: a foreground-only 30 Hz clock sourced from SceneSurface.elapsedSeconds, weak drawable/view observers, bounded 128-square reflection maps, and a current strength-blended map. IconShellDrawable caches optical rasters by shape/folder/material/transparency and scene/strength signature; the raster does not change with each animation frame.

The reflection sources are Spatial's lake, Gravity's thumbnail, the planet thumbnail, pond bed, architectural sky, the aurora atlas half, reef water, the sharp city atlas half, ice material, lava material and the stars atlas half. Original glass_sky supplies the neutral/off environment. Blend strength is adjustable from 0–100; off/zero returns the exact neutral map. Non-built-in wallpapers use the neutral path for this trial.

One cached gradient per surface carries the shared primary/secondary light colors. Only its matrix and alpha change with phase/tilt. Aurora and lava use related shader-clock frequencies, with restrained secondary colors. SolidCrystalDrawable, CrystalGlassDrawable and IconShellDrawable share the same controller. Settings/desktop miniatures are invalidated through weak view observers. AdaptiveMenuDrawable applies the light to RGB only, retaining its existing per-pixel alpha mask, fading behavior and native input.

SettingsModel exposes scene_lighting and scene_light_strength near the beginning of Look & feel, supports existing export/import/Undo, and the offline settings parser accepts the new controls. Frame state is ephemeral and is not written to preferences every frame.

Reduced motion, system animation reduction and power saving freeze temporal lighting and tilt. Ambient off and Subtle lighting freeze the temporal sweep; existing tilt/lighting controls gate motion. MainActivity already stops foreground frames on pause, so the new controller introduces no background timer. Hidden/detached surface observers do not force display work. The existing shader, scene IDs, desktop schema, item geometry, Gravity mechanics and folder transitions are unchanged.

Verification covers pure profile selection, comparison-off/zero, tint opacity preservation, motion/freeze/tilt bounds, color families, safe numeric input and command routing. The native SceneLighting089Smoke checks real optical material round trips, fading-menu alpha and settings export/import with isolated settings, restoring the live controller afterward. Native execution is separately reported; it is not implied by JVM/software-render checks.

See feature-trials.md for the owner-approved one-feature-at-a-time review sequence. Version 0.8.9/code 10104; Google-enabled APK; existing R2 update certificate.
