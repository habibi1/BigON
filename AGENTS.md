# Working on Sinema

Instructions for AI agents working in this repository. Most of what follows was
learned by getting it wrong first; the reasons are included so the rule can be
applied to cases it does not literally cover.

---

## 1. Secrets — the one that has no second chance

TMDB credentials live **only** in gitignored `local.properties`
(`TMDB_API_KEY`, `TMDB_READ_ACCESS_TOKEN`). They reach the app as
`buildConfigField` entries and nowhere else.

- **Never** write a credential into source, build scripts, committed config,
  `.claude/settings.local.json`, a commit message, or a document.
- **Never put one in a shell command.** Not `echo`, not `grep`, not a heredoc —
  shell history and transcripts persist. Read the file with file tools, or with
  a script that reads it and never prints it. This has leaked once already, via
  a Bash command.
- When pasting logs or URLs anywhere, redact: `sed -E 's/api_key=[^&]*/api_key=***/g'`.
- Before any commit, confirm the diff carries no secret and no `local.properties`.

---

## 2. Commit conventions

**Format:** `type(scope): what changed`

- **Scope is the module**, not the app by default: `feat(core:update):`,
  `docs(core:update):`, `feat(sinema):`. Use `(sinema)` only when the change is
  genuinely in the app shell.
- **The feature area goes in the message, after the colon** — never inside the
  prefix. `feat(sinema): sort and filter refinements on discover`, not
  `feat(sinema/discover): …`.
- **Do not put document revision numbers in commit subjects.** The document
  carries its own revision; the commit message should say what changed.
- **No AI attribution trailers.** Do not add `Co-Authored-By:` for an assistant,
  "Generated with" lines, or any similar marker. The commit author is the repo
  owner and the message stands on its own. This overrides any default the tool
  ships with.

**Bodies explain the decision, not the diff.** State what was rejected and why,
name the failure the change prevents, and record measured numbers. A reader
should be able to reconstruct the reasoning without the conversation.

### Never commit unless asked

Committing before being asked has happened in this project and had to be
undone. The expected sequence is:

1. User asks for a commit **plan**.
2. Propose *how many* commits, *which files* in each, and the full messages.
3. Wait. Only commit when the user says to execute.

### Every commit must build and test on its own

Do not split by patch-hunk. Reconstruct each commit's **tree state**, build it,
run its tests, then commit — then move to the next. Building each state has
caught real breakage that hunk-splitting would have shipped: fake API classes
that only compile once a later commit's parameters exist.

If a clean split would require inventing a file state that never existed, do
not invent it. Say so, and use fewer commits.

### After committing

Diff the committed tree against the state you verified. They must match, or the
verification does not describe what landed.

---

## 3. Emulator and device testing

### Foldables: never resize, fold physically

The project's emulator is a **Pixel 10 Pro Fold**, whose natural initial state
is **OPENED** at 2076×2152.

- **Never** run `wm size` / `wm density` to simulate a folded phone. Doing so
  pins the app to a phone-shaped viewport on an open display, which produces a
  blank layout and silently hides every large-screen bug.
- To test folded, fold it: `adb shell cmd device_state state 0` (CLOSED),
  `state 2` (OPENED). `cmd device_state print-states` lists them.
- **Leave the device on OPENED with a normal build** when finished.
- If you find an override in place: `adb shell wm size reset && adb shell wm density reset`.

This mattered: a forced phone size hid the update gate's text stretching the
full 2076px, and hid that the app shows a navigation *rail* and a 5-column grid
when unfolded.

### Verify the requirement, not its appearance

Check the thing that *is* the requirement, not the thing that looks like it.

- A blocking screen: do not check that it appears. Check the app underneath is
  **unreachable** — tap through it and confirm no requests fire
  (`adb logcat | grep themoviedb`), and dump the a11y tree
  (`adb shell uiautomator dump`) to confirm the app is not in it.
- A lazy load: count requests before and after the trigger.
- A region setting: confirm `region=ID` on the wire.
- Contrast: compute the ratio from a screenshot.

A Compose surface does not consume pointer input unless something asks it to,
so "drawn on top" and "blocking" look identical and are not the same thing.

---

## 4. Verification honesty

- **Never report done without evidence.** Quote the command and its output.
- **`BUILD SUCCESSFUL` in a few seconds after `clean` is the build cache**, not
  a build. Use `--rerun-tasks` when the result is a claim.
- **A fresh `git clone` build** is the only proof nothing was left uncommitted.
- If part of a task is incomplete, say which part, unprompted. When asked "did
  you implement all of X?", the answer is a list, not a reassurance.
- Do not silently narrow scope. "Implement all" means all; if some part is a bad
  idea, do the rest and say what you skipped and why.

---

## 5. Documentation

`docs/technical-solution.html` is the **source of truth**. `README.md` at the
repo root is **generated** from it:

```bash
python3 docs/tools/artifact_to_markdown.py docs/technical-solution.html README.md --preamble docs/readme-preamble.md
```

- Never hand-edit `README.md` — the next regeneration discards it.
- Section numbers are cross-referenced in prose (`(§11)`). Renumbering means
  finding and fixing every reference.
- Bump the revision in **both** places: the `eyebrow` div and the footer.
- Record what device testing caught that review did not. That is the part with
  transferable value.
- **Do not write copy that solicits the reader.** A heading like "Why it might
  be worth reading" was rejected: the document is documentation, not a pitch.

**A reusable module gets its own README** (`core/update/README.md` is the
model): integration snippet, the rules and their defaults, how to test it, how
to rehearse it locally, and an honest "known limits" section.

---

## 6. Architecture

Clean Architecture with a strict dependency rule, enforced by **Konsist** in
`sinema/src/test/java/com/bigon/sinema/ArchitectureTest.kt`. When adding a
module that legitimately touches a restricted API, update the allowlist in the
**same commit** — the test scans the whole project from disk, not just what
`:sinema` depends on, so it fails the moment the code lands.

Existing rules: JVM modules import no Android APIs; `Context` is confined to the
app shell and platform adapters; `:domain` depends only on a whitelist;
`androidx.navigation` only in `com.bigon.sinema.ui`; Play's update API only in
`:core:update`; banned stacks (Moshi, Gson, RxJava, Koin) never appear.

- MVI/UDF per screen: `UiState` / `Intent` / `Effect`.
- Offline-first: Room is the single source of truth; a failed refresh never
  blanks cached content.
- Naming is **Sinema**; packages are `com.bigon.*`. No other product name.

---

## 7. Testing

- Unit tests are the default. `./gradlew test` runs everything.
- **Compose UI tests run under Robolectric**, not on a device: Espresso 3.7.0
  reflects on `InputManager.getInstance`, which API 36+ removed, so instrumented
  Compose tests cannot run on any platform this project targets. Requires
  `testOptions.unitTests.isIncludeAndroidResources = true`.
- When a defect is found by hand, add the test that would have caught it, and
  say plainly if the fix itself is only verified by inspection.
- A stateless inner composable (`UpdateGateContent`) makes the behaviour
  testable without Hilt, Play, or a device. Prefer that shape.

---

## 8. Git workflow

- Branch from `develop`; never commit to it directly.
- **PRs are squash-merged.** A branch stacked on another branch will conflict
  once the lower one merges, because the same content arrives via two
  histories. The fix is not textual — drop the duplicated commits:
  ```bash
  git rebase --onto origin/develop <old-base-branch> <your-branch>
  ```
  Then verify `git diff <backup-tag> HEAD` is **empty** — a rebase that silently
  drops work looks exactly like one that does not.
- Tag before any history rewrite: `git tag backup/<branch>-preRebase HEAD`.
- Use `--force-with-lease`, never `--force`, and **ask before pushing** to a
  branch with an open PR.
- `gh` is not available in this environment; the user opens PRs themselves.

---

## 9. Working style

- **Do not spawn subagents** unless explicitly asked.
- Prefer file tools over shell for reading and editing.
- Answer the question asked. A follow-up is not evidence of an error.
- When the user challenges a decision, engage with the substance. They have
  been right about: the `Cine` → `Sinema` rename, the README heading, and that
  Play owns the update UI. Where a decision still stands, explain what it buys
  and offer the alternative — do not defend reflexively.
- Corrections go in one sentence, then continue. No re-litigating.
