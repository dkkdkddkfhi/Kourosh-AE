# Kourosh-AE Android design contract (3.0, imperial holo)

## Intent

Kourosh-AE is a trustworthy Android VPN console dressed as an imperial night
scene: ornate gold, neon cyan, dark glass. The home screen stays
single-purpose. The connection state is visible at a glance, the connect dial
is physically obvious, and live connection facts (exit IP, country, data,
duration) are readable without scrolling.

This is Kourosh-AE's own visual system. Any third-party app is an interaction
reference only; do not copy its code, wording, logo, or branding.

## Foundations

- **Platform:** native Android views and Canvas, platform typography, Android
  system bars. No UI framework migration and no image assets for the look:
  every frame, emblem, and glow is drawn in code.
- **Palettes:** exactly two, defined in `AppAppearance.kt`.
  - `ORBIT` (dark): canvas `#05080D`, surface `#0B121B`, ink `#F3E6C4`,
    gold `#E3B856` / frame gold `#D4A64A`, cyan `#22D3EE`, violet `#B45CFF`.
  - `PORCELAIN` (light): canvas `#F3ECDD`, surface `#FFFCF5`, ink `#1A1508`,
    gold `#B0852B`, teal `#0891B2`, violet `#7C3AED`.
  - Every accent has a `...Text` sibling that clears 4.5:1 for letters; vivid
    accents are for shapes only.
- **Metal:** gold is always a gradient, never a flat fill: highlight
  `#F6D98B`, body `#D4A64A`, shadow `#8A6420` on dark; `#E6C36F`, `#B8892F`,
  `#7A5718` on light.
- **State accents:** idle uses gold, connecting and degraded use amber,
  connected uses cyan, failure uses danger.
- **Typography:** system sans for English, Vazirmatn for Persian, Noto Sans SC
  for Chinese, system mono for digits (see `Typefaces.kt`). Letter-spacing is
  applied to Latin text only.

## Panels (GlassDrawable)

- Every card, row, chip, and button is a chamfered (cut-corner) panel. The cut
  follows the caller's corner radius and never exceeds 30% of the short side.
- Layers: drop shadow (light palette only), body gradient, top sheen, inner
  shadow, gold gradient frame. An accented panel adds a neon inner glow and an
  accent inner line. Large panels add a dark-gold inner frame and gold diamond
  studs at top and bottom centre.
- Pressing darkens the body, removes the sheen, and brightens the glow.

## The dial (OrbitDialView)

- A gold medallion: crest rays, a metal bezel with engraved ticks and four
  cardinal studs, a neon ring in the state accent, an inner gold ring, and a
  dark glass core.
- Inside the core: a holographic wireframe globe that turns slowly, a
  code-drawn crown, and the state content.
  - Idle: power glyph and `TAP TO CONNECT`. Failed: the same in danger with
    `RETRY`.
  - Connecting: radar arcs, a comet on the neon ring, and `CONNECTING` or
    `CONNECTING n%` only when the transport reports real progress.
  - Connected: `CONNECTED`, the session timer, and the ticks light up.
  - No check mark or protection claim appears before CONNECTED.
- Minimum touch target 176dp. The view is measured as ring + bleed so the halo,
  crest, and ripples are never cropped.

## Home screen

- Put the dial at the visual centre. It is the only large, lit control.
- Place the selected connection status immediately below the dial.
- The protocol picker is disabled while a tunnel is active.
- Use real connection wording only: `Not connected`, `Connecting`,
  `Connected`, and `Connection failed`. Show real data only: no fake servers,
  protocols, or statistics.

## Motion and feedback

- A press scales the dial to 96.5% over 110ms, then returns over 190ms.
- A tap counts only when the finger is released inside the dial. A disabled
  dial ignores touch and D-pad.
- Use Android's standard context-click haptic on an intentional connection tap.
- The dial loop runs at 8s when idle or connected and 1.15s while connecting,
  switches tempo with the state, is capped at about 30fps, and stops while the
  window is hidden or the view is detached.
- Respect accessibility: every interactive element has a content description,
  colour never carries state alone, and D-pad focus shows a visible ring.

## Notification and tile icon

- `ic_kourosh_notification`: a 24dp white shield with a "K" cut out
  (even-odd fill). Monochrome silhouette only, as Android requires.

## Guardrails

- No copied third-party assets, names, code, screenshots, or branding.
- No heavy UI or 3D libraries. The arm64 APK must stay under Telegram's 50 MB
  document cap.
- The dial must never be the reason the app drains battery: no redraws while
  hidden.
