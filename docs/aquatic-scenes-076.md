# Lifelike aquatics and uniform effects — 0.7.6

Living Pond and Biolume Reef use photographic-style generated environment textures and separate transparent animal layers in the live GLES renderer. Koi follow independent bounded paths, face their direction of travel, flex their bodies/tails and move their fins. Jellyfish have individual paths, depths and speeds, with bell pulses and trailing-arm motion. Ambient off, reduced motion and power saving pause autonomous movement through the existing renderer controls. Existing pond fish count, speed, scene shadows and touch-ripple settings remain.

Four bounded WebP textures are loaded once per GL context. Bitmap scaling is disabled and straight alpha is used for the animal cutouts; decoded CPU bitmaps are recycled after upload. The shader samples animals only inside their bounds. The environment is center-cropped for portrait and landscape. Generated source artwork is converted for packaging without changing its composition; original alpha is retained. `tools/check_scenes.py` loads the identical textures and uses the same orientation as Android.

## Effects

Fresh initialization defaults to desktop effect ID 4 and menu/folder style ID 3, all named 3D Stack. Existing saved integer choices remain valid. The desktop Stack has perspective tilt as well as stacking scale/translation; menus and folders share the same opening pose. Reduced motion still suppresses animations.

Look & feel begins with Use 3D Stack everywhere and the Desktop transition, Menu opening animation and Folder opening animation selectors. These are outside Advanced. The shared preset is one undoable operation across both preference files. Folder and scene contextual settings continue to route to the canonical controls. Each selector exposes its complete effect list and a working preview.

## Validation

- Exact shipped GLES shader and all four texture assets: seven scene renders, animated and paused aquatic frames, parallax, koi count/speed, pond ripple and mesh compilation.
- Native focused suite: isolated fresh-install defaults and preservation of stored choices; shared preset/Undo; texture decoding and transparent corners; top-level effect controls and live photo-scene screenshots.
- Standard Android build, lint, unit and inherited native suites. Detailed CI outcomes belong in the delivered verification file, including any emulator-infrastructure gaps.

Artwork generation prompts and packaged asset hashes are recorded in aquatic-assets-076.json. Physical Pixel 10 performance, sensors and Google app integration require device testing.
