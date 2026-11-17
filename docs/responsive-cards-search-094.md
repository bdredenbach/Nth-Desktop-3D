# Responsive cards and unified search — 0.9.4 / Trial 6

The owner supplied 227388.mp4 and requested card data to adapt to the resized card. The recording could not be inspected while the execution environment was unavailable. This implementation is based on the source and the owner's written description; video and phone review remain required.

Desktop cards now measure content against the real viewport. Narrow/short cards reduce spacing and modestly reduce text, with a 12sp floor that retains Android font scaling. Buttons wrap and grow rather than clipping their labels, and retain at least 48dp touch targets. Photos use FIT_CENTER and a viewport-bounded height to retain their complete image. Growing a card restores the original larger type and spacing.

Remove the previous 200-character Notes preview cutoff. Full saved Notes remain in the card. Finite short content can fit; arbitrarily long notes, reading lists, event titles or enlarged accessibility text remain scrollable with an explicit scrollbar. Data is never deleted to make it fit, and text is never shrunk without a readability bound. Native child actions, stationary hold menus, system card drags and resize grips retain their existing paths.

Trial 6 adds local results beneath app/folder matches in the drawer's existing search. Settings are ranked by name, then descriptions and all query words; category results open the matching page. Setting results focus the real control without changing its value. Notes matches open the complete note and select the matching text where possible. Ask Nth Nota opens the query as a draft; only the existing Send action can run commands or request optional cloud advice. Note content is not appended to Nota drafts.

Matching is case/diacritic insensitive. Preserve Nth tools and folder filter behavior, the Search folders preference, original app/profile keys, held menus and dragging. Local matches disappear when search clears. When local results exist with no app matches, the local results receive the viewport. Enter opens only a unique concrete result and never submits a Nota request.

Validation is pending at initial publication. The focused adaptive suite covers all eight card types across small/wide/tall viewports, complete long Notes, type restoration, wrapping actions and setting/note/folder/Nota routing without execution. Existing Cards/Placement/Drawer suites remain in CI. No signed APK is available until the execution environment resumes. Trial 7 remains queued for owner feedback.
