# Kourosh-AE Android design contract

## Intent

Kourosh-AE should feel like a royal Persian command console: black glass, gold frames, a live
connection dial. The home screen is a single-purpose connection console: the connection state
is visible at a glance, the main action is physically obvious, and live connection facts
(exit IP, country, rates, totals, duration) are readable without scrolling.

This is Kourosh-AE's own visual system. Any third-party app is an interaction reference only;
do not copy its code, wording, logo, or branding.

## Foundations

- **Platform:** native Android views, platform typography, and Android system bars. No Compose
  runtime and no Material components ship with the app; every surface is hand-drawn in
  `Sculpt.kt` (`GlassDrawable`) and carried on the palette in `AppAppearance.kt`.
- **Two palettes, both fixed.** `ORBIT` (dark) and `PORCELAIN` (light) are the only themes; the
  user picks one in Appearance. Nothing is derived from the phone's wallpaper — no dynamic
  colour, no `values-night` split, because the choice is the user's, not the OS's.

| Role | Orbit (dark) | Porcelain (light) |
|---|---|---|
| Canvas | `#050505` | `#EEF1F4` |
| Surface | `#0B0A08` | `#FFFFFF` |
| Surface variant | `#12100C` | `#F4F7F9` |
| Ink | `#F3E6C4` | `#111A1F` |
| Muted text | `#A89870` | `#4E6069` |
| Divider | `#5B4522` | `#E0E6EA` |
| Primary | `#F6D98B` | `#0E9C82` |
| Connected | `#22D3C5` | `#17A05E` |
| Error | `#F2A39B` | `#B3261E` |
| Accents | mint `#D4A64A` · violet `#8A6420` · amber `#F6D98B` | mint `#0E9C82` · violet `#6B5BD6` · amber `#A96A08` |

- **Text vs graphics.** Every accent has a `…Text` sibling used for letters, and every letter
  colour in both palettes clears 4.5:1 on the surfaces it sits on. On the dark palette most
  pairs are identical — except violet (letters `#D9B45C`, shapes `#8A6420`) and danger
  (letters `#F2A39B`, shapes `#B3372F`), whose deep royal values fail as letters — and the
  tertiary `faint` was lifted to `#8F8060` for the same reason. Shapes keep the deep colours,
  which is what keeps the dark console royal instead of washed out.
- **Lighting is a property of the palette.** Depth on dark comes from a white specular and an
  inner bottom shadow; on light those vanish, so the model inverts: a real drop shadow
  (`Sculpt.Lighting.elevationDp`) and a card that is *lighter* than the page.
- **Typography:** Roboto / Android system sans, with bundled Vazirmatn and Noto Sans for Persian
  and Chinese (see `Typefaces`, `FontChoice`). Compact hierarchy: headline 21sp, status detail
  13.5sp, labels 14sp, metadata 10–12sp, section captions 8.5–9.5sp with wide tracking. Latin-only
  letter spacing: joined scripts shatter under it.

## Home screen

A royal masthead opens the screen: the lion-crest logo beside the KOUROSH-AE / PRIVATE NETWORK
lockup, with the connection LED next to it. On the dark theme the palace artwork sits behind
the whole screen at 14% under a scrim; the light theme skips it and stays clean porcelain.

The screen is one reading order, top to bottom, and each band answers exactly one question:

1. **Stage** — a sculpted panel with a lit gold frame. State pill and live throughput sit above
   the dial; the headline and its detail sit below it; latency and transport close the panel
   in gold-lit pills. Everything needed to answer "is the tunnel up, on what, how fast" is
   inside one object.
2. **Metrics** — live rates (download / upload / combined) as waveform tiles, with the session
   totals on one line beneath them.
3. **Exit node** — the address the tunnel is really leaving from.
4. **Transport** — the picker under a SELECT PROTOCOL caption, locked while a tunnel is up.
5. **Chain / split / MIM** — the one applicable card, in a fixed-height slot.
6. **Signal trace** — the strip that closes the screen.

Rules that come out of that order:

- The header carries the brand: crest logo plus the KOUROSH-AE / PRIVATE NETWORK lockup. The
  "server" badge, the tagline and the security panel stay gone: the exit card states the real
  exit, and an app may not advertise privacy facts the core has not reported. State is carried
  by the dial, the pill and the headline, which all read the same source.
- Put the circular connection control at the visual centre of the stage. It is the only large,
  filled control on the screen and has a minimum 176dp target.
- Place the selected connection status immediately below the circle.
- The app is VPN-mode only; there is no mode selector. The protocol picker is the single
  outlined, full-width control beneath the stage, and it is disabled while a tunnel is active.
- Use real connection wording only: `Not connected`, `Connecting`, `Connected`,
  `Connection degraded` and `Connection failed`. Do not claim protection before the core
  reports it is running.
- The control uses the primary colour while idle or connecting, brighter green while connected,
  amber when a tunnel is up but failing its health check, and red only after a reported
  connection failure.
- Height is budgeted from the dial upward. Only the dial scales when the console would
  overflow the viewport, so decoration above it is paid for in control size; a shorter stage
  is a bigger dial.

## Motion and feedback

- A press scales the circular control to 97% for 90ms, then returns over 180ms with a
  strong ease-out. State changes redraw the ring and icon rather than moving the layout.
- Use Android's standard context-click haptic on an intentional connection tap.
- Keep motion under 300ms and restricted to transform, alpha, and the control's own drawing.
- Latency keeps its normal graph while probing. After each returned `N ms`, lift and settle the
  graph over 280ms without moving surrounding content.
- Respect Android accessibility: every interactive element has a clear content description;
  colour never carries state alone.
- While turning on, the dial shows a live percent answering "how much is left": the
  transport's own number when it reports one (Tor bootstrap), otherwise a UI-side
  time-based estimate that asymptotes at 95% and clears the moment the tunnel resolves.
- Connected is a lighting state, not a colour swap: the stage frame goes bright in the
  state accent, its fill lifts, the metric tiles undim, the live-speed readout takes the
  connected accent, and (dark theme) the palace backdrop fades brighter. All of it reverts
  on disconnect — a glow that outlives the tunnel would be a lie.

## Guardrails

- No copied third-party assets, names, code, screenshots, or branding.
- No decorative gradients, glow, oversized text, fake statistics, or nested-card dashboards. The
  lit frames, the dial's halo and the dark theme's 14% palace backdrop are the only light
  effects in the app; the first two carry state, and the third is atmosphere that never sits
  behind text without its scrim.
- No extra UI libraries for this first screen. Add Jetpack Compose or Material dependencies
  only when the app grows enough screens to justify that migration.
