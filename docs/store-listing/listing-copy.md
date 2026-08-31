# Store listing copy

Paste-ready text for **Play Console → Grow → Store presence → Main store listing**.

Character counts are given because Play enforces them and truncates silently in some surfaces.

---

## App name (30 max)

```
Sinema
```

<sub>6 / 30</sub>

Leaving it as the bare product name rather than padding with keywords ("Sinema — Movie Finder & Tracker").
Keyword-stuffed titles read as spam and Play's own guidance discourages them.

---

## Short description (80 max)

```
Browse, search and save films from TMDB — and keep browsing offline.
```

<sub>68 / 80</sub>

This is the line shown before a user expands the listing, so it leads with the two things that actually
differentiate the app: it is a TMDB client, and it works without a connection.

**Alternatives** if you want a different emphasis:

| Copy | Chars | Emphasis |
| --- | --- | --- |
| `A fast, offline-first movie browser powered by The Movie Database.` | 66 | Speed and the data source |
| `Discover films, save favourites, and browse offline. No ads, no account.` | 71 | Privacy and no friction |
| `Trending, popular and upcoming films — searchable, saveable, offline.` | 69 | Breadth of content |

---

## Full description (4000 max)

```
Sinema is a clean, fast way to explore films — what is trending today, what is popular, what is coming soon — and it is built to keep working when your connection does not.

BROWSE FIVE CURATED LISTS
Trending today, Popular, Now Playing, Top Rated and Upcoming. Every list scrolls endlessly, loading the next page as you reach the bottom, so there is no pagination to fight with.

SEARCH THAT KEEPS UP WITH YOU
Results arrive as you type, without a Search button to hunt for. Filter by genre to narrow things down, and clear the filter to widen them again. Typing quickly does not fire a request per keystroke, so search stays responsive on a slow connection.

DETAIL WORTH OPENING
Each film opens with its backdrop, synopsis, rating, runtime, genres and full cast — in a single screen, from a single request. Posters animate from the grid into the detail header rather than cutting abruptly, so you never lose your place.

SAVE WHAT YOU WANT TO WATCH
Tap the heart to keep a film. Favourites are stored as complete snapshots on your device, which means they survive clearing the cache and remain readable with no connection at all.

DESIGNED TO WORK OFFLINE
Sinema keeps what you have already browsed on your device and treats that as the source of truth. Open the app on a plane or in a tunnel and your films are still there. If a refresh fails, you keep the content you had and get a quiet notice — never a blank screen.

BUILT FOR YOUR SCREEN
Phones get a bottom navigation bar; tablets and unfolded foldables get a navigation rail and a wider grid. Light and dark themes are both first-class, and you can pin either one or follow your system setting.

RESPECTS YOUR PRIVACY
No account. No sign-in. No advertising. Sinema never asks who you are, so it has nothing to sell. Your favourites, your theme and the cached film data stay on your device and are removed when you uninstall. Anonymous usage, crash and performance reports go to Google Firebase so faults can be found and fixed; they are not tied to any identity.

MANAGE YOUR OWN STORAGE
A single tap in Settings clears the cached catalogue and images and shows you exactly how much space that reclaims. Your favourites are deliberately left alone.

Film data and imagery are provided by The Movie Database.

This product uses TMDB and the TMDB APIs but is not endorsed, certified, or otherwise approved by TMDB.
```

<sub>2,373 / 4,000</sub>

### Notes on the wording

- **No superlatives that cannot be substantiated.** No "best", no "#1", no invented ratings — Play's Deceptive
  Behaviour policy covers listing text, not just the app.
- **The privacy paragraph is literally true** and matches both the privacy policy and the Data safety
  declaration. It must be rewritten the moment analytics or crash reporting ships (see
  [`../play-store-notes.md`](../play-store-notes.md)).
- **TMDB is credited twice**, once as a plain courtesy and once with the exact notice TMDB requires. Their terms
  prohibit implying endorsement, which is why the required sentence is used verbatim.
- **All-caps headings** rather than Markdown — Play's description field renders limited formatting, and caps
  survive everywhere.
