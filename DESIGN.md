# Kourosh-AE Android design contract (3.0)

## Intent

Kourosh-AE is a trustworthy Android VPN console with one living object at its
centre: the connection orb. The home screen stays single-purpose. The
connection state is visible at a glance, the main action is physically
obvious, and live connection facts (exit IP, country, data, duration) are
readable without scrolling.

Version 3.0 moves the dial from a flat glass disc to a lit 3D orb. The orb is
the only place where depth, glow, and motion are spent. Everything around it
stays quiet so the orb reads as the product.

This is Kourosh-AE's own visual system. Any third-party app is an interaction
reference only; do not copy its code, wording, logo, or branding.

## Foundations

- **Platform:** native Android views and Canvas, platform typography, Android
  system bars. No UI framework migration for this release.
- **Palettes:** exactly two, defined in `AppAppearance.kt`. `ORBIT` (dark
  glass) and `PORCELAIN` (light). Colours are never derived from the
  wallpaper. Every accent has a `...Text` sibling that clears 4.5:1 for
  letters; vivid accents are for shapes only.
- **State accents:** idle uses `muted`, connecting and degraded use `amber`,
  connected uses `connected`, failure uses `danger`.
- **Typography:** system sans for English, Vazirmatn for Persian, Noto Sans SC
  for Chinese, system mono for digits (see `Typefaces.kt`).

## The orb (OrbitDialView)

- Rendered as a sphere, not a disc: per-pixel diffuse light, a tight specular,
  and a fresnel rim in the state accent.
- Energy under the glass follows the state: almost still when idle, flowing
  while connecting, fully alive when connected.
- A rotating orbital grid and a sheen band (connected only) sit on top.
- **Rendering:** Android 13+ (API 33) uses an AGSL `RuntimeShader`
  (`OrbCoreShader.kt`). Android 8 to 12, and any device whose driver rejects the
  shader, use the Canvas renderer. Both paths must look like the same object.
- **Parallax:** the light, the grid, and the drop shadow move a little with the
  phone's tilt (gravity sensor). The sensor runs only while the orb is visible
  and is off when the system "remove animations" setting is on.
- Minimum touch target 176dp. The view is measured as ring + bleed so the halo
  and ripples are never cropped.

## Home screen

- Keep the top of the screen quiet: no logo, no wordmark, no decorative cards.
  The brand lives on the launcher icon and the opening splash.
- Put the orb at the visual centre. It is the only large, lit control on the
  screen.
- Place the selected connection status immediately below the orb.
- The protocol picker is the single outlined, full-width control beneath the
  status, and it is disabled while a tunnel is active.
- Use real connection wording only: `Not connected`, `Connecting`,
  `Connected`, and `Connection failed`. Do not claim protection before the core
  reports it is running. The shield with a checkmark never appears before
  CONNECTED.

## Motion and feedback

- A press scales the orb to about 97% for ~100ms, then returns over ~190ms.
- A tap counts only when the finger is released inside the orb.
- State changes blend the accent colour and the orb energy over 280ms.
- Use Android's standard context-click haptic on an intentional connection tap.
- Discrete transitions stay under 300ms. Ambient loops (the orb energy, the
  ripples, the connecting wave) are the only continuous motion, and they pause
  when the window is hidden.
- Respect accessibility: every interactive element has a content description,
  colour never carries state alone, and D-pad focus shows a visible ring.

## Notification and tile icon

- `ic_kourosh_notification`: a 24dp white shield with a "K" cut out
  (even-odd fill). Monochrome silhouette only, as Android requires. It is also
  the Quick Settings tile icon.

## Guardrails

- No copied third-party assets, names, code, screenshots, or branding.
- Glow and gradients belong to the orb and its ring. No neon backgrounds,
  oversized text, fake statistics, or nested-card dashboards elsewhere.
- No heavy UI or 3D libraries. The arm64 APK must stay under Telegram's 50 MB
  document cap.
- The orb must never be the reason the app drains battery: no redraws while
  hidden, no sensor while hidden.
