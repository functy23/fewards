# AGENTS.md

Agent instructions for **Fewards** (`com.functy.fewards`). Product surface and user steps live in [README.md](README.md). Versions and coordinates live in `build.gradle.kts` / `gradle/libs.versions.toml`.

## Hard rules

- UI follows **KernelSU manager** source, not Flutter.
- 米游社 logic follows **MiyoQian** (DS salts, act_id, endpoints). Adjust from capture only when MiyoQian is stale.
- WorkBuddy follows the public HTTP contract (`copilot.tencent.com/billing/meter/daily-checkin`: `code` 0 success / 10001 already signed / 401 expired). Adjust from capture only when that contract drifts.
- Treat `autorewards-runner` / Flutter mothers as out of spec. Do not copy their UI or bug-for-bug behavior.
- Old `AGENTS.md` history is environment-only (toolchain, mirrors). Do not take product requirements from it.

## Build

Ship **release + R8**. Debug first-frame composition hitch is expected; do not paper over it with enter delays.

```bash
./gradlew :app:assembleRelease
```

Every user-facing APK bumps `fewardsVersionCode` / `fewardsVersionName` (patch +1) and the filename (`Fewards-<version>-release.apk`).

Toolchain facts: AGP 9.4 / Kotlin 2.4 / miuix 0.9.3 (`-android` artifact suffix) / miuix-nav 0.9.4-rc01 / minSdk 31 / Java 21. Room stays on a version the Aliyun/Google mirrors actually serve. Maven Central is not reachable; use the Aliyun mirror chain in `settings.gradle.kts`.

`miuix-nav` owns `androidx.navigation3.ui`. Do not also depend on `androidx.navigation3:navigation3-ui` (duplicate classes).

## Layout

```
app/src/main/java/com/functy/fewards/
  core/mihoyo/          # DsSign, constants, HTTP, engine
  core/workbuddy/       # check-in engine
  data/repository/      # settings, accounts, config transfer
  ui/screen/            # home, account, settings, colorpalette, about (Miuix)
  ui/viewmodel/
  work/                 # scheduler, notifier, boot, notification actions
```

UI is Miuix only. Navigation is miuix-nav `NavDisplay`, not androidx Nav3 UI.

## Conventions

- Settings children **expand/collapse** with the parent switch (Monet pattern). Do not grey them out.
- Hold-time pickers use **OverlaySpinnerPreference** (list + 确定), not a stack of TextButtons.
- Theme page: WindowSpinnerPreference; do not empty `Scaffold.popupHost`.
- Live task notification stays ongoing + promoted through complete; auto-dismiss follows `overviewAutoDismiss` / `overviewHoldSeconds`.
- Mihoyo already-signed / `can_get_points==0` / 「已签」 is success, not failure.
- Tokens stay in on-device prefs. Logs never print credentials.

## Nav / overlay

`NavDisplayEffects.dimAmount = 0.5` (library scrim between pages). Incoming routes paint an opaque surface on frame 0. Corner clip: `if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius`. Predictive back is the manifest `enableOnBackInvokedCallback`; animation choice is Compose-only and reads from `MainActivityUiState.predictiveBackAnimation`.
