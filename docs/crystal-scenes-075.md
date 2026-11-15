# Crystal surfaces and scenes — 0.7.5

The desktop manager preview frames, drawer icon plinths and category tabs, Crystal cards and Scene studio choices now share an opaque polished crystal core. The surface draws a thick bright/dark bevel, a silver-blue edge and a restrained curved reflection. Face colors respond to the selected crystal material. Labels keep their existing contrast. The desktop itself retains its existing transparency preferences.

The drawer hint is Search. The About tool is removed from drawer results while About remains in settings. The drawer instruction footer, standalone folder Rename button, desktop-management instruction text and two bottom Scene studio buttons are removed. Folder hold menus still offer Rename. Existing layouts and shortcuts remain.

Tapping a Scene studio card immediately applies it and closes Scene studio. The selected checkmark is visible when reopening the chooser. The three-dot menu still opens the shared scene and motion settings.

| Stable ID | Scene | Environment |
| --- | --- | --- |
| 0 | Spatial Desktop | Original mountains and mesh |
| 1 | Gravity Playground | Original physics and mesh |
| 2 | Holographic Depth | Original stars and planet |
| 3 | Living Pond | Original koi, leaves and ripples |
| 4 | Crystal Atrium | Beveled glass monoliths and a perspective floor |
| 5 | Aurora Veil | Northern lights above an alpine lake |
| 6 | Biolume Reef | Luminous jellyfish and underwater light shafts |

The appended IDs preserve existing saved scenes. Scene selection, settings ranges and settings previews use the expanded catalog. Thumbnail assets are rendered from the exact shipped fragment shader. All new scenes use the existing tilt, ambient-motion, reduced-motion and power-saving controls.

Validation commands:

- `python3 tools/check_scenes.py`: compile and render all seven backgrounds and the mesh; verify ambient movement, disabled-motion stability and tilt for the appended scenes.
- Android build workflow: app builds, lint and unit suite; focused `crystal` suite for actual scene taps, persistence, settings preview loading, drawer cleanup and opaque center pixels across all four crystal materials.
- The inherited settings/rotation suite remains separate so an emulator disconnect cannot hide the focused crystal result.

The Google-enabled deliverable must retain package com.nth.desktop3d, version code 10090 and the existing R2 update certificate. Physical Pixel 10 appearance, accelerometer motion and Google Discover integration remain device checks.
