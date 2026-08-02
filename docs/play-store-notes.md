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
reason. Based on the code as it stands:

| Question | Answer | Why |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **No** | No personal data is gathered or transmitted. The only bound analytics sink is `TimberAnalyticsSink`, which writes to logcat, and Timber's tree is planted only when `BuildConfig.DEBUG` — so in a release build the events go nowhere. |
| Is all user data encrypted in transit? | **Yes** | All requests are HTTPS to TMDB. |
| Do you provide a way for users to request data deletion? | Not applicable — but describe local deletion | There is no server-side data. In-app *Settings → Clear cache*, per-item favourite removal, and uninstall remove everything. |
| Data collected for analytics / advertising / personalisation | **None** | No analytics backend, no ad SDK, no attribution SDK, no crash reporting SDK is present. |
| Does the app contain ads? | **No** | |
| Target audience | Not directed at children | See §6 of the policy — TMDB content may include adult titles. |

**This changes the moment a backend analytics sink is added.** §9·05 of the technical solution plans a Firebase
analytics and Remote Config adapter. When that ships:

- the Data safety form must be updated to declare what Firebase collects (typically app interactions, crash
  logs, device identifiers),
- the privacy policy needs a third-party services entry for Firebase,
- both must be updated **before** the release goes out, not after.

## 4. Other Play requirements worth checking

- **App content declarations:** ads (no), target audience, content rating questionnaire, news app (no),
  data safety, government app (no), financial features (none).
- **TMDB attribution** is already in the app (Settings) and satisfies TMDB's requirement to display their mark
  and notice. Their terms also prohibit implying endorsement — the required wording is used verbatim.
- **Release signing:** the app currently builds debug only. A release keystore is needed, and it must be kept
  out of version control (`.gitignore`) with its passwords out of the build files.
- **`versionCode` / `versionName`:** currently `0.0.1`. Play rejects a re-upload of an existing `versionCode`.
