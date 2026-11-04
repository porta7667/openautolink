# Optional main-screen background matching

Custom version0.1.575-background1 (code577), based on upstreamv0.1.575 and the prior optional picture-inset patch.

Settings → Display → CarPlay safe area → Match background in margins. DefaultOFF; shared draft and global Save apply on next USB connection. Enabling matching turns whole-picture insetsOFF; enabling whole-picture insets turns matchingOFF. A corrupted record with bothtrue uses only whole-picture insets. With no nonzero safe margins, matching has nothing to paint.

The phone still receives the exact full-window video geometry and original encoded safe rectangle. The main picture and touch layer keep their original size/localcoordinates. Only the pixels outside the encoded safe rectangle are painted, using gradients from colors sampled just inside that rectangle. Calibration remains in the full parent; there is no automatic bezel outline detection. Cluster output is unchanged.

PixelCopy downsamples the decoder SurfaceView to160pxwide,16–96pxhigh, at most one request at a time and no more than one completedrequest plus400ms. Per-strip and longitudinal medians soften isolated highlights. Small color arrays are retained; each copiedBitmap is recycled after extraction. PixelCopy reads the decoderSurface, so the overlay cannot sample itself. No frame files or network transfer. Capturefailure clears thefill and retries; stop, background lifecycle, resize, surfacegeneration change, settings/diagnostics and disconnect cancel theloop and reject late results.

This blends colors; it does not create new map content, extend album art faithfully, remove iPhone card borders or rearrange controls around a curve. Artwork/maps may tint the fill. Physical GM PixelCopy support, appearance across apps/transitions and playbackperformance require parkedtrucktesting. Tests are not proof of OEMhardwareacceptance.

Checks: settings roundtrip/defaultOFF, immutable requestidentity, unchanged wireoffer, exact safe geometry, sampledsource excludes wallpaper, smoothing/bounds, oneoutstandingcopy, boundedretry, late/duplicatecallbackrejection, renderonlyoutsidesafe, actualSurfaceViewsizeunchanged, touchpass-throughanddisable.

Truck test: retain existing calibratedsafeedges; enable Match background in margins; save and USBreconnect. Compare MusicNowPlaying and browsepage; map; dashboard; app/song switches; top/rightcorner taps; pinch; cluster maps. TurnOFF/save/reconnect if filllookswrong or videoislesssmooth. Keep existingversion576 available in localarchive; no appuninstallneeded.
