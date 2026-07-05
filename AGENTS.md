# Agents Guidelines

Behavioral guidelines for autonomous and semi-autonomous task execution in Ventus (native Android,
Kotlin/Jetpack Compose weather app).

## 1. Search & Verify First

Don't speculate. Don't hide uncertainty. Use tools to ground claims.

Before proposing a solution:
- Current state (Gradle/Kotlin/Compose dependency versions, Open-Meteo response shape, Play
  Services fused-location behavior): check `app/build.gradle.kts` and actual code, don't assume.
- Problem others have hit (OkHttp/Gson parsing quirks, DataStore, Compose Navigation, fused
  location permission edge cases): search for existing solutions.
- Uncertain about a fact (e.g. an Open-Meteo field name or unit): verify against
  `data/model/WeatherModels.kt` or the live API, don't guess.

Can't verify (tool fails, no results):
- Say so explicitly. Don't pretend certainty.
- Propose what you'd check if you could.
- Ask user to run diagnostic (`./gradlew assembleDebug` output, `adb logcat`) or provide context.

## 2. Tool Chains Over Single Actions

Connect tools into a complete diagnostic or workflow. Don't stop at one search result. Chain:
- Web search for an Open-Meteo/Compose/DataStore issue → fetch full articles → extract steps
- Run `./gradlew testDebugUnitTest` → search for why a test fails → propose fix
- Apply fix → rebuild → verify behavior (unit test or manual run)

State the chain explicitly:
```
1. [Search for] → found: [result]
2. [Fetch details] → learned: [insight]
3. [Run diagnostic] → state is: [finding]
4. [Propose fix] → verify by: [check]
```

## 3. Autonomous Decisions

Act without asking when path is clear. Pause when ambiguous.

Proceed without asking:
- Run `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew installDebug`
- Search for error messages or known issues (OkHttp, Gson, Compose, DataStore, fused location)
- Check config (`app/build.gradle.kts`, `AndroidManifest.xml`)
- Diagnose system state (logcat, permission state)
- Propose fixes based on clear patterns (`StateFlow`/ViewModel wiring, theme token usage, Gson
  model field mapping)

Ask before acting:
- Creating new files outside standard dirs (`ui/`, `data/`, `viewmodel/`)
- Modifying `app/build.gradle.kts`, `AndroidManifest.xml`, or anything with broad impact
- Deleting or overwriting anything
- Changing release/signing config
- Interpreting vague requirements ("make it faster," "improve it")
- Changing the `.toml` theme file format or `AppTheme`/`AppColors` shape in `ui/theme/` — existing
  theme files depend on the current keys

Test: if the user might reasonably disagree with the choice, ask first.

## 4. Error Recovery

Retry intelligently. Escalate clearly when stuck.

When a tool fails:
1. Try once more with adjusted parameters (different search terms, different flags).
2. Still blocked, explain: what did you try, what was the error, what would unblock you?
3. Don't retry the same thing repeatedly.

Facing ambiguity: propose multiple interpretations, ask which matches intent, continue once clear.

## 5. Communication During Execution

Show tool invocations when they failed, produced unexpected output, or the reasoning chain is
non-obvious. Stay quiet on routine diagnostic checks and predictable steps.

Always summarize: what you found, what it means, what comes next (or what you're blocked on).

## 6. Goal Verification

Define "done." Loop until verified.

- "Fix forecast parsing bug" → verify: add/adjust a case in `WeatherResponseMappingTest.kt`
  reproducing it, `./gradlew testDebugUnitTest` passes.
- "Add a new settings option" → verify: `AppPreferences` flow + setter added, `SettingsViewModel`
  exposes it, `SettingsScreen` renders and calls it, `./gradlew assembleDebug` succeeds.
- "Fix location/GPS issue" → verify: `LocationSource`/`FusedLocationSource` behaves correctly,
  tested via `./gradlew installDebug` + manual permission grant/deny + `adb logcat`.
- "Add a new built-in theme" → verify: entry added to `ALL_THEMES` in `AppTheme.kt` with all 7
  colors, renders correctly in the Settings theme grid.

Then loop: make change → run verification → pass: done, fail: diagnose and retry.

## 7. Keep Docs in Sync

- New user-facing feature → add to `FEATURES.md` (user-focused, no code references).
- Removing/changing a feature → update `FEATURES.md` to match.
- New Open-Meteo endpoint/field used, or API integration change → update `API.md`.
- Architecture-level change (new module/package, restructuring) → update `CLAUDE.md`.

## When to Use These Guidelines

- Iterative debugging (Compose recomposition issues, OkHttp/Gson parsing, DataStore, fused-location
  permission flows)
- Multi-step research (Open-Meteo API quirks, Compose Navigation issues)
- Testing and verification workflows (unit tests, manual install/run per CLAUDE.md "Testing")

For one-off questions, quick answers, or clarifications: overkill. Use judgment.
