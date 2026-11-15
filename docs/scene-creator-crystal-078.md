# Layered scene creator and rolled crystal edges — 0.7.8

Settings > Wallpaper > Create or edit a scene opens three steps: gallery background (or a gradient), up to six optional objects, and depth/preview. Crystal, glass orb and prism star objects are built in. Imported transparent PNG/WebP images remain separate foreground layers; the editor does not automatically segment a flat photograph. Drag an object to position it, change size/rotation, move it to the front or remove it. Each object and the background have independent depth; motion strength and scene parallax are adjustable. Reduced motion, disabled tilt/parallax and Android battery saver suppress camera movement. Rendering occurs on changes/sensor updates rather than a continuous animation loop.

The draft is separate from the applied scene. Closing saves a draft; Discard requires confirmation and leaves the applied wallpaper intact. Apply atomically saves the scene before switching wallpaper source. Gallery images are copied, EXIF-normalized and bounded; imported foreground alpha is preserved. The active scene is self-contained in an atomic JSON file (8 MiB cap, six objects, image checksums and bounded coordinates/size/depth). Scene file work uses a serial executor which drains pending draft saves during Activity destruction. Configuration changes reopen the draft.

Desktop backup format 2 gains an optional customScene record. Existing 0.7.7 backups remain readable. The complete scene, supplied images, order, placement, rotation and depth are validated before restore; the existing Undo snapshot includes the previous active scene. Drafts are local editing state, not part of an applied scene backup. Older app versions cannot import backups containing the new wallpaper source; update the receiving launcher first.

Android wallpaper apps remain optional. ACTION_SET_WALLPAPER now opens a chooser. Installed live wallpaper services are queried through a scoped package-visibility declaration and shown in a launcher list; selecting one opens Android's explicit component preview for confirmation. The system chooser is retained as fallback. The launcher forwards page offsets and empty-space taps to Android wallpapers and reapplies visibility on return. Exact behavior of a particular third-party wallpaper app requires an on-device check.

Solid and translucent crystal materials retain an illuminated top/left shoulder. The right and lower hard outlines are replaced by clipped shading and reflected-light rolls, producing rounded thickness without a four-sided frame. Existing elevation and perspective remain in use. This is drawn optical styling rather than physically simulated refraction.

Primary Android references:
- https://developer.android.com/reference/android/app/WallpaperManager#ACTION_CHANGE_LIVE_WALLPAPER
- https://developer.android.com/reference/android/app/WallpaperManager#EXTRA_LIVE_WALLPAPER_COMPONENT
- https://developer.android.com/reference/android/app/WallpaperManager#ACTION_LIVE_WALLPAPER_CHOOSER
- https://developer.android.com/reference/android/service/wallpaper/WallpaperService#SERVICE_INTERFACE

Aquatic lighting: Living Pond uses a moving caustic cell network warped by image-derived rock relief, drawn before the koi. Biolume Reef applies the same treatment to the rock floor, with broad drifting light shafts, moving surface glints and a restrained jellyfish shimmer. Aquatic light (0–100, default 75) sets intensity; Ambient animation, Reduced motion and power saver freeze movement. This follows photographed surface features rather than reconstructing a 3D mesh from the photo. The GLES check isolates time-varying lit/unlit differences over the floor and checks pause; native PixelCopy checks real SurfaceView intensity/movement/pause.
