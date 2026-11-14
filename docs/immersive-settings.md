# Immersive settings — 0.4.0

Reference: owner recording 226768.mp4. Home/Dock live previews at 00:14–01:14,
Folders at 01:16–01:48, Search at 01:52–02:12, appearance/gestures/feed afterward.
Mobile plan and subscription promotion are omitted.

## Delivered in this pass

Settings is a native searchable category browser with a persistent live preview.
Home, Dock, Folders, App drawer, Search & Nth Nota, Look & feel, Scenes & motion,
Gestures, Feed, settings backup and launcher information have real controls.
The preview uses current desktop/folder/app identities and the same appearance
preferences as the launcher. Widget footprints are represented without binding
additional widget instances. Drag the preview to rotate its plane. Reduced motion disables
preview rotation and decorative navigation motion. Categories survive Activity
recreation and device rotation; the Nota draft is saved locally.

Settings include grid columns/row height, icon size and labels, font size/color,
side padding, layout locking, infinite pages, auto-placement of new apps, dock
visibility/background/clock/padding/corners, folder columns/size/labels/title/corners,
drawer columns/size/labels, folder search, search keyboard, Crystal material,
lighting, transparency, menu opening style, scene transitions/depth/tilt/quality,
Living Pond controls and configurable empty-space up/down/double-tap actions.

## Fading menu material

AdaptiveMenuDrawable measures the visible icon, folder and widget rectangles
behind the active launcher panel. A bounded 48×80 field uses a smooth spatial
falloff and temporal easing. Geometry is checked at at most 30 Hz; target opacity
is recomputed only when geometry or settings change. Empty regions can be fully
transparent while occupied regions gain opacity. The tint is clipped to the
panel corners. Text and input targets retain native accessibility and touch.
No screen capture, accessibility permission or repeated full-screen blur is used.

This pass tracks launcher objects, not the contrast of arbitrary wallpaper pixels
or individual transparent pixels inside a widget. The empty-area opacity setting
provides a readable floor when desired. Android/system dialogs and provider-owned
widget configuration screens retain their own appearance. All launcher panels
opened through the shared panel host use the adaptive material; it can be disabled
for the existing fixed Crystal surface.

## Nth Nota

Local commands use SettingsCommands and the same SettingsModel registry as the
controls. Examples: "turn off tilt", "set grid to five columns", "make icons
larger", "show folder settings", "open Calculator", and "undo". Bounds are
validated. Questions do not become actions. Undo restores the prior setting plus
shortcut/widget grid placement, rather than merely changing the grid count back.
A continuous slider drag is one undoable change and defers desktop reconstruction
until release while the preview updates immediately.

Cloud questions default off. The visible switch explains that the question and
current setting values are sent to the owner's existing Nth Nota gateway. No
provider keys are shipped; no paid service is added. A successful live request to
the configured endpoint was verified on 2026-11-13. The endpoint may be temporarily
unavailable or rate limited; direct local commands remain usable. Replies are
plain advice and cannot execute commands. Stop, navigation, pause and disposal
cancel outstanding delivery and discard late replies.

## Settings backup

Export creates a versioned .nth3dsettings JSON document through Android's document
picker. Restore validates the format, all setting IDs, integer types and ranges
before mutating settings. Restore is undoable. This is a settings-only backup:
it does not export or replace notes, desktop items, widget bindings, icon caches,
private signing material or cloud consent. It is not a full .nthdesktop migration.

## Remaining reference-video scope

This is the native settings and assistant foundation, not a claim of reproducing
every reference feature. External service cards (Spotify/Outlook/Twitch/Discord/Drive,
etc.), embedded Discover, third-party icon packs, unlimited/custom dock pages,
the full reference transition library and accessibility-based system gesture actions
are not implemented. Unsupported integrations are not presented as working toggles.
The feed continues to open the installed Google app. App, folder and widget
collision handling from 0.3.2 remains authoritative.

## Validation

Pure tests cover opacity envelopes and command interpretation (including number
words, explanation-vs-command, malformed large numbers and navigation). Native
settings tests exercise accessible sliders, real preference persistence, manual
preview rotation, recreation/rotation, local Nota commands/Undo, search, transparent
and feathered material regions, fixed/adaptive switching, document export/restore,
and preservation of notes. The existing separate-UID widget/shortcut regression
runs in the same Android API 35 suite. Signing continues with the owner's R2 key.
