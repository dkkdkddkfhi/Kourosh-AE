# Kourosh-AE Android design contract

## Intent

Kourosh-AE should feel like a royal Persian command console: black glass, gold frames, one
live connection dial. The home screen is a single-purpose connection console: the connection
state is visible at a glance, the main action is physically obvious, and live connection
facts (exit IP, country, rates, totals, duration) are readable without hunting for them.

This is Kourosh-AE's own visual system. Any third-party app is an interaction reference only;
do not copy its code, wording, logo, or branding.

## Foundations

- **Platform:** native Android views, platform typography, and Android system bars. No Compose
  runtime and no Material components ship with the app; every surface is hand-drawn
  (`Sculpt.kt` for the shared glass, `AuroraDesign.kt` for the home screen's own painter) and
  carried on the palette in `AppAppearance.kt`.
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
  and Chinese (see `Typefaces`, `FontChoice`). The home screen's steps are fixed in `Aurora`:
  captions 8.5sp with wide tracking, body 13sp, readouts 15sp mono, timer 16–17sp mono. Latin-only
  letter spacing: joined scripts shatter under it.

## Home screen — the Aurora console

The screen is drawn by `AuroraDesign.kt` (tokens, panel painter, icons, backdrop),
`AuroraDial.kt`, `AuroraWidgets.kt` and `AuroraCards.kt`, and assembled in
`MainActivity.createHomeHeader/createHomeConsole/createHomeDock`. The previous console —
palace-photo backdrop, tick gauge dial, waveform tiles, segmented rail, text-glyph navigation —
was deleted outright, not restyled.

**One loud object per screen.** The hero panel is the largest surface and the only one with a
state-tinted body; the cards under it are deliberately quieter (smaller radius, neutral fill, a
hairline rather than a lit frame). Depth comes from light — body gradient, top specular, inner
bottom shadow, 1dp bevel — never from a stack of identical outlined boxes.

Reading order, top to bottom:

1. **Header** — drawn-icon menu button, the crest on a lit tile, the KOUROSH-AE / PRIVATE NETWORK
   lockup, the state LED in its own pill, and the settings button. No text glyphs anywhere.
2. **Hero** — a state-tinted panel holding the dial, the state pill, the live throughput, the
   headline and its two-line detail, and the latency/transport chips. Everything needed to answer
   "is the tunnel up, on what, and how fast" is inside one object.
3. **Live traffic** — one card: the dual-series chart on top, the three rates below it as metered
   tiles, the session totals on the card's header line.
4. **Exit node** — the address the tunnel is really leaving from.
5. **Transport** — a grid of glyph pills under a SELECT PROTOCOL caption, locked while a tunnel
   is up.
6. **Chain / split / MIM** — the one applicable card, in a fixed-height slot.
7. **Signal strip** — the ambient strip that closes the screen.
8. **Dock** — four drawn-icon entries with the current page on a lit pill, and the connect
   control elevated through the bar.

Rules that come out of that order:

- **The dial is the state.** One continuous ring: a 40° cap at rest, an amber comet while
  dialling (with the transport's own percent when it reports one), a closed mint ring that
  animates shut on connect, red only after a reported failure. No tick gauge, no overlapping
  radar sweep, no second progress bar — one idea per state, given room.
- **The numbers live in the glass.** The connect estimate and the session timer are set inside
  the disc, so the control answers "how long / how much left" without the eye leaving it.
- **Put the circular control at the visual centre of the hero.** It is the only large filled
  control on the screen, and the hero's whole layout is budgeted around it.
- The app is VPN-mode only; there is no mode selector. The protocol grid is the single control
  of its kind and it is disabled while a tunnel is active.
- Use real connection wording only: `Not connected`, `Connecting`, `Connected`,
  `Connection degraded` and `Connection failed`. Do not claim protection before the core
  reports it is running.
- The dial's accent is `primary` while idle, `amber` while connecting or degraded, `connected`
  while up, and `danger` only after a reported failure.
- Height is budgeted from the dial upward: the dial is the only element that scales when the
  console would overflow the viewport (`AuroraDialView.sizeScale`, floor 0.80), so decoration
  above it is paid for in control size.

## Motion and feedback

- A press scales the dial to 97% for 100ms, then returns over 180ms with a strong ease-out.
- State changes redraw the ring, the glyph and the lighting rather than moving the layout.
- Use Android's standard context-click haptic on an intentional connection tap.
- Keep motion under 300ms and restricted to transform, alpha, and the control's own drawing.
  The only continuous motion is the ambient kind: the dial's orbit dot and bloom, the backdrop's
  24s drift and the signal strip.
- Connected is a lighting state, not a colour swap: the hero frame takes the accent and its body
  lifts, the traffic tiles undim, the live-speed readout takes the connected accent, the strip
  brightens and the backdrop comes up. All of it reverts on disconnect — a glow that outlives
  the tunnel would be a lie.
- Respect Android accessibility: every interactive element has a clear content description;
  colour never carries state alone.

## Guardrails

- No copied third-party assets, names, code, screenshots, or branding.
- No decorative gradients for their own sake, no oversized text, no fake statistics, no
  nested-card dashboards. The lit frames, the dial's bloom and the backdrop's two washes are the
  only light effects in the app; the first two carry state, the third is atmosphere that never
  sits behind text.
- No extra UI libraries for this screen. Add Jetpack Compose or Material dependencies only when
  the app grows enough screens to justify that migration.
- The backdrop is **drawn**, not shipped: `AuroraBackdrop` replaced the 6 MB palace photograph so
  both palettes share one atmosphere and the APK stays small. Do not reintroduce bitmaps for the
  home screen.
