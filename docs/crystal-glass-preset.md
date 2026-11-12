# Crystal Glass — approved baseline

Crystal Glass is the default visual preset for the next Nth Desktop 3D implementation pass.

| Setting | Default | Normalized |
| --- | ---: | ---: |
| Depth | 60% | 0.60 |
| Glass transparency | 72% | 0.72 |
| Motion intensity | 35% | 0.35 |
| Rendering | Balanced | balanced |
| Accelerometer parallax | On | true |
| Dynamic shadows and highlights | On | true |

## Implementation requirements

- Expose these as user-adjustable persistent settings, and use them as defaults for new installations. Do not overwrite saved user preferences on upgrade.
- Depth affects spatial offsets/perspective, motion intensity scales tilt-driven movement, and transparency controls glass panel opacity (28% opacity at 72% transparency, before legibility safeguards).
- Balanced is the default GPU quality tier; avoid expensive full-screen multipass blur. Adapt to battery saver and reduced motion.
- Keep contrast and touch targets readable regardless of transparency; use a fallback opaque surface where needed.
- Parallax and shadow rendering stop when activity is paused, and sensor movement is filtered/clamped.
- Preserve other scene modes and the existing canonical shortcut layout.

Status: approved specification; runtime wiring and APK verification pending.
