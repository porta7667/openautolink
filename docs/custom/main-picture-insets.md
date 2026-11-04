# Custom main-screen CarPlay picture margins

Based on upstream v0.1.575. Settings → Display → CarPlay safe area now has
**Inset entire CarPlay picture**, off by default. When on, the existing calibration
edges physically inset both the main video and its local touch layer. The editor
continues to measure the full parent. No duplicate safe insets are sent to iPhone.
The driver-display stream is unchanged.

Enable the switch, select Calibrate picture margins, place the four boundaries
inside the visible screen, select Use calibration, then the main Save changes.
Unplug/reconnect USB. Use Video → Fit to window for standard-tier video that uses
the inset rectangle's aspect. Exact/custom presets retain aspect-fit and may add
black borders. Background outside the picture is black. The shape is rectangular;
there is no automatic curve detection or contour warp. Turn the switch off and
save/reconnect to restore upstream rendering.

Proposed first trial: top 5%, bottom 0%, left 0.45%, right 12.05%, then tune on the
parked truck. These are trial margins, not measured panel geometry.

Software tests do not establish hardware decoder, physical touch or phone acceptance.
Verify music top tabs/search, maps right-side controls, all four touch corners,
pinch zoom, portrait-independent phone reconnect, settings/calibration overlay,
screen mode changes and continued cluster maps while parked.
