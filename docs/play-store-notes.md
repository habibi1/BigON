# Play Store submission notes

Operational notes for publishing Sinema. Not part of the technical solution — kept here so the answers are not
re-derived at submission time.

> **These notes are engineering documentation, not legal advice.** The privacy policy in
> [`privacy-policy.html`](privacy-policy.html) was written to describe what the code actually does, verified
> against the manifest, the DI graph and the network layer. You are responsible for its accuracy, and it must be
> revisited whenever the app's data behaviour changes.

## 1. Hosting the privacy policy

Google Play requires a privacy policy at a URL that is **publicly reachable without a login** and stays
reachable. A Google Doc, a Dropbox link or anything behind sign-in will fail review.

Recommended: **GitHub Pages**, because the repository already exists and it costs nothing.

1. Push the repository to GitHub as a **public** repo. (Pages on a private repo needs a paid plan.)
2. On GitHub: **Settings → Pages**.
3. Under *Build and deployment*, set **Source: Deploy from a branch**, **Branch: `main`**, **Folder: `/docs`**.
4. Save. After a minute the policy is live at:

   ```
   https://<your-github-username>.github.io/<repository-name>/privacy-policy.html
   ```

5. Open that URL in a private/incognito window to confirm it loads with no sign-in.
6. Paste it into **Play Console → Policy → App content → Privacy policy**.

Serving `/docs` also publishes `technical-solution.html` at the same host. That is harmless — it contains no
credentials — but be aware it becomes public.

**Alternatives** if you would rather not use GitHub Pages: Firebase Hosting (free tier, you already use Google
infrastructure), Netlify, or Cloudflare Pages. All serve a static HTML file at a stable URL. Avoid anything that
expires or requires an account to view.

## 2. Before publishing — required edits

| Item | Where | Status |
| --- | --- | --- |
| Contact email in the policy | `privacy-policy.html` §10 | **Done** — `habibi.ilyas@gmail.com`. Must stay monitored; Play requires a working contact route, and it should match the developer contact on the Play listing. |
| Effective / last-updated date | `privacy-policy.html` header and footer | Set to 2 August 2026 — update if you publish later |
| Policy URL | Play Console → App content | Add after hosting is live |

## 3. Data safety form answers

Play cross-checks the Data safety declaration against the privacy policy; a mismatch is a common rejection
reason. **The app ships Firebase Analytics and Crashlytics**, so this section declares collection. It is a
positive declaration, not an absence — deleting a line does not answer the form.

Based on the code as it stands:

| Question | Answer | Why |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** | Firebase Analytics and Crashlytics are bound in `AppModule`, and Performance Monitoring reports on its own, whenever a `google-services.json` is present. |
| **App activity** — other actions | **Collected, not shared.** Not linked to identity. Purpose: Analytics | `AnalyticsEvent` sends `screen_view`, `movie_opened`, `outbound_link`, `experiment_exposed`. No search terms, no favourites. |
| **App info and performance** — crash logs | **Collected, not shared.** Not linked to identity. Purpose: Analytics, App functionality | Crashlytics, plus the event breadcrumbs preceding a crash. |
| **App info and performance** — diagnostics | **Collected, not shared.** Not linked to identity. Purpose: Analytics, App functionality | Performance Monitoring: start-up time, frame rendering, and automatic traces of the app's own HTTPS calls to TMDB — URL, latency, payload size, response code. No request bodies. |
| **Device or other IDs** | **Collected, not shared.** Not linked to identity. Purpose: Analytics | Firebase generates an installation ID. It identifies an install, not a person. Performance Monitoring adds carrier and radio type to the device picture. |
| Is all user data encrypted in transit? | **Yes** | HTTPS to TMDB; Firebase SDKs use TLS. |
| Do you provide a way for users to request data deletion? | No in-app route; uninstall stops collection | There is no account to delete. In-app *Settings → Clear cache* and per-item favourite removal clear local data. Firebase data is deleted per Google's retention policy. |
| Data collected for advertising or personalisation | **None** | No ad SDK, no attribution SDK, no advertising ID. |
| Does the app contain ads? | **No** | |
| Target audience | Not directed at children | See §7 of the policy — TMDB content may include adult titles. |

**"Not linked to identity" is the load-bearing claim.** It holds only because the app has no sign-in and never
calls `setUserId` with a real identity. If an account system is ever added, every row above changes.

The privacy policy's §4 was written to match this table exactly. Change one, change both, in the same commit.

## 3a. Firebase setup — required before the data above is true

`google-services.json` is **not** in the repository: it identifies one Firebase project and is gitignored
alongside the TMDB credentials. To configure it:

1. Firebase console → create a project (or open the existing one).
2. Add an Android app with package name **`com.bigon.sinema`**.
3. Download `google-services.json` and put it in **`sinema/`**.
4. Rebuild.

Without that file the build still succeeds and prints:

```
google-services.json not found in :sinema — building without Firebase. Analytics, Crashlytics and Performance will be inert.
```

`BuildConfig.FIREBASE_ENABLED` is then false, the sinks are never bound, and nothing is collected. Performance
Monitoring is not gated by that flag because it is not bound through the graph at all — its Gradle plugin is what
instruments the app, so it is absent from the build entirely rather than switched off inside it. **A release
built without the file reports nothing and says so only in the Gradle log** — check for that line before shipping,
because the Data safety declaration above claims collection that would not be happening.

Crashlytics needs one more thing to be useful: R8 is on, so stack traces arrive obfuscated unless the mapping file
is uploaded. The `com.google.firebase.crashlytics` plugin does that automatically for minified builds — the
`injectCrashlyticsMappingFileId` task in the build log is the sign it is wired up.

Remote Config remains unimplemented; `StaticFeatureFlagDataSource` is still the bound `FeatureFlagDataSource`.

## 4. Other Play requirements worth checking

- **App content declarations:** ads (no), target audience, content rating questionnaire, news app (no),
  data safety, government app (no), financial features (none).
- **TMDB attribution** is in the app (Settings) and satisfies §3 of the API Terms of Use: their mark, shown less
  prominently than Sinema's own branding, plus the mandated notice. The notice is quoted verbatim in
  `TMDB_ATTRIBUTION` (`AttributionFooter.kt`) — *"This product uses TMDB and the TMDB APIs but is not endorsed,
  certified, or otherwise approved by TMDB."* It reads redundantly and ends on three verbs; both are required.
  Earlier versions of this file claimed a paraphrase was verbatim. It was not — check the string against
  <https://www.themoviedb.org/api-terms-of-use>, not against this note.

- **The TMDB mark** is their official *Alt short* SVG, converted to a gradient `VectorDrawable` and shipped
  unmodified — no tint, no recolour. It previously shipped as a black silhouette tinted to the caption colour, which
  is the kind of change their terms make risky; there is no monochrome official variant to justify it. If the mark
  ever needs to sit on a busy background, change the background, not the logo.

- **TMDB's six-month cache limit (§1.C)** is enforced by `TmdbCachePolicy`. Detail payloads expire on read and are
  swept on write; favourite snapshots are refreshed in place at launch by `RefreshStaleFavoritesUseCase` rather
  than deleted, because a favourite is the user's record and only the TMDB data cached alongside it expires.
  A device offline past the limit keeps showing its favourites and retries each launch — see the note below.
- **Release signing:** `assembleRelease` succeeds (R8 minification, resource shrinking and `lintVitalRelease` all
  pass; the minified build has been smoke-tested on device) but produces `sinema-release-unsigned.apk` — there is
  no `signingConfigs` block yet. A release keystore is needed, kept out of version control (`.gitignore` already
  covers `*.jks`, `*.keystore` and `signing.properties`) with its passwords out of the build files.
- **`versionCode` / `versionName`:** `2` and `1.0.0`. The two are independent: `versionName` is a string shown to
  readers, while `versionCode` is the integer Play orders uploads by. Play never accepts a `versionCode` twice —
  not even after a release is deleted or rolled back — so **every** upload after this one must increment it,
  including a re-upload of a build rejected at review. `1.0.0` can stay put while `versionCode` climbs.
