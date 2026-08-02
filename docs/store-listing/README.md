# Store listing assets

Everything Play Console asks for on the main store listing, sized to spec and generated from the app itself
rather than mocked up — the icon is built from the shipped adaptive launcher icon, and every screenshot is a
real capture of a real build, so the listing cannot drift from what users install.

## Where each file goes

| Play Console field | File | Spec | Delivered |
| --- | --- | --- | --- |
| App icon | `icon-512.png` | 512×512, 32-bit PNG | 512×512 RGBA ✓ |
| Feature graphic | `feature-graphic.png` | 1024×500, no alpha | 1024×500 RGB ✓ |
| Phone screenshots | `phone/01…05` | 2–8, max 2:1 ratio, 320–3840 px | 5 × 1080×2160 ✓ |
| 7-inch tablet screenshots | `tablet7/01…04` | up to 8, 320–3840 px | 4 × 1200×1920 ✓ |
| 10-inch tablet screenshots | `tablet10/01…04` | up to 8, 320–3840 px | 4 × 1600×2560 ✓ |
| App name, short & full description | `listing-copy.md` | 30 / 80 / 4000 chars | 6 / 68 / 2303 ✓ |
| Privacy policy URL | [`../privacy-policy.html`](../privacy-policy.html) | public URL, no login | needs hosting — see [`../play-store-notes.md`](../play-store-notes.md) |

## How the assets were produced

**Icon.** `icon-512.svg` carries the same path data as
`sinema/src/main/res/drawable/ic_launcher_*.xml`, with the viewBox set to the central 72×72 of the 108×108
adaptive canvas — the region a launcher mask actually shows — so the store icon and the home-screen icon are the
same artwork at the same proportions. Rendered with headless Chrome.

**Feature graphic.** `feature-graphic.html`, rendered at 1024×500. Deliberately typographic: no film posters
appear in it. Poster artwork is studio-owned and licensed to TMDB for display *inside* the app, which is not
the same as a licence to use it as store marketing.

**Screenshots.** Captured from a debug build on an emulator, with the display resized to each form factor
(`adb shell wm size` / `wm density`) so the tablet captures exercise the real navigation-rail layout rather
than a stretched phone layout:

| Set | Display | Density | Effective width | Layout shown |
| --- | --- | --- | --- | --- |
| phone | 1080×2364 | 390 dpi | 443 dp | bottom navigation bar |
| tablet7 | 1200×1920 | 320 dpi | 600 dp | navigation rail |
| tablet10 | 1600×2560 | 320 dpi | 800 dp | navigation rail, 5-column grid |

Phone captures are cropped from 2364 to 2160 rows — status bar off the top, gesture pill off the bottom. That is
not cosmetic: the raw capture is 2.19:1, and Play rejects phone screenshots beyond 2:1.

## Regenerating

```bash
# icon
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --disable-gpu \
  --screenshot=icon-512.png --window-size=512,512 --default-background-color=00000000 \
  file://$PWD/icon-512.svg

# feature graphic
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --disable-gpu \
  --screenshot=feature-graphic.png --window-size=1024,500 \
  file://$PWD/feature-graphic.html
```

Both then need a mode fix — the icon must be RGBA, the feature graphic must have no alpha channel at all.

## Known gap

On a 10-inch tablet the **detail screen leaves roughly the lower third empty** (`tablet10/02-detail.png`). The
content is correct but the layout does not use the width: it is a phone layout on a large canvas. §9·04 of the
technical solution already lists the adaptive list–detail layout as outstanding work, and this is what that gap
looks like to a user. The screenshot is honest, but a two-pane detail layout would make it a much stronger
asset.
