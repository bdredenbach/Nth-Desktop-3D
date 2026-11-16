# Celestial realism, 0.8.7

The recovered asset checkpoint is commit `6ebfbe9dd442eec3161bf13eedcebf4aae173809`, based on 0.8.6 main. Packed generated WebP resources were preserved there; scene implementation was recovered in the continuation.

## Rendering

- City in Focus uses the generated straight-alpha black acetate frame. Its matching R/G lens-mask and interior-distance atlas keeps the sharp city confined to the actual photographed openings. The city atlas remains registered, with a mild radius-4.5 blur outside the lenses. Tilt moves the glasses across the city; subtle ambient movement and lens reflections remain independently switchable.
- Aurora Veil uses a transparent alpine silhouette, distant photographic stars and a photographic aurora layer whose sampling coordinates fold and drift over time. The foreground has a separate parallax depth and opaque interiors, avoiding aurora bleed through the terrain.
- Holographic Depth shades a sphere from an equirectangular surface atlas, with independent rotation, directional light, restrained ocean specular and atmospheric scattering at the rim.
- Asteroid Field is appended at ID 10. Five differently sized irregular ellipsoids use the crater/regolith atlas, a texture-derived perturbed normal, different rotation/drift rates and parallax depths. These are analytic shader surfaces rather than a static asteroid photograph.

Names and descriptions are in SceneCatalog/LauncherStore. IDs 0–9 retain their meaning. The existing settings and desktop-backup code uses the catalog length and includes the appended scene without a schema change.

## Texture budget

Keep eight sampler uniforms. Units 0 (pond), 1 (koi), 2 (reef water) and 5 (city) remain fixed. For modes 2, 5, 7 and 10 the GL thread replaces slots 3, 4, 6 and 7 with optical_frame, alpine_foreground, celestial_material and celestial_sky. Other modes restore jellyfish, glass_sky, ice_material and lava_material. Context creation invalidates the bank marker; the next draw uploads its current bank before sampling. Snapshot the chosen mode once per frame so UI updates cannot pair a mode with the other bank.

All textures fit the GLES2 minimum 2048-pixel limit. Upload one bitmap at a time with scaling/premultiplication disabled and recycle it immediately. The packing manifest, source hashes and generation prompts are in scene-087-assets.json. Its old scratch input paths are provenance only; the committed packed textures are the current runtime inputs.

## Checks

`python3 tools/check_scenes.py OUTPUT` compiles/links the shipped GLES shader and lit mesh, renders all eleven scenes in portrait and landscape, verifies animation/freeze/tilt, pond count/speed/touch ripple, aquatic lighting and top-edge coverage, exact bank round trips, and independent aurora motion above still terrain. This is software GLES correctness evidence, not a phone frame-rate benchmark.

The Android realism suite uses PixelCopy from the actual scene surface and covers modes 2/5/7/10 followed by pond/reef/glass/ice/lava, including motion, ambient freeze, tilt, settings persistence, full backup restore, gallery entries and landscape. It runs alongside the descriptorless Discover fixture. Other suites retain their existing coverage and previously documented limitations.

The primary deliverable remains the Google-enabled APK, package com.nth.desktop3d, version 0.8.7 / code 10102, signed using the existing R2 private backup. Do not replace that signing identity. See SIGNED-APK-UPDATES.md and development-handoff.md.
