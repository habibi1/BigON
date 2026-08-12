# :core:update

In-app updates through Play's [in-app update API](https://developer.android.com/guide/playcore/in-app-updates) —
both the **forced** kind (an unskippable screen instead of the app) and the
**optional** kind (a bottom sheet over an app that keeps working).

Self-contained and app-agnostic: it names the host app from the package
manager, styles itself from the host's `MaterialTheme`, and brings its own DI
wiring. Adopting it costs one dependency and one wrapper.

## Integration

```kotlin
// build.gradle.kts
implementation(project(":core:update"))
```

```kotlin
AppTheme {
    UpdateGate {
        MyApp()
    }
}
```

That is the whole integration — both flows. Asking Play on every resume,
resuming an interrupted update, registering the `IntentSender` launchers,
handling back, counting how often someone has said "not now", and drawing both
surfaces all happen inside the gate. There is nothing
for the host `Activity` to hold, register or remember to call — every one of
those would be a chance to integrate this *almost* correctly and ship a build
that cannot be recalled.

Place it inside the theme and outside any scaffold, so a blocked build has no
reachable navigation.

## When it forces, and when it suggests

| `updatePriority` | staleness | result |
|---|---|---|
| 5 | any | **force** — unskippable screen |
| 4 | ≥ 7 days | **force** |
| 4 | < 7 days | suggest |
| 0–3 | any | suggest — bottom sheet, app stays usable |

"Suggest" still respects the nag policy below, so a suggestion does not
necessarily reach the user.

The default is deliberately reluctant. Forcing on every update would mean a
typo fix in a string locks out everyone on the previous build until they sit
through a download, so a release has to *ask* to be treated as urgent.

Three refusals matter more than the numbers:

- **An update Play will not install immediately never blocks.** Sideloads and
  some enterprise installs report an update but disallow the flow; blocking
  there strands the app with no way forward.
- **Unknown staleness counts as fresh.** Play returns `null` until it knows,
  and reading that as "very stale" would force the moment a high-priority
  release appeared — the opposite of a grace period.
- **A check that fails lets the app run.** No network, no Play Store, or a
  Play Store too old to answer are all normal, and none is a reason to stop
  someone using the app.

### Different thresholds

```kotlin
UpdateGate(
    config = UpdateConfig(
        criticalPriority = 4,
        highPriority = 3,
        gracePeriodDays = 14,
    ),
) { MyApp() }
```

`UpdateConfig` validates on construction — a zero threshold would silently turn
"urgent releases only" into "every release", on the whole user base, at the
next routine patch.

## How often it asks

An optional update the user can ignore is one they *will* ignore, so the cost
of asking badly is that the prompt becomes reflex-dismissed — and the one that
matters later gets dismissed with it. `UpdateNagPolicy` is kept apart from
`UpdatePolicy` because they answer different questions: whether the *release*
matters, versus whether this *person* should be asked again.

| | default | meaning |
|---|---|---|
| first ask | immediate | a version never shown is always worth one mention |
| `promptAfterLaunches` | 3 | launches to wait after each "Not now" |
| `maxDismissals` | 3 | refusals before this version goes quiet for good |

A new version resets all of it — declining 1.4 says nothing about 1.5.

Three details that are easy to get wrong, and are each a test:

- **Swiping the sheet away counts as a dismissal.** Otherwise the cheapest way
  to get rid of it is the one the policy never learns from.
- **Cancelling Play's own confirmation counts too**, which is why the flexible
  flow has its own launcher — cancelling a *forced* update changes nothing,
  cancelling an optional one is a "not now".
- **Launches before the first dismissal are not counted**, or a
  frequently-opened app spends its budget before it has asked anything.

## Your own surfaces

Both defaults are drawn from `MaterialTheme`, so they inherit the host's
colours and typography. Either can be replaced:

```kotlin
UpdateGate(
    blockedScreen = { state, onUpdate -> MyBlockingScreen(state, onUpdate) },
    optionalPrompt = { state, accept, dismiss, restart -> MySheet(...) },
) { MyApp() }
```

Note what the blocking screen is *not*. Play's IMMEDIATE flow owns the update itself —
download, progress, install, restart — and this module contains none of that.
The screen exists for the windows Play leaves open: the user backing out of
Play's flow, `startUpdateFlowForResult` failing to start, and the moment
between resume and Play's UI appearing. In the happy path nobody sees it.

The optional flow has a second moment the sheet has to carry: Play downloads a
flexible update but **will not install it**. The app must call `completeUpdate()`
or the update sits on disk forever, so the sheet returns as "Update ready" with
a Restart button once the download lands.

Two behaviours are the gate's own and are not yours to re-implement:

- **The app is not composed while blocked** — not covered, not disabled.
  Drawing an opaque screen on top looks identical and is not the same thing: a
  Compose surface does not consume pointer input unless asked, so taps land on
  whatever is behind it and the app stays in the accessibility tree for
  TalkBack to walk.
- **Back closes the app.** It does not dismiss the gate, and it is not
  swallowed. There is no way forward except updating, and a back press that
  silently does nothing reads as a frozen app.

## Testing

### Automated

```bash
./gradlew :core:update:test
```

45 tests — both policies, the ViewModel, and Compose UI tests asserting that a
blocked build cannot be reached and that an optional prompt leaves the app
usable. The UI tests run under Robolectric rather than on a
device: Espresso 3.7.0 reflects on `InputManager.getInstance`, which API 36+
removed, so instrumented Compose tests cannot run on any platform this project
targets.

### On a device

Play refuses an immediate update against a sideloaded build — it has no
release to compare against and answers `UPDATE_NOT_AVAILABLE` — so the flow is
otherwise unverifiable until it is already in front of users. Google's
`FakeAppUpdateManager` stands in, driven by Gradle properties on **this**
module, so rehearsing costs the host app nothing:

```bash
# forced — blocks immediately
./gradlew :app:installDebug -Pbigon.fakeUpdatePriority=5

# optional — bottom sheet over a working app
./gradlew :app:installDebug -Pbigon.fakeUpdatePriority=1

# priority 4 inside the grace period — should NOT block
./gradlew :app:installDebug -Pbigon.fakeUpdatePriority=4 -Pbigon.fakeUpdateStalenessDays=6

# one day later — blocks
./gradlew :app:installDebug -Pbigon.fakeUpdatePriority=4 -Pbigon.fakeUpdateStalenessDays=7

# back to normal; the flag never sticks between builds
./gradlew :app:installDebug
```

Debug builds only — the release variant has no such field, and the fake is
never constructed without the property.

`FakeAppUpdateManager` records that a flow started but never progresses on its
own: no bytes are downloaded, `DOWNLOADED` never arrives, and the restart
prompt cannot be reached. `FakeUpdateDriver` advances it, so rehearsing an
optional update shows the whole sequence — sheet, download, restart — rather
than only its first frame.

### Against real Play

The fake cannot tell you Play will actually serve the update. For that: publish
a build with a higher `versionCode` to an internal test track, install the
older build on a device signed into an account on that track, and set the new
release's priority with the Play Developer API —

```
edits.tracks.update → releases[].inAppUpdatePriority
```

`inAppUpdatePriority` is **not** exposed in the Play Console UI, which is the
part that usually surprises people. Priority is a property of the release that
supersedes yours, so it cannot be set retroactively and cannot be tested
before publishing.

## Known limits

- **One window remains.** The first launch after a release turns critical still
  composes the app for roughly 300 ms while Play is asked. Subsequent launches
  do not — `UpdateGateStore` remembers the last verdict and starts blocked with
  no window at all. Closing the first one too would charge every launch, for
  every user, for a case almost nobody hits.
- **The Play hand-off itself is not covered by an automated test.** What the
  gate does with the result is; the `IntentSender` round trip needs a device.
- **`PlayUpdateSource`'s cancellation rethrow is verified by inspection**, not
  by a unit test — faking `AppUpdateManager` well enough to test it needs real
  GMS `Task` objects on the JVM.
