# Spatial Desktop and optical shells, 0.8.8

## Spatial Desktop

Scene ID 0 is now a photographic dusk mountain lake. A separate transparent wet-rock shoreline moves at a nearer depth; subtle water distortion stays below the photographed horizon. The central glass orb is an analytic sphere with photograph-based refraction/reflection, restrained chromatic separation, a Fresnel rim, directional specular light and a lake shadow. Its small ambient drift and water movement freeze when ambient motion is disabled. Reduced motion and power saving retain the existing scene controls.

The live shader supplies its gallery/settings thumbnail. Portrait and landscape retain the same scene ID and existing backups. The original polygon mesh is now drawn only for Gravity Playground (ID 1), whose background is unchanged.

Generated source prompts/hashes are in spatial-088-assets.json. New packed resources are spatial_environment.webp (1536 × 1024) and spatial_foreground.webp (1024 × 1024 with straight alpha). A third GL texture bank shares slots 3/4/6/7; all eleven scenes remain inside GLES2's eight-sampler budget. Scene/context changes restore the correct bank before drawing.

## Icon material

IconOptics is the pure-Java material rasterizer used directly by IconShellDrawable. It samples the existing photographic glass_sky environment along bevel normals, with a continuous chamfer, Fresnel-weighted reflection, soft light-source highlights, glass absorption, a small dark extrusion and independent folder tint. Rounded-square, circle and square use distinct matching silhouettes. Saved app and folder shape values retain their different existing enum orders.

IconShellDrawable shares bounded 192-pixel ARGB textures in a 24-entry cache; it retains/reuses active bitmaps safely and decodes the reflection photograph once at 128-pixel resolution. Changing tilt updates only a lightweight glass-cover gradient, not the raster. The cover is drawn above the actual installed app art and folder children so they sit beneath the same lens. Menus, desktop tiles, folder/app shape previews and desktop-management miniatures use the same shell/cover. Global material/transparency choices still apply; shadows and dynamic lighting follow their existing preferences. Transparency endpoints control the core while retaining reflective edges.

Gravity's frame integration, collision response, widget avoidance, input/drag behavior, physics tile sizing, pinning and saved positions are unchanged. DesktopView changes only send presentation light values to tile drawables and reset those values for reduced motion/power saving. No layout migration or backup schema change is needed.

## Validation

- Build Google/release and Android-test APKs; run Google/release lint and JVM tests.
- IconOptics tests cover shape containment, matching app/folder geometry, tint/material differences, transparency endpoints/rim, photographic environment response and deterministic input-preserving rendering.
- tools/check_scenes.py compiles the shipped shaders, renders all eleven scenes in portrait/landscape, verifies Spatial and other photographic motion/freeze/tilt, restores exact frozen frames after texture-bank round trips, and retains aquatic controls/light/edge and aurora-independence checks.
- Native Realism087Smoke includes scene 0 in its existing PixelCopy, persistence, backup, tilt, motion and rotation checks. Native emulator/phone execution remains separately reported; local software GLES is not a phone performance benchmark.

Package com.nth.desktop3d, version 0.8.8 / code 10103. The primary Google-enabled APK continues using the same R2 signing certificate and installs as an update.
