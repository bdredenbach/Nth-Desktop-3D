# Nth Desktop 3D 0.7.4

- Setup uses an internal scrolling body and a pinned navigation row: Back at the lower left, Next at the lower right, and Finish on the final step. The early Explore button is removed. Returning from Android settings still preserves the active permission step and App info recovery action.
- Panel headers have contextual three-dot menus. Folder menus list every registered folder setting. Drawer menus include drawer layout and its search options. Desktop management, widgets, scenes, cards, feed and appearance link to their existing controls. Nth Nota retains its command-examples menu. Labels wrap, and closing or rotating a panel dismisses its menu.
- The Add desktop preview uses the same width, vertical spacers and tilt transform as existing desktop previews.
- Vertical desktop touch interception handles downward as well as upward motion, including swipes beginning on app and folder tiles. Actual scrolling, widget interaction, held drag operations and Gravity Playground physics retain priority.

`Polish074Smoke` runs in the native settings suite and exercises setup Back/Next/Finish placement in portrait and landscape, permission-body scrolling, contextual folder settings and choices, drawer and scene menus, Nota's single examples menu, Add desktop geometry and tilt, nine downward gesture assignments, and scroll precedence. Existing workspace, widgets, core, feed/Discover and settings checks remain enabled.
