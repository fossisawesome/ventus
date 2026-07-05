# Ventus Weather App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Kotlin/Jetpack Compose Android weather app (Ventus) with a single-location current/hourly/7-day forecast screen and a settings screen, using Firmium's custom (non-Material3) theming system ported wholesale plus a new "Ventus" flagship theme.

**Architecture:** MVVM — `WeatherViewModel` (StateFlow) drives `MainScreen`, `SettingsViewModel` drives `SettingsScreen`, both backed by a manual-DI `VentusApplication` singleton container. Data flows: `LocationSource` → `WeatherRepository` → `WeatherApi` (Open-Meteo, OkHttp+Gson) with a DataStore-backed offline cache. Theming is CompositionLocal-based (`AppColors`/`AppTheme`), no MaterialTheme.

**Tech Stack:** Kotlin, Jetpack Compose, navigation-compose, OkHttp + Gson, DataStore Preferences, Play Services fused location, kotlinx-coroutines, JUnit4 + kotlinx-coroutines-test.

## Global Constraints

- `minSdk 26`, `compileSdk 36`, `targetSdk 36`.
- `namespace` / `applicationId`: `com.fossisawesome.ventus`.
- No `androidx.compose.material3` components anywhere — all custom primitives (`Text`, `IconButton`, `Toggle`, `Divider`, `Spinner`, `TextButton`) live in `ui/components/`. `androidx.compose.material:material-icons-extended` (the `Icons.Default.*` vector set) IS allowed — Firmium uses it purely for icon glyphs, never for Material3 widgets.
- Theming: 7-token color model (`bg, surface, surface2, text, muted, accent, error`, computed `border`), 19 built-in themes (18 ported verbatim from Firmium + new "Ventus" theme), `DEFAULT_THEME_ID = "ventus"`, `.toml` import mechanism cross-compatible with Firmium's theme files.
- Networking: Open-Meteo forecast + geocoding APIs, no API key. OkHttp `execute()` inside `withContext(Dispatchers.IO)` (matches Firmium's `ApiClient` style), Gson for parsing.
- Units: auto-by-locale with manual override (`"auto"|"metric"|"imperial"`), stored in DataStore.
- No instrumented/UI tests for v1 (matches spec) — unit tests only, for pure logic and orchestration classes behind fakes.

---

## File Structure

```
ventus/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradle/wrapper/gradle-wrapper.properties (+ gradlew, gradlew.bat, gradle-wrapper.jar copied from Firmium)
  app/
    build.gradle.kts
    proguard-rules.pro
    src/main/
      AndroidManifest.xml
      java/com/fossisawesome/ventus/
        VentusApplication.kt
        MainActivity.kt
        ui/
          theme/
            AppColors.kt        - AppColors data class + CompositionLocals
            AppTheme.kt         - AppTheme data class, ALL_THEMES, VentusTheme() composable
            ThemeImport.kt      - .toml parser/import/delete (ported)
            AppFont.kt          - AppFontKey enum, font list, font-family resolution
          components/
            Widgets.kt          - Text, IconButton, Toggle, Divider, Spinner, TextButton
          navigation/
            AppNavGraph.kt      - "main" / "settings" routes
          screens/
            MainScreen.kt
            SettingsScreen.kt
        data/
          UnitConversions.kt    - pure conversion + weather-code-to-description/icon functions
          storage/
            AppPreferences.kt  - DataStore: theme id, font, units mode, location, weather cache
          model/
            WeatherModels.kt   - Gson response models + domain WeatherSnapshot/WeatherUiState
          api/
            WeatherApi.kt      - interface + OpenMeteoWeatherApi
            GeocodingApi.kt    - interface + OpenMeteoGeocodingApi
          repository/
            WeatherRepository.kt
          location/
            LocationSource.kt  - interface + FusedLocationSource
        viewmodel/
          WeatherViewModel.kt
          SettingsViewModel.kt
      res/
        values/themes.xml
        values/colors.xml
        values/strings.xml
        font/ (copied .ttf files from Firmium)
        mipmap-anydpi-v26/ic_launcher.xml
        drawable/ic_launcher_background.xml
        drawable/ic_launcher_foreground.xml
    src/test/java/com/fossisawesome/ventus/
      ui/theme/ThemeImportTest.kt
      data/UnitConversionsTest.kt
      data/api/WeatherResponseMappingTest.kt
      data/repository/WeatherRepositoryTest.kt
      viewmodel/WeatherViewModelTest.kt
      viewmodel/SettingsViewModelTest.kt
```

---

### Task 1: Project scaffold — buildable empty shell

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/java/com/fossisawesome/ventus/VentusApplication.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/MainActivity.kt`

**Interfaces:**
- Produces: `VentusApplication` (empty `Application` subclass for now — later tasks add lazy singletons), `MainActivity` (shows a plain placeholder Compose screen).

- [ ] **Step 1: Copy the Gradle wrapper from Firmium**

Both projects are local, same author, same toolchain — reuse the wrapper instead of regenerating it.

```bash
mkdir -p gradle/wrapper
cp /home/larp/firmium-app/firmium/android/gradlew ./gradlew
cp /home/larp/firmium-app/firmium/android/gradlew.bat ./gradlew.bat
cp /home/larp/firmium-app/firmium/android/gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
cp /home/larp/firmium-app/firmium/android/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
```

- [ ] **Step 2: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ventus"
include(":app")
```

- [ ] **Step 3: Write root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
```

- [ ] **Step 4: Write `gradle.properties`**

```properties
android.useAndroidX=true
android.enableJetifier=false
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.configuration-cache=true
kotlin.code.style=official
android.suppressUnsupportedCompileSdk=36
```

- [ ] **Step 5: Write `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fossisawesome.ventus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fossisawesome.ventus"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true; buildConfig = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.google.code.gson:gson:2.14.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
```

- [ ] **Step 6: Write `app/proguard-rules.pro`**

```
# Gson uses reflection on model classes — keep field names intact.
-keep class com.fossisawesome.ventus.data.model.** { *; }
```

- [ ] **Step 7: Write `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".VentusApplication"
        android:label="Ventus"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Ventus"
        android:allowBackup="false"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 8: Write `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base theme required by the manifest; actual theming is handled by Compose. -->
    <style name="Theme.Ventus" parent="android:Theme.Material.NoActionBar" />
</resources>
```

- [ ] **Step 9: Write `app/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#0d1420</color>
</resources>
```

- [ ] **Step 10: Write `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Ventus</string>
</resources>
```

- [ ] **Step 11: Write launcher icon resources**

`app/src/main/res/drawable/ic_launcher_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/ic_launcher_background" />
</shape>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml` (simple wind-swirl glyph):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#5ec8f0"
        android:pathData="M30,40 H70 A12,12 0 1 0 58,28" />
    <path
        android:fillColor="#5ec8f0"
        android:pathData="M24,54 H78 A13,13 0 1 1 65,67" />
    <path
        android:fillColor="#5ec8f0"
        android:pathData="M34,70 H62 A9,9 0 1 1 53,79" />
</vector>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`: identical content to `ic_launcher.xml` above.

- [ ] **Step 12: Write `VentusApplication.kt`**

```kotlin
package com.fossisawesome.ventus

import android.app.Application

// Manual DI container — holds app-wide singletons shared across ViewModels.
// Later tasks add lazy properties here (prefs, weather API, repository, location source).
class VentusApplication : Application()
```

- [ ] **Step 13: Write `MainActivity.kt` (placeholder content)**

Uses `BasicText` (not Material3 `Text`) so the "no Material3" constraint holds from this first
commit onward — Task 3 swaps this for the real themed `VentusTheme` + custom `Text` primitive.

```kotlin
package com.fossisawesome.ventus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText("Ventus")
            }
        }
    }
}
```

- [ ] **Step 14: Build the shell**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 15: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/ app/
git commit -m "Scaffold Ventus Android project shell"
```

---

### Task 2: Theme system — colors, built-in themes, `.toml` import, fonts

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/theme/AppColors.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/theme/AppTheme.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/theme/ThemeImport.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/theme/AppFont.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/ui/theme/ThemeImportTest.kt`
- Copy font files from Firmium into `app/src/main/res/font/`

**Interfaces:**
- Produces: `AppColors` (data class: `bg, surface, surface2, text, muted, accent, error: Color`, computed `border`), `LocalAppColors`, `LocalAppIsDark`, `LocalAppFontFamily` (CompositionLocals), `AppTheme` (data class: `id, name: String, isDark: Boolean, bg..error: Color, isImported: Boolean = false, sourceFile: String? = null`), `ALL_THEMES: List<AppTheme>`, `DEFAULT_THEME_ID = "ventus"`, `themeById(id: String): AppTheme`, `VentusTheme(themeId: String, fontFamily: String, content: @Composable () -> Unit)`, `loadImportedThemes(context): List<AppTheme>`, `allThemes(context): List<AppTheme>`, `importThemeFromUri(context, uri): Result<Unit>`, `deleteImportedTheme(context, filename: String)`, `AppFontKey` enum, `FONT_OPTIONS: List<String>`, `fontKeyFor(displayName: String): AppFontKey`.

- [ ] **Step 1: Copy bundled fonts from Firmium**

```bash
mkdir -p app/src/main/res/font
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/inter.ttf app/src/main/res/font/
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/liberation_mono.ttf app/src/main/res/font/
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/cousine.ttf app/src/main/res/font/
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/firacode.ttf app/src/main/res/font/
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/hack.ttf app/src/main/res/font/
cp /home/larp/firmium-app/firmium/android/app/src/main/res/font/bigblue_terminal_plus.ttf app/src/main/res/font/
```

- [ ] **Step 2: Write `AppColors.kt`**

```kotlin
package com.fossisawesome.ventus.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Typed color tokens exposed to every composable via CompositionLocal — no MaterialTheme.
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val error: Color,
) {
    val border: Color get() = surface2.copy(alpha = 0.4f)
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No VentusTheme provided")
}

val LocalAppIsDark = staticCompositionLocalOf { true }

val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

fun AppTheme.toAppColors() = AppColors(
    bg = bg, surface = surface, surface2 = surface2,
    text = text, muted = muted, accent = accent, error = error,
)
```

- [ ] **Step 3: Write `AppTheme.kt`**

```kotlin
package com.fossisawesome.ventus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.fossisawesome.ventus.R

// One entry per theme — id matches the key stored in AppPreferences.
data class AppTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val error: Color,
    // True for user-imported themes (deletable, unlike built-ins).
    val isImported: Boolean = false,
    // Filename under filesDir/themes/ for imported themes; null for built-ins.
    val sourceFile: String? = null,
)

// Pure-Kotlin hex parser (no android.graphics.Color) so this is usable from plain JUnit tests
// without Robolectric — ALL_THEMES is built by calling this at class-init time.
internal fun hex(s: String): Color {
    val v = s.trimStart('#')
    val rgb = v.toLong(16)
    val argb = if (v.length == 6) 0xFF000000L or rgb else rgb
    return Color(argb)
}

// 18 themes ported verbatim from Firmium, plus the new "Ventus" flagship theme.
val ALL_THEMES: List<AppTheme> = listOf(
    AppTheme("ventus", "Ventus", true,
        bg = hex("0d1420"), surface = hex("16202e"), surface2 = hex("1f2c3d"),
        text = hex("e8f0f7"), muted = hex("7891a8"), accent = hex("5ec8f0"), error = hex("f2665a")),
    AppTheme("firmium", "Firmium", true,
        bg = hex("0f0f0f"), surface = hex("1a1a1a"), surface2 = hex("242424"),
        text = hex("f0f0f0"), muted = hex("888888"), accent = hex("e8c97e"), error = hex("e06060")),
    AppTheme("dracula", "Dracula", true,
        bg = hex("282a36"), surface = hex("343746"), surface2 = hex("44475a"),
        text = hex("f8f8f2"), muted = hex("6272a4"), accent = hex("bd93f9"), error = hex("ff5555")),
    AppTheme("tokyo-night", "Tokyo Night", true,
        bg = hex("1a1b26"), surface = hex("2b2d3a"), surface2 = hex("3c3f50"),
        text = hex("c0caf5"), muted = hex("7aa2f7"), accent = hex("9ece6a"), error = hex("f7768e")),
    AppTheme("catppuccin-mocha", "Catppuccin Mocha", true,
        bg = hex("1e1e2e"), surface = hex("313244"), surface2 = hex("45475a"),
        text = hex("cdd6f4"), muted = hex("a6adc8"), accent = hex("a6e3a1"), error = hex("f38ba8")),
    AppTheme("catppuccin-frappe", "Catppuccin Frappé", true,
        bg = hex("303446"), surface = hex("414559"), surface2 = hex("51576d"),
        text = hex("c6d0f5"), muted = hex("a5adce"), accent = hex("a6d189"), error = hex("e78284")),
    AppTheme("catppuccin-macchiato", "Catppuccin Macchiato", true,
        bg = hex("24273a"), surface = hex("363a4f"), surface2 = hex("494d64"),
        text = hex("cad3f5"), muted = hex("a5adcb"), accent = hex("a6da95"), error = hex("ed8796")),
    AppTheme("gruvbox", "Gruvbox", true,
        bg = hex("282828"), surface = hex("3c3836"), surface2 = hex("504945"),
        text = hex("ebdbb2"), muted = hex("928374"), accent = hex("b8bb26"), error = hex("fb4934")),
    AppTheme("nord", "Nord", true,
        bg = hex("2e3440"), surface = hex("3b4252"), surface2 = hex("434c5e"),
        text = hex("eceff4"), muted = hex("81a1c1"), accent = hex("a3be8c"), error = hex("bf616a")),
    AppTheme("synthwave", "Synthwave '84", true,
        bg = hex("262335"), surface = hex("2a2139"), surface2 = hex("34294f"),
        text = hex("ffffff"), muted = hex("848bbd"), accent = hex("ff7edb"), error = hex("fe4450")),
    AppTheme("ayu", "Ayu Dark", true,
        bg = hex("0d1017"), surface = hex("131721"), surface2 = hex("1c2333"),
        text = hex("bfbdb6"), muted = hex("5c6773"), accent = hex("ffb454"), error = hex("f07178")),
    AppTheme("github-dark", "GitHub Dark", true,
        bg = hex("0d1117"), surface = hex("161b22"), surface2 = hex("21262d"),
        text = hex("e6edf3"), muted = hex("7d8590"), accent = hex("2f81f7"), error = hex("f85149")),
    AppTheme("adwaita-dark", "Adwaita Dark", true,
        bg = hex("242424"), surface = hex("303030"), surface2 = hex("3c3c3c"),
        text = hex("deddda"), muted = hex("9a9996"), accent = hex("3584e4"), error = hex("e01b24")),
    AppTheme("nordfox", "Nordfox", true,
        bg = hex("232831"), surface = hex("2e3440"), surface2 = hex("3b4252"),
        text = hex("cdcecf"), muted = hex("60728a"), accent = hex("81a1c1"), error = hex("bf616a")),
    AppTheme("monokai", "Monokai Classic", true,
        bg = hex("272822"), surface = hex("3e3d32"), surface2 = hex("49483e"),
        text = hex("f8f8f2"), muted = hex("75715e"), accent = hex("a6e22e"), error = hex("f92672")),
    AppTheme("svalbard", "Svalbard", true,
        bg = hex("0b1117"), surface = hex("121d27"), surface2 = hex("1c2c39"),
        text = hex("e8f1f7"), muted = hex("7e9bb0"), accent = hex("6cc8e0"), error = hex("e06c75")),
    // Light themes
    AppTheme("adwaita", "Adwaita", false,
        bg = hex("fafafa"), surface = hex("ffffff"), surface2 = hex("f0f0f0"),
        text = hex("2e3436"), muted = hex("8e9399"), accent = hex("3584e4"), error = hex("e01b24")),
    AppTheme("catppuccin-latte", "Catppuccin Latte", false,
        bg = hex("eff1f5"), surface = hex("ffffff"), surface2 = hex("e6e9ef"),
        text = hex("4c4f69"), muted = hex("8c8fa1"), accent = hex("40a02b"), error = hex("d20f39")),
    AppTheme("ayu-light", "Ayu Light", false,
        bg = hex("fafafa"), surface = hex("ffffff"), surface2 = hex("f0f0f0"),
        text = hex("5c6166"), muted = hex("abb0b6"), accent = hex("ff9940"), error = hex("f51818")),
)

val DEFAULT_THEME_ID = "ventus"

fun themeById(id: String): AppTheme =
    ALL_THEMES.find { it.id == id } ?: ALL_THEMES.first()

fun AppFontKey.toFontFamily(): FontFamily = when (this) {
    AppFontKey.INTER -> FontFamily(Font(R.font.inter))
    AppFontKey.LIBERATION_MONO -> FontFamily(Font(R.font.liberation_mono))
    AppFontKey.MONOSPACE -> FontFamily.Monospace
    AppFontKey.SANS_SERIF -> FontFamily.SansSerif
    AppFontKey.BIGBLUE_TERMINAL -> FontFamily(Font(R.font.bigblue_terminal_plus))
    AppFontKey.COUSINE -> FontFamily(Font(R.font.cousine))
    AppFontKey.FIRACODE -> FontFamily(Font(R.font.firacode))
    AppFontKey.HACK -> FontFamily(Font(R.font.hack))
    AppFontKey.DEFAULT -> FontFamily.Default
}

// Provides AppColors and isDark flag to the entire composable tree via CompositionLocals.
// No MaterialTheme — all color tokens are accessed via LocalAppColors.current.
@Composable
fun VentusTheme(
    themeId: String = DEFAULT_THEME_ID,
    fontFamily: String = "Liberation Mono",
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = remember(themeId) {
        allThemes(context).find { it.id == themeId } ?: ALL_THEMES.first()
    }
    CompositionLocalProvider(
        LocalAppColors provides remember(theme) { theme.toAppColors() },
        LocalAppIsDark provides theme.isDark,
        LocalAppFontFamily provides remember(fontFamily) { fontKeyFor(fontFamily).toFontFamily() },
        content = content,
    )
}
```

- [ ] **Step 4: Write `AppFont.kt`**

```kotlin
package com.fossisawesome.ventus.ui.theme

// Maps the user-facing font display names (Settings > Appearance) to a key.
enum class AppFontKey {
    INTER, LIBERATION_MONO, MONOSPACE, DEFAULT, SANS_SERIF,
    BIGBLUE_TERMINAL, COUSINE, FIRACODE, HACK,
}

val FONT_OPTIONS: List<String> = listOf(
    "Inter", "Liberation Mono", "Monospace", "System",
    "Sans Serif", "BigBlue Terminal", "Cousine", "FiraCode", "Hack",
)

fun fontKeyFor(displayName: String): AppFontKey = when (displayName) {
    "Inter" -> AppFontKey.INTER
    "Liberation Mono" -> AppFontKey.LIBERATION_MONO
    "Monospace" -> AppFontKey.MONOSPACE
    "Sans Serif" -> AppFontKey.SANS_SERIF
    "BigBlue Terminal" -> AppFontKey.BIGBLUE_TERMINAL
    "Cousine" -> AppFontKey.COUSINE
    "FiraCode" -> AppFontKey.FIRACODE
    "Hack" -> AppFontKey.HACK
    else -> AppFontKey.DEFAULT // "System" and any unrecognized value
}
```

- [ ] **Step 5: Write `ThemeImport.kt`** (ported verbatim from Firmium, package renamed)

```kotlin
package com.fossisawesome.ventus.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.io.File

// Imported themes live as one .toml per theme under filesDir/themes/. The format
// matches the desktop/Firmium theme files (name, color_scheme, [colors] table), so a
// file authored for either app imports unchanged here.

private const val THEMES_DIR = "themes"
private const val MAX_THEME_BYTES = 50 * 1024

internal data class ParsedTheme(
    val name: String,
    val colorScheme: String?,
    val colors: Map<String, String>,
)

private fun themesDir(context: Context): File = File(context.filesDir, THEMES_DIR)

private fun parseThemeToml(text: String): ParsedTheme? {
    var name: String? = null
    var colorScheme: String? = null
    val colors = mutableMapOf<String, String>()
    var inColors = false

    for (raw in text.lines()) {
        // Strip a trailing "# comment", but only when the '#' falls outside a quoted value —
        // hex colors like "#101010" contain '#' themselves and must not be truncated.
        val withoutComment = if (raw.contains('"')) {
            val lastQuote = raw.lastIndexOf('"')
            val hashAfterQuote = raw.indexOf('#', lastQuote)
            if (hashAfterQuote >= 0) raw.substring(0, hashAfterQuote) else raw
        } else {
            raw.substringBefore('#')
        }
        val line = withoutComment.trim()
        if (line.isEmpty()) continue

        if (line.startsWith("[") && line.endsWith("]")) {
            inColors = line.substring(1, line.length - 1).trim() == "colors"
            continue
        }

        val eq = line.indexOf('=')
        if (eq <= 0) continue
        val key = line.substring(0, eq).trim()
        val value = line.substring(eq + 1).trim().trim('"')

        if (inColors) {
            colors[key] = value
        } else when (key) {
            "name" -> name = value
            "color_scheme" -> colorScheme = value
        }
    }

    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return null
    return ParsedTheme(n, colorScheme, colors)
}

private fun toAppTheme(id: String, parsed: ParsedTheme, sourceFile: String): AppTheme? {
    fun color(key: String): Color? = parsed.colors[key]?.let {
        runCatching { hex(it) }.getOrNull()
    }
    return AppTheme(
        id = id,
        name = parsed.name,
        isDark = parsed.colorScheme != "light",
        bg = color("bg") ?: return null,
        surface = color("surface") ?: return null,
        surface2 = color("surface2") ?: return null,
        text = color("text") ?: return null,
        muted = color("muted") ?: return null,
        accent = color("accent") ?: return null,
        error = color("error") ?: return null,
        isImported = true,
        sourceFile = sourceFile,
    )
}

fun loadImportedThemes(context: Context): List<AppTheme> {
    val dir = themesDir(context)
    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".toml") } ?: return emptyList()
    return files.mapNotNull { file ->
        val content = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
        val parsed = parseThemeToml(content) ?: return@mapNotNull null
        toAppTheme(id = file.nameWithoutExtension, parsed = parsed, sourceFile = file.name)
    }.sortedBy { it.name.lowercase() }
}

fun allThemes(context: Context): List<AppTheme> = ALL_THEMES + loadImportedThemes(context)

private fun sanitizeName(name: String): String {
    val cleaned = name.lowercase().map { c -> if (c.isLetterOrDigit()) c else '-' }.joinToString("")
        .trim('-').replace(Regex("-+"), "-")
    return cleaned.ifEmpty { "theme" }
}

fun importThemeFromUri(context: Context, uri: Uri): Result<Unit> {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return Result.failure(Exception("Couldn't read the selected file"))

    if (bytes.size > MAX_THEME_BYTES) {
        return Result.failure(Exception("File is too large (max 50 KB)"))
    }

    val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
        ?: return Result.failure(Exception("File isn't valid text"))
    val parsed = parseThemeToml(text)
        ?: return Result.failure(Exception("Not a valid theme file (missing name or colors)"))
    if (toAppTheme(sanitizeName(parsed.name), parsed, "tmp") == null) {
        return Result.failure(Exception("Theme is missing one or more required colors"))
    }

    val dir = themesDir(context)
    if (!dir.exists() && !dir.mkdirs()) {
        return Result.failure(Exception("Couldn't create the themes folder"))
    }
    val target = File(dir, "${sanitizeName(parsed.name)}.toml")
    return runCatching { target.writeText(text); Unit }
        .recoverCatching { throw Exception("Couldn't save the theme") }
}

fun deleteImportedTheme(context: Context, filename: String) {
    runCatching { File(themesDir(context), filename).delete() }
}

// Exposed for the unit test (Robolectric-free — tests the pure parser only).
internal fun parseThemeTomlForTest(text: String) = parseThemeToml(text)
```

- [ ] **Step 6: Write the parser unit test**

```kotlin
package com.fossisawesome.ventus.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeImportTest {

    @Test
    fun `parses a well-formed theme file`() {
        val toml = """
            name = "Custom Storm"
            color_scheme = "dark"

            [colors]
            bg = "#101010"
            surface = "#202020"
            surface2 = "#303030"
            text = "#f0f0f0"
            muted = "#888888"
            accent = "#66ccff"
            error = "#ff5555"
        """.trimIndent()

        val parsed = parseThemeTomlForTest(toml)
        assertEquals("Custom Storm", parsed?.name)
        assertEquals("dark", parsed?.colorScheme)
        assertEquals("#101010", parsed?.colors?.get("bg"))
        assertEquals("#66ccff", parsed?.colors?.get("accent"))
    }

    @Test
    fun `returns null when name is missing`() {
        val toml = """
            color_scheme = "dark"
            [colors]
            bg = "#101010"
        """.trimIndent()

        assertNull(parseThemeTomlForTest(toml))
    }

    @Test
    fun `ignores comments and blank lines`() {
        val toml = """
            # this is a comment
            name = "Commented"

            [colors]
            # another comment
            bg = "#111111" # trailing comment
        """.trimIndent()

        val parsed = parseThemeTomlForTest(toml)
        assertEquals("Commented", parsed?.name)
        assertEquals("#111111", parsed?.colors?.get("bg"))
    }
}
```

- [ ] **Step 7: Run the test**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.ui.theme.ThemeImportTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/ui/theme app/src/main/res/font app/src/test/java/com/fossisawesome/ventus/ui/theme
git commit -m "Port Firmium theme system, add Ventus flagship theme"
```

---

### Task 3: UI primitives + wire real theme into MainActivity

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/components/Widgets.kt`
- Modify: `app/src/main/java/com/fossisawesome/ventus/MainActivity.kt`

**Interfaces:**
- Consumes: `LocalAppColors`, `LocalAppFontFamily`, `VentusTheme` (Task 2).
- Produces: `Text(text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textAlign, textDecoration, lineHeight, overflow, softWrap, maxLines, minLines)`, `IconButton(onClick, modifier, enabled, content: @Composable BoxScope.() -> Unit)`, `AppIcon(imageVector, contentDescription, tint, modifier)`, `Toggle(checked, onCheckedChange, modifier)`, `Divider(modifier, color)`, `Spinner(color, modifier, strokeWidth)`, `TextButton(onClick, modifier, content)`.

- [ ] **Step 1: Write `Widgets.kt`** (ported from Firmium's `FirmiumUi.kt`, prefix dropped)

```kotlin
package com.fossisawesome.ventus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.fossisawesome.ventus.ui.theme.LocalAppColors

// Convenience Text composable backed by BasicText — replaces material3 Text() throughout the app.
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDecoration = textDecoration,
            lineHeight = lineHeight,
        ),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

// Renders an ImageVector with a colour tint — replaces material3 Icon().
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

@Composable
private fun rememberPressAnimations(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedAlpha: Float,
    pressedScale: Float,
    label: String,
): Pair<Float, Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedAlpha else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "${label}Press",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = if (isPressed && enabled) {
            tween(durationMillis = 80, easing = LinearEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "${label}Scale",
    )
    return overlayAlpha to scale
}

// Tap-target Box that wraps icon content — replaces material3 IconButton().
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val pressColor = LocalAppColors.current.text
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val (overlayAlpha, scale) = rememberPressAnimations(
        interactionSource = interactionSource,
        enabled = enabled,
        pressedAlpha = 0.12f,
        pressedScale = 0.82f,
        label = "iconBtn",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                ) else Modifier
            )
            .background(pressColor.copy(alpha = overlayAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// 1dp horizontal rule — replaces material3 HorizontalDivider().
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.border,
) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

// Spinning arc — replaces material3 CircularProgressIndicator().
@Composable
fun Spinner(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    Canvas(modifier = modifier) {
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
        )
    }
}

// Minimal toggle switch — used for the units auto/override control.
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val thumbX by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "toggleThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent.copy(alpha = 0.4f) else colors.border,
        animationSpec = tween(150),
        label = "toggleTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.muted,
        animationSpec = tween(150),
        label = "toggleThumbColor",
    )
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .offset(x = (thumbX * 18).dp)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

// Clickable text — replaces material3 TextButton().
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pressColor = LocalAppColors.current.text
    val interactionSource = remember { MutableInteractionSource() }
    val (overlayAlpha, scale) = rememberPressAnimations(
        interactionSource = interactionSource,
        pressedAlpha = 0.08f,
        pressedScale = 0.93f,
        label = "textBtn",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(pressColor.copy(alpha = overlayAlpha), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
```

- [ ] **Step 2: Wire the real theme + primitives into `MainActivity.kt`**

```kotlin
package com.fossisawesome.ventus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fossisawesome.ventus.ui.components.Text
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.VentusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VentusTheme {
                val colors = LocalAppColors.current
                Box(
                    modifier = Modifier.fillMaxSize().background(colors.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Ventus", color = colors.text)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build and confirm the themed placeholder renders**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/ui/components app/src/main/java/com/fossisawesome/ventus/MainActivity.kt
git commit -m "Add themed UI primitives, wire VentusTheme into MainActivity"
```

---

### Task 4: AppPreferences (DataStore)

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/storage/AppPreferences.kt`

**Interfaces:**
- Produces: `AppPreferences(context: Context)` with flows `themeId: Flow<String>` (default `"ventus"`), `fontFamily: Flow<String>` (default `"Liberation Mono"`), `unitsMode: Flow<String>` (default `"auto"`), `locationLat: Flow<Double?>`, `locationLon: Flow<Double?>`, `locationName: Flow<String?>`, `cachedWeatherJson: Flow<String?>`, `cachedWeatherFetchedAt: Flow<Long>` (default `0L`); setters `setThemeId`, `setFontFamily`, `setUnitsMode`, `setLocation(lat: Double, lon: Double, name: String)`, `clearLocation()`, `setCachedWeather(json: String, fetchedAt: Long)`.

- [ ] **Step 1: Write `AppPreferences.kt`**

```kotlin
package com.fossisawesome.ventus.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("ventus_prefs")

// Non-sensitive app preferences stored in DataStore.
class AppPreferences(context: Context) {

    private val store = context.dataStore

    companion object {
        val THEME_ID = stringPreferencesKey("theme_id")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        // "auto" | "metric" | "imperial"
        val UNITS_MODE = stringPreferencesKey("units_mode")
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LON = doublePreferencesKey("location_lon")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val CACHED_WEATHER_FETCHED_AT = longPreferencesKey("cached_weather_fetched_at")
    }

    val themeId: Flow<String> = store.data.map { it[THEME_ID] ?: "ventus" }
    val fontFamily: Flow<String> = store.data.map { it[FONT_FAMILY] ?: "Liberation Mono" }
    val unitsMode: Flow<String> = store.data.map { it[UNITS_MODE] ?: "auto" }
    val locationLat: Flow<Double?> = store.data.map { it[LOCATION_LAT] }
    val locationLon: Flow<Double?> = store.data.map { it[LOCATION_LON] }
    val locationName: Flow<String?> = store.data.map { it[LOCATION_NAME] }
    val cachedWeatherJson: Flow<String?> = store.data.map { it[CACHED_WEATHER_JSON] }
    val cachedWeatherFetchedAt: Flow<Long> = store.data.map { it[CACHED_WEATHER_FETCHED_AT] ?: 0L }

    suspend fun setThemeId(id: String) = store.edit { it[THEME_ID] = id }
    suspend fun setFontFamily(name: String) = store.edit { it[FONT_FAMILY] = name }
    suspend fun setUnitsMode(mode: String) = store.edit { it[UNITS_MODE] = mode }

    suspend fun setLocation(lat: Double, lon: Double, name: String) = store.edit {
        it[LOCATION_LAT] = lat
        it[LOCATION_LON] = lon
        it[LOCATION_NAME] = name
    }

    suspend fun clearLocation() = store.edit {
        it.remove(LOCATION_LAT)
        it.remove(LOCATION_LON)
        it.remove(LOCATION_NAME)
    }

    suspend fun setCachedWeather(json: String, fetchedAt: Long) = store.edit {
        it[CACHED_WEATHER_JSON] = json
        it[CACHED_WEATHER_FETCHED_AT] = fetchedAt
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/storage
git commit -m "Add DataStore-backed AppPreferences"
```

---

### Task 5: Unit conversions + weather code mapping (pure logic, TDD)

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/UnitConversions.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/data/UnitConversionsTest.kt`

**Interfaces:**
- Produces: `enum class Units { METRIC, IMPERIAL }`, `fun resolveUnits(mode: String, countryCode: String): Units`, `fun celsiusToFahrenheit(c: Double): Double`, `fun fahrenheitToCelsius(f: Double): Double`, `fun kmhToMph(kmh: Double): Double`, `fun mmToInches(mm: Double): Double`, `data class WeatherCodeInfo(val description: String, val icon: String)`, `fun weatherCodeInfo(code: Int): WeatherCodeInfo`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.fossisawesome.ventus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionsTest {

    @Test
    fun `celsius to fahrenheit`() {
        assertEquals(32.0, celsiusToFahrenheit(0.0), 0.001)
        assertEquals(212.0, celsiusToFahrenheit(100.0), 0.001)
    }

    @Test
    fun `fahrenheit to celsius`() {
        assertEquals(0.0, fahrenheitToCelsius(32.0), 0.001)
        assertEquals(100.0, fahrenheitToCelsius(212.0), 0.001)
    }

    @Test
    fun `kmh to mph`() {
        assertEquals(62.1371, kmhToMph(100.0), 0.001)
    }

    @Test
    fun `mm to inches`() {
        assertEquals(1.0, mmToInches(25.4), 0.001)
    }

    @Test
    fun `resolves imperial for US`() {
        assertEquals(Units.IMPERIAL, resolveUnits("auto", "US"))
    }

    @Test
    fun `resolves metric for non-US locale`() {
        assertEquals(Units.METRIC, resolveUnits("auto", "DE"))
    }

    @Test
    fun `explicit mode overrides locale`() {
        assertEquals(Units.METRIC, resolveUnits("metric", "US"))
        assertEquals(Units.IMPERIAL, resolveUnits("imperial", "DE"))
    }

    @Test
    fun `known weather codes map to description and icon`() {
        assertEquals(WeatherCodeInfo("Clear sky", "☀️"), weatherCodeInfo(0))
        assertEquals(WeatherCodeInfo("Thunderstorm", "⛈️"), weatherCodeInfo(95))
    }

    @Test
    fun `unknown weather code falls back to a generic description`() {
        val info = weatherCodeInfo(999)
        assertEquals("Unknown", info.description)
        assertEquals("❓", info.icon)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.data.UnitConversionsTest"`
Expected: FAIL (compilation error — `UnitConversions.kt` doesn't exist yet).

- [ ] **Step 3: Write `UnitConversions.kt`**

```kotlin
package com.fossisawesome.ventus.data

enum class Units { METRIC, IMPERIAL }

// Countries that use imperial units for everyday weather reporting.
private val IMPERIAL_COUNTRY_CODES = setOf("US", "LR", "MM")

fun resolveUnits(mode: String, countryCode: String): Units = when (mode) {
    "metric" -> Units.METRIC
    "imperial" -> Units.IMPERIAL
    else -> if (countryCode.uppercase() in IMPERIAL_COUNTRY_CODES) Units.IMPERIAL else Units.METRIC
}

fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0
fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0
fun kmhToMph(kmh: Double): Double = kmh / 1.609344
fun mmToInches(mm: Double): Double = mm / 25.4

data class WeatherCodeInfo(val description: String, val icon: String)

// WMO weather interpretation codes, as used by Open-Meteo.
private val WEATHER_CODES: Map<Int, WeatherCodeInfo> = mapOf(
    0 to WeatherCodeInfo("Clear sky", "☀️"),
    1 to WeatherCodeInfo("Mainly clear", "🌤️"),
    2 to WeatherCodeInfo("Partly cloudy", "⛅"),
    3 to WeatherCodeInfo("Overcast", "☁️"),
    45 to WeatherCodeInfo("Fog", "🌫️"),
    48 to WeatherCodeInfo("Depositing rime fog", "🌫️"),
    51 to WeatherCodeInfo("Light drizzle", "🌦️"),
    53 to WeatherCodeInfo("Moderate drizzle", "🌦️"),
    55 to WeatherCodeInfo("Dense drizzle", "🌧️"),
    56 to WeatherCodeInfo("Light freezing drizzle", "🌧️"),
    57 to WeatherCodeInfo("Dense freezing drizzle", "🌧️"),
    61 to WeatherCodeInfo("Slight rain", "🌧️"),
    63 to WeatherCodeInfo("Moderate rain", "🌧️"),
    65 to WeatherCodeInfo("Heavy rain", "⛈️"),
    66 to WeatherCodeInfo("Light freezing rain", "🌨️"),
    67 to WeatherCodeInfo("Heavy freezing rain", "🌨️"),
    71 to WeatherCodeInfo("Slight snow fall", "🌨️"),
    73 to WeatherCodeInfo("Moderate snow fall", "🌨️"),
    75 to WeatherCodeInfo("Heavy snow fall", "❄️"),
    77 to WeatherCodeInfo("Snow grains", "❄️"),
    80 to WeatherCodeInfo("Slight rain showers", "🌦️"),
    81 to WeatherCodeInfo("Moderate rain showers", "🌧️"),
    82 to WeatherCodeInfo("Violent rain showers", "⛈️"),
    85 to WeatherCodeInfo("Slight snow showers", "🌨️"),
    86 to WeatherCodeInfo("Heavy snow showers", "❄️"),
    95 to WeatherCodeInfo("Thunderstorm", "⛈️"),
    96 to WeatherCodeInfo("Thunderstorm with slight hail", "⛈️"),
    99 to WeatherCodeInfo("Thunderstorm with heavy hail", "⛈️"),
)

fun weatherCodeInfo(code: Int): WeatherCodeInfo =
    WEATHER_CODES[code] ?: WeatherCodeInfo("Unknown", "❓")
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.data.UnitConversionsTest"`
Expected: `BUILD SUCCESSFUL`, 9 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/UnitConversions.kt app/src/test/java/com/fossisawesome/ventus/data/UnitConversionsTest.kt
git commit -m "Add unit conversion and weather-code mapping helpers"
```

---

### Task 6: Data models (Gson response + domain types)

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/model/WeatherModels.kt`

**Interfaces:**
- Consumes: `Units` (Task 5).
- Produces: `OpenMeteoForecastResponse`, `CurrentBlock`, `HourlyBlock`, `DailyBlock` (Gson models), `GeocodingSearchResponse`, `GeocodingResult` (Gson models), `WeatherSnapshot(locationName: String, units: Units, currentTempC: Double, currentApparentTempC: Double, currentHumidity: Int, currentWindKmh: Double, currentWeatherCode: Int, hourly: List<HourlyPoint>, daily: List<DailyPoint>)`, `HourlyPoint(epochSeconds: Long, tempC: Double, precipitationProbability: Int, weatherCode: Int)`, `DailyPoint(epochSeconds: Long, tempMaxC: Double, tempMinC: Double, weatherCode: Int)`, `sealed interface WeatherUiState` with `object Loading`, `object NeedsLocation`, `data class Success(val snapshot: WeatherSnapshot)`, `data class Stale(val snapshot: WeatherSnapshot, val fetchedAt: Long)`, `data class Error(val message: String)`.

Note: all internal domain types (`WeatherSnapshot`, `HourlyPoint`, `DailyPoint`) always store temperature/wind/precip in their **base SI unit** (Celsius, km/h, mm) — the `units` field says which unit the UI should *display* them in, and display-layer conversion happens in the screen composables using Task 5's conversion functions. This keeps the repository/cache format unit-independent regardless of the user's current settings.

- [ ] **Step 1: Write `WeatherModels.kt`**

```kotlin
package com.fossisawesome.ventus.data.model

import com.fossisawesome.ventus.data.Units
import com.google.gson.annotations.SerializedName

// ── Open-Meteo forecast response (raw Gson shape) ──────────────────────────

data class OpenMeteoForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentBlock?,
    val hourly: HourlyBlock?,
    val daily: DailyBlock?,
)

data class CurrentBlock(
    val time: String,
    @SerializedName("temperature_2m") val temperature2m: Double,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double,
)

data class HourlyBlock(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature2m: List<Double>,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
)

data class DailyBlock(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>,
)

// ── Open-Meteo geocoding response ───────────────────────────────────────────

data class GeocodingSearchResponse(
    val results: List<GeocodingResult>?,
)

data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
)

// ── Domain model used by the repository, viewmodel, and UI ─────────────────
// All temperature/wind/precipitation fields are stored in SI units (Celsius, km/h, mm)
// regardless of display `units` — the UI layer converts at render time (see UnitConversions.kt).

data class WeatherSnapshot(
    val locationName: String,
    val units: Units,
    val currentTempC: Double,
    val currentApparentTempC: Double,
    val currentHumidity: Int,
    val currentWindKmh: Double,
    val currentWeatherCode: Int,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
)

data class HourlyPoint(
    val epochSeconds: Long,
    val tempC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
)

data class DailyPoint(
    val epochSeconds: Long,
    val tempMaxC: Double,
    val tempMinC: Double,
    val weatherCode: Int,
)

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object NeedsLocation : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Stale(val snapshot: WeatherSnapshot, val fetchedAt: Long) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/model
git commit -m "Add Open-Meteo response models and WeatherSnapshot/WeatherUiState domain types"
```

---

### Task 7: Open-Meteo API clients + response-mapping unit tests

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/api/WeatherApi.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/data/api/GeocodingApi.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/data/api/WeatherResponseMappingTest.kt`

**Interfaces:**
- Consumes: `OpenMeteoForecastResponse`, `GeocodingSearchResponse`, `GeocodingResult` (Task 6), `Units` (Task 5).
- Produces: `interface WeatherApi { suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse }`, `class OpenMeteoWeatherApi(private val client: OkHttpClient = OkHttpClient()) : WeatherApi`, `interface GeocodingApi { suspend fun search(query: String): List<GeocodingResult> }`, `class OpenMeteoGeocodingApi(private val client: OkHttpClient = OkHttpClient()) : GeocodingApi`, `fun mapForecastResponse(locationName: String, units: Units, response: OpenMeteoForecastResponse): WeatherSnapshot`.

`fetchForecast` always requests metric units from Open-Meteo regardless of the user's display
preference — `WeatherSnapshot` stores SI values unconditionally (see Task 6's note), and the UI
converts for display. Requesting the API's own Fahrenheit/mph conversion here would double-convert
once the UI applies `celsiusToFahrenheit()`/`kmhToMph()` on top of already-imperial values.

- [ ] **Step 1: Write `WeatherApi.kt`**

```kotlin
package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface WeatherApi {
    suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse
}

class OpenMeteoWeatherApi(
    private val client: OkHttpClient = OkHttpClient(),
) : WeatherApi {

    private val gson = Gson()

    // Always requested in metric — WeatherSnapshot stores SI values regardless of the user's
    // display unit preference; conversion for display happens in the UI layer (see
    // UnitConversions.kt). Requesting the API's own unit conversion here would double-convert
    // once the UI applies celsiusToFahrenheit()/kmhToMph() on top.
    override suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl()
                .newBuilder()
                .addQueryParameter("latitude", lat.toString())
                .addQueryParameter("longitude", lon.toString())
                .addQueryParameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                .addQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code")
                .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .addQueryParameter("temperature_unit", "celsius")
                .addQueryParameter("wind_speed_unit", "kmh")
                .addQueryParameter("precipitation_unit", "mm")
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "7")
                .build()

            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) error("Weather request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty response from forecast endpoint")
            gson.fromJson(body, OpenMeteoForecastResponse::class.java)
        }
}

// Converts an Open-Meteo ISO-local-time string (e.g. "2026-07-05T14:00") to epoch seconds,
// interpreting it as UTC (the API's "timezone=auto" values are local to the queried location,
// but for relative ordering/display within a single location this is sufficient).
internal fun isoLocalTimeToEpochSeconds(iso: String): Long =
    LocalDateTime.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        .toEpochSecond(ZoneOffset.UTC)

fun mapForecastResponse(locationName: String, units: Units, response: OpenMeteoForecastResponse): WeatherSnapshot {
    val current = response.current ?: error("Forecast response missing current block")
    val hourly = response.hourly
    val daily = response.daily

    val hourlyPoints = if (hourly != null) {
        hourly.time.indices.map { i ->
            HourlyPoint(
                epochSeconds = isoLocalTimeToEpochSeconds(hourly.time[i]),
                tempC = hourly.temperature2m[i],
                precipitationProbability = hourly.precipitationProbability[i],
                weatherCode = hourly.weatherCode[i],
            )
        }
    } else emptyList()

    val dailyPoints = if (daily != null) {
        daily.time.indices.map { i ->
            DailyPoint(
                epochSeconds = isoLocalTimeToEpochSeconds(daily.time[i] + "T00:00"),
                tempMaxC = daily.temperature2mMax[i],
                tempMinC = daily.temperature2mMin[i],
                weatherCode = daily.weatherCode[i],
            )
        }
    } else emptyList()

    return WeatherSnapshot(
        locationName = locationName,
        units = units,
        currentTempC = current.temperature2m,
        currentApparentTempC = current.apparentTemperature,
        currentHumidity = current.relativeHumidity2m,
        currentWindKmh = current.windSpeed10m,
        currentWeatherCode = current.weatherCode,
        hourly = hourlyPoints,
        daily = dailyPoints,
    )
}
```

- [ ] **Step 2: Write `GeocodingApi.kt`**

```kotlin
package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.model.GeocodingResult
import com.fossisawesome.ventus.data.model.GeocodingSearchResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

interface GeocodingApi {
    suspend fun search(query: String): List<GeocodingResult>
}

class OpenMeteoGeocodingApi(
    private val client: OkHttpClient = OkHttpClient(),
) : GeocodingApi {

    private val gson = Gson()

    override suspend fun search(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "10")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) error("Geocoding request failed with HTTP ${response.code}")
        val body = response.body?.string() ?: error("Empty response from geocoding endpoint")
        gson.fromJson(body, GeocodingSearchResponse::class.java).results ?: emptyList()
    }
}
```

- [ ] **Step 3: Write the response-mapping unit test** (pure function, no network)

```kotlin
package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.CurrentBlock
import com.fossisawesome.ventus.data.model.DailyBlock
import com.fossisawesome.ventus.data.model.HourlyBlock
import com.fossisawesome.ventus.data.model.OpenMeteoForecastResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherResponseMappingTest {

    private val sampleResponse = OpenMeteoForecastResponse(
        latitude = 40.7128,
        longitude = -74.0060,
        current = CurrentBlock(
            time = "2026-07-05T14:00",
            temperature2m = 22.5,
            relativeHumidity2m = 60,
            apparentTemperature = 23.0,
            weatherCode = 2,
            windSpeed10m = 12.3,
        ),
        hourly = HourlyBlock(
            time = listOf("2026-07-05T14:00", "2026-07-05T15:00"),
            temperature2m = listOf(22.5, 23.0),
            precipitationProbability = listOf(10, 15),
            weatherCode = listOf(2, 2),
        ),
        daily = DailyBlock(
            time = listOf("2026-07-05", "2026-07-06"),
            weatherCode = listOf(2, 61),
            temperature2mMax = listOf(25.0, 20.0),
            temperature2mMin = listOf(15.0, 14.0),
        ),
    )

    @Test
    fun `maps current block into snapshot`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals("New York", snapshot.locationName)
        assertEquals(Units.METRIC, snapshot.units)
        assertEquals(22.5, snapshot.currentTempC, 0.001)
        assertEquals(23.0, snapshot.currentApparentTempC, 0.001)
        assertEquals(60, snapshot.currentHumidity)
        assertEquals(12.3, snapshot.currentWindKmh, 0.001)
        assertEquals(2, snapshot.currentWeatherCode)
    }

    @Test
    fun `maps hourly block preserving order`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals(2, snapshot.hourly.size)
        assertEquals(22.5, snapshot.hourly[0].tempC, 0.001)
        assertEquals(23.0, snapshot.hourly[1].tempC, 0.001)
        assertEquals(10, snapshot.hourly[0].precipitationProbability)
    }

    @Test
    fun `maps daily block preserving order`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals(2, snapshot.daily.size)
        assertEquals(25.0, snapshot.daily[0].tempMaxC, 0.001)
        assertEquals(15.0, snapshot.daily[0].tempMinC, 0.001)
        assertEquals(61, snapshot.daily[1].weatherCode)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when current block is missing`() {
        mapForecastResponse("New York", Units.METRIC, sampleResponse.copy(current = null))
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.data.api.WeatherResponseMappingTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/api app/src/test/java/com/fossisawesome/ventus/data/api
git commit -m "Add Open-Meteo forecast/geocoding API clients and response mapping"
```

---

### Task 8: WeatherRepository (fetch/cache/stale/error orchestration)

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/repository/WeatherRepository.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/data/repository/WeatherRepositoryTest.kt`

**Interfaces:**
- Consumes: `WeatherApi` (Task 7), `AppPreferences` (Task 4), `WeatherSnapshot`/`WeatherUiState` (Task 6), `Units` (Task 5).
- Produces: `class WeatherRepository(private val api: WeatherApi, private val prefs: AppPreferences)` with `suspend fun refresh(lat: Double, lon: Double, locationName: String, units: Units): WeatherUiState` and `suspend fun loadCached(): WeatherUiState`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.fossisawesome.ventus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.data.api.WeatherApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// In-memory DataStore fake — avoids needing an Android Context/Robolectric for these tests.
private class FakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

private class FakeWeatherApi(
    private val response: OpenMeteoForecastResponse? = null,
    private val shouldFail: Boolean = false,
) : WeatherApi {
    override suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse {
        if (shouldFail) error("network down")
        return response ?: error("no fixture response configured")
    }
}

private val sampleResponse = OpenMeteoForecastResponse(
    latitude = 40.7128,
    longitude = -74.0060,
    current = CurrentBlock("2026-07-05T14:00", 22.5, 60, 23.0, 2, 12.3),
    hourly = HourlyBlock(listOf("2026-07-05T14:00"), listOf(22.5), listOf(10), listOf(2)),
    daily = DailyBlock(listOf("2026-07-05"), listOf(2), listOf(25.0), listOf(15.0)),
)

class WeatherRepositoryTest {

    // AppPreferences takes a Context only to build its DataStore; we can't easily substitute
    // the fake DataStore through the public constructor, so these tests exercise the repository
    // against a temporary in-memory AppPreferences built via reflection-free duplication: a
    // second, test-only constructor overload. See Step 3 for the corresponding production change.

    @Test
    fun `refresh returns Success and caches on API success`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(response = sampleResponse), prefs)

        val state = repo.refresh(40.7128, -74.0060, "New York", Units.METRIC)

        assertTrue(state is WeatherUiState.Success)
        val snapshot = (state as WeatherUiState.Success).snapshot
        assertEquals("New York", snapshot.locationName)
        assertEquals(22.5, snapshot.currentTempC, 0.001)

        // Cache was written.
        val cachedJson = prefs.cachedWeatherJson.firstOrNull()
        assertTrue(cachedJson != null && cachedJson.contains("New York"))
    }

    @Test
    fun `refresh falls back to cache as Stale on API failure`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val cachedSnapshot = mapForecastResponseForTest("Cached City")
        prefs.setCachedWeather(Gson().toJson(cachedSnapshot), fetchedAt = 1000L)

        val repo = WeatherRepository(FakeWeatherApi(shouldFail = true), prefs)
        val state = repo.refresh(0.0, 0.0, "New Location", Units.METRIC)

        assertTrue(state is WeatherUiState.Stale)
        val stale = state as WeatherUiState.Stale
        assertEquals("Cached City", stale.snapshot.locationName)
        assertEquals(1000L, stale.fetchedAt)
    }

    @Test
    fun `refresh returns Error on API failure with no cache`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(shouldFail = true), prefs)

        val state = repo.refresh(0.0, 0.0, "Nowhere", Units.METRIC)

        assertTrue(state is WeatherUiState.Error)
    }

    @Test
    fun `loadCached returns Stale when a cache entry exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val cachedSnapshot = mapForecastResponseForTest("Cached City")
        prefs.setCachedWeather(Gson().toJson(cachedSnapshot), fetchedAt = 2000L)

        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.Stale)
        assertEquals(2000L, (state as WeatherUiState.Stale).fetchedAt)
    }

    @Test
    fun `loadCached returns NeedsLocation when no cache exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)

        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.NeedsLocation)
    }
}

private fun mapForecastResponseForTest(name: String) =
    com.fossisawesome.ventus.data.api.mapForecastResponse(name, Units.METRIC, sampleResponse)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.data.repository.WeatherRepositoryTest"`
Expected: FAIL — `AppPreferences` has no constructor accepting a `DataStore<Preferences>` directly
yet, and `WeatherRepository` doesn't exist.

- [ ] **Step 3: Add a test-only constructor seam to `AppPreferences`**

Modify `app/src/main/java/com/fossisawesome/ventus/data/storage/AppPreferences.kt` — replace the
class declaration and the `store` property with:

```kotlin
// The primary constructor takes the DataStore directly (rather than only a Context) so unit
// tests can inject an in-memory fake instead of a real, file-backed one, without needing
// Robolectric or an Android Context.
class AppPreferences(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    companion object {
```

(Everything else in the file — the keys, flows, and setters — is unchanged.)

Note: a `constructor(fakeDataStore: DataStore<Preferences>)` secondary constructor alongside the
primary `AppPreferences(store: DataStore<Preferences>)` would be a duplicate-signature compile
error — parameter names don't participate in overload resolution, only types. A single
constructor taking `DataStore<Preferences>` covers both the real and fake cases; tests just call
`AppPreferences(FakeDataStore())` positionally.

- [ ] **Step 4: Write `WeatherRepository.kt`**

```kotlin
package com.fossisawesome.ventus.data.repository

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.api.mapForecastResponse
import com.fossisawesome.ventus.data.model.WeatherSnapshot
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class WeatherRepository(
    private val api: WeatherApi,
    private val prefs: AppPreferences,
) {
    private val gson = Gson()

    suspend fun refresh(lat: Double, lon: Double, locationName: String, units: Units): WeatherUiState {
        return try {
            val response = api.fetchForecast(lat, lon)
            val snapshot = mapForecastResponse(locationName, units, response)
            val now = System.currentTimeMillis()
            prefs.setCachedWeather(gson.toJson(snapshot), now)
            WeatherUiState.Success(snapshot)
        } catch (e: Exception) {
            val cached = readCache()
            if (cached != null) {
                WeatherUiState.Stale(cached.first, cached.second)
            } else {
                WeatherUiState.Error(e.message ?: "Couldn't load weather")
            }
        }
    }

    suspend fun loadCached(): WeatherUiState {
        val cached = readCache() ?: return WeatherUiState.NeedsLocation
        return WeatherUiState.Stale(cached.first, cached.second)
    }

    private suspend fun readCache(): Pair<WeatherSnapshot, Long>? {
        val json = prefs.cachedWeatherJson.first() ?: return null
        val fetchedAt = prefs.cachedWeatherFetchedAt.first()
        val snapshot = try {
            gson.fromJson(json, WeatherSnapshot::class.java)
        } catch (_: Exception) {
            null
        } ?: return null
        return snapshot to fetchedAt
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.data.repository.WeatherRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/storage/AppPreferences.kt app/src/main/java/com/fossisawesome/ventus/data/repository app/src/test/java/com/fossisawesome/ventus/data/repository
git commit -m "Add WeatherRepository with offline-cache fallback"
```

---

### Task 9: Location source (fused location wrapper)

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/data/location/LocationSource.kt`

**Interfaces:**
- Produces: `sealed interface LocationResult { data class Success(val lat: Double, val lon: Double) : LocationResult; data object PermissionDenied : LocationResult; data object Unavailable : LocationResult }`, `interface LocationSource { suspend fun getCurrentLocation(): LocationResult }`, `class FusedLocationSource(private val context: Context) : LocationSource`, `fun hasLocationPermission(context: Context): Boolean`.

No unit test for this task — it's a thin wrapper over `FusedLocationProviderClient`/`ActivityCompat`
that requires a real Android device/emulator to exercise meaningfully; `WeatherViewModel`'s tests
(Task 10) cover the orchestration logic around it using a hand-written fake `LocationSource`.

- [ ] **Step 1: Write `LocationSource.kt`**

```kotlin
package com.fossisawesome.ventus.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(val lat: Double, val lon: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
}

interface LocationSource {
    suspend fun getCurrentLocation(): LocationResult
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

class FusedLocationSource(private val context: Context) : LocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission(context)) return LocationResult.PermissionDenied

        return suspendCancellableCoroutine { cont: CancellableContinuation<LocationResult> ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(LocationResult.Success(location.latitude, location.longitude))
                        } else {
                            cont.resume(LocationResult.Unavailable)
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(LocationResult.Unavailable)
                    }
            } catch (_: SecurityException) {
                cont.resume(LocationResult.PermissionDenied)
            }
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/data/location
git commit -m "Add fused-location LocationSource wrapper"
```

---

### Task 10: WeatherViewModel

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/viewmodel/WeatherViewModel.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/viewmodel/WeatherViewModelTest.kt`

**Interfaces:**
- Consumes: `WeatherRepository` (Task 8), `LocationSource`/`LocationResult` (Task 9), `AppPreferences` (Task 4), `GeocodingApi` (Task 7), `Units`/`resolveUnits` (Task 5).
- Produces: `class WeatherViewModel(private val repository: WeatherRepository, private val locationSource: LocationSource, private val geocodingApi: GeocodingApi, private val prefs: AppPreferences, private val countryCode: String) : ViewModel()` with `val state: StateFlow<WeatherUiState>`, `val searchResults: StateFlow<List<GeocodingResult>>`, `fun loadInitial()`, `fun refresh()`, `fun useCurrentLocation()`, `fun search(query: String)`, `fun selectLocation(result: GeocodingResult)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

private val sampleResponse = OpenMeteoForecastResponse(
    latitude = 40.7128, longitude = -74.0060,
    current = CurrentBlock("2026-07-05T14:00", 22.5, 60, 23.0, 2, 12.3),
    hourly = HourlyBlock(listOf("2026-07-05T14:00"), listOf(22.5), listOf(10), listOf(2)),
    daily = DailyBlock(listOf("2026-07-05"), listOf(2), listOf(25.0), listOf(15.0)),
)

private class FakeWeatherApi : WeatherApi {
    override suspend fun fetchForecast(lat: Double, lon: Double) = sampleResponse
}

private class FakeGeocodingApi(private val results: List<GeocodingResult> = emptyList()) : GeocodingApi {
    override suspend fun search(query: String): List<GeocodingResult> = results
}

private class FakeLocationSource(private val result: LocationResult) : LocationSource {
    override suspend fun getCurrentLocation(): LocationResult = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadInitial with granted GPS fetches weather and reaches Success`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.Success(40.7128, -74.0060)),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is com.fossisawesome.ventus.data.model.WeatherUiState.Success)
    }

    @Test
    fun `loadInitial with denied permission and no cache reaches NeedsLocation`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.PermissionDenied),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(com.fossisawesome.ventus.data.model.WeatherUiState.NeedsLocation, vm.state.value)
    }

    @Test
    fun `selectLocation saves location and fetches weather`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.PermissionDenied),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.selectLocation(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is com.fossisawesome.ventus.data.model.WeatherUiState.Success)
        assertEquals("Paris, France", prefs.locationName.first())
    }

    @Test
    fun `search populates searchResults from geocoding api`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val fakeResults = listOf(GeocodingResult(1, "London", 51.5, -0.12, "UK", null))
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.Unavailable),
            geocodingApi = FakeGeocodingApi(fakeResults),
            prefs = prefs,
            countryCode = "US",
        )

        vm.search("Lon")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeResults, vm.searchResults.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.viewmodel.WeatherViewModelTest"`
Expected: FAIL — `WeatherViewModel` doesn't exist yet.

- [ ] **Step 3: Write `WeatherViewModel.kt`**

```kotlin
package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.GeocodingResult
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.resolveUnits
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val locationSource: LocationSource,
    private val geocodingApi: GeocodingApi,
    private val prefs: AppPreferences,
    private val countryCode: String,
) : ViewModel() {

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    // On first load: prefer a previously saved location, otherwise try GPS, otherwise fall
    // back to any cached weather, otherwise ask the user to search.
    fun loadInitial() {
        viewModelScope.launch {
            val savedLat = prefs.locationLat.first()
            val savedLon = prefs.locationLon.first()
            val savedName = prefs.locationName.first()

            if (savedLat != null && savedLon != null && savedName != null) {
                fetchWeather(savedLat, savedLon, savedName)
                return@launch
            }

            when (val located = locationSource.getCurrentLocation()) {
                is LocationResult.Success -> {
                    prefs.setLocation(located.lat, located.lon, "Current location")
                    fetchWeather(located.lat, located.lon, "Current location")
                }
                LocationResult.PermissionDenied, LocationResult.Unavailable -> {
                    _state.value = repository.loadCached()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val lat = prefs.locationLat.first()
            val lon = prefs.locationLon.first()
            val name = prefs.locationName.first()
            if (lat != null && lon != null && name != null) {
                fetchWeather(lat, lon, name)
            } else {
                // No saved location yet — most likely retrying after a failed location fetch
                // (e.g. tapping "Retry" on the Error screen), so attempt it again rather than
                // silently doing nothing.
                attemptCurrentLocation()
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch { attemptCurrentLocation() }
    }

    private suspend fun attemptCurrentLocation() {
        when (val located = locationSource.getCurrentLocation()) {
            is LocationResult.Success -> {
                prefs.setLocation(located.lat, located.lon, "Current location")
                fetchWeather(located.lat, located.lon, "Current location")
            }
            LocationResult.PermissionDenied, LocationResult.Unavailable -> {
                _state.value = WeatherUiState.Error("Location unavailable — try searching for your city")
            }
        }
    }

    private var searchJob: Job? = null

    // Cancels any in-flight search before starting a new one — without this, a stale response
    // for an earlier keystroke's partial query can arrive after a later, more complete query's
    // response and overwrite it with outdated results.
    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchResults.value = geocodingApi.search(query)
        }
    }

    fun selectLocation(result: GeocodingResult) {
        viewModelScope.launch {
            val name = listOfNotNull(result.name, result.country).joinToString(", ")
            prefs.setLocation(result.latitude, result.longitude, name)
            _searchResults.value = emptyList()
            fetchWeather(result.latitude, result.longitude, name)
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double, name: String) {
        _state.value = WeatherUiState.Loading
        val unitsMode = prefs.unitsMode.first()
        val units = resolveUnits(unitsMode, countryCode)
        _state.value = repository.refresh(lat, lon, name, units)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.viewmodel.WeatherViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/viewmodel/WeatherViewModel.kt app/src/test/java/com/fossisawesome/ventus/viewmodel/WeatherViewModelTest.kt
git commit -m "Add WeatherViewModel orchestrating location, repository, and units"
```

---

### Task 11: SettingsViewModel

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/viewmodel/SettingsViewModel.kt`
- Create: `app/src/test/java/com/fossisawesome/ventus/viewmodel/SettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `AppPreferences` (Task 4), `AppTheme` (Task 2).
- Produces: `class SettingsViewModel(private val prefs: AppPreferences, private val loadThemes: () -> List<AppTheme>, private val importTheme: (String) -> Result<Unit>, private val deleteTheme: (String) -> Unit) : ViewModel()` with `val themeId: StateFlow<String>`, `val fontFamily: StateFlow<String>`, `val unitsMode: StateFlow<String>`, `val availableThemes: StateFlow<List<AppTheme>>`, `fun selectTheme(id: String)`, `fun selectFont(name: String)`, `fun selectUnitsMode(mode: String)`, `fun importThemeFile(uriString: String): Result<Unit>`, `fun deleteThemeFile(theme: AppTheme)`.

The Android-specific theme functions (`allThemes(context)`, `importThemeFromUri(context, uri)`,
`deleteImportedTheme(context, filename)` from Task 2) need a real `Context`, which a plain JUnit
test can't construct. Rather than duplicate the view model under test (which would test nothing
real) or pull in Robolectric for one class, `SettingsViewModel` takes those three operations as
constructor-injected function parameters — same pattern as `WeatherApi`/`LocationSource` in
Tasks 7/9. Production code (Task 13) supplies closures over `applicationContext`; tests supply
plain fakes.

`importTheme` takes a plain `String` (the picked file's URI, stringified) rather than
`android.net.Uri` — `Uri` is a platform type whose real methods throw outside an Android runtime
(no Robolectric here), so even `Uri.EMPTY`/`Uri.parse(...)` in a plain JUnit test either throws or
resolves to `null`, which then trips Kotlin's non-null parameter check on `importThemeFile` and
throws a `NullPointerException`. Keeping `SettingsViewModel` string-typed avoids the platform type
entirely; `MainActivity` (Task 13) converts `Uri.toString()` / `Uri.parse(...)` at the boundary.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.ui.theme.ALL_THEMES
import com.fossisawesome.ventus.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SettingsFakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `selectTheme persists the new theme id`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = SettingsViewModel(prefs, loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})

        vm.selectTheme("dracula")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("dracula", prefs.themeId.first())
    }

    @Test
    fun `selectFont persists the new font name`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = SettingsViewModel(prefs, loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})

        vm.selectFont("Hack")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Hack", prefs.fontFamily.first())
    }

    @Test
    fun `selectUnitsMode persists the new mode`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = SettingsViewModel(prefs, loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})

        vm.selectUnitsMode("imperial")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("imperial", prefs.unitsMode.first())
    }

    @Test
    fun `availableThemes reflects loadThemes at construction time`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val custom = ALL_THEMES.first().copy(id = "custom", name = "Custom", isImported = true, sourceFile = "custom.toml")
        val vm = SettingsViewModel(prefs, loadThemes = { ALL_THEMES + custom }, importTheme = { Result.success(Unit) }, deleteTheme = {})

        assertTrue(vm.availableThemes.value.any { it.id == "custom" })
    }

    @Test
    fun `importThemeFile refreshes availableThemes on success`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        var themesAfterImport = ALL_THEMES
        val vm = SettingsViewModel(
            prefs,
            loadThemes = { themesAfterImport },
            importTheme = { _ ->
                themesAfterImport = ALL_THEMES + ALL_THEMES.first().copy(id = "imported", name = "Imported", isImported = true, sourceFile = "imported.toml")
                Result.success(Unit)
            },
            deleteTheme = {},
        )

        val result = vm.importThemeFile("content://fake")

        assertTrue(result.isSuccess)
        assertTrue(vm.availableThemes.value.any { it.id == "imported" })
    }

    @Test
    fun `deleteThemeFile removes the theme and refreshes availableThemes`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val imported = ALL_THEMES.first().copy(id = "imported", name = "Imported", isImported = true, sourceFile = "imported.toml")
        var themes = ALL_THEMES + imported
        var deletedFile: String? = null
        val vm = SettingsViewModel(
            prefs,
            loadThemes = { themes },
            importTheme = { Result.success(Unit) },
            deleteTheme = { file -> deletedFile = file; themes = ALL_THEMES },
        )

        vm.deleteThemeFile(imported)

        assertEquals("imported.toml", deletedFile)
        assertTrue(vm.availableThemes.value.none { it.id == "imported" })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.viewmodel.SettingsViewModelTest"`
Expected: FAIL — `SettingsViewModel` doesn't exist yet.

- [ ] **Step 3: Write `SettingsViewModel.kt`**

```kotlin
package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: AppPreferences,
    private val loadThemes: () -> List<AppTheme>,
    private val importTheme: (String) -> Result<Unit>,
    private val deleteTheme: (String) -> Unit,
) : ViewModel() {

    val themeId: StateFlow<String> = prefs.themeId.stateIn(viewModelScope, SharingStarted.Eagerly, "ventus")
    val fontFamily: StateFlow<String> = prefs.fontFamily.stateIn(viewModelScope, SharingStarted.Eagerly, "Liberation Mono")
    val unitsMode: StateFlow<String> = prefs.unitsMode.stateIn(viewModelScope, SharingStarted.Eagerly, "auto")

    private val _availableThemes = MutableStateFlow(loadThemes())
    val availableThemes: StateFlow<List<AppTheme>> = _availableThemes

    fun selectTheme(id: String) {
        viewModelScope.launch { prefs.setThemeId(id) }
    }

    fun selectFont(name: String) {
        viewModelScope.launch { prefs.setFontFamily(name) }
    }

    fun selectUnitsMode(mode: String) {
        viewModelScope.launch { prefs.setUnitsMode(mode) }
    }

    fun importThemeFile(uriString: String): Result<Unit> {
        val result = importTheme(uriString)
        if (result.isSuccess) _availableThemes.value = loadThemes()
        return result
    }

    fun deleteThemeFile(theme: AppTheme) {
        val file = theme.sourceFile ?: return
        deleteTheme(file)
        _availableThemes.value = loadThemes()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fossisawesome.ventus.viewmodel.SettingsViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/viewmodel/SettingsViewModel.kt app/src/test/java/com/fossisawesome/ventus/viewmodel/SettingsViewModelTest.kt
git commit -m "Add SettingsViewModel for theme/font/units/import management"
```

---

### Task 12: MainScreen composable

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/screens/MainScreen.kt`

**Interfaces:**
- Consumes: `WeatherUiState`, `WeatherSnapshot`, `HourlyPoint`, `DailyPoint` (Task 6), `Units`, `celsiusToFahrenheit`, `kmhToMph`, `weatherCodeInfo` (Task 5), `Text`, `IconButton`, `AppIcon`, `Divider`, `Spinner`, `TextButton` (Task 3), `LocalAppColors`, `LocalAppFontFamily` (Task 2), `GeocodingResult` (Task 6).
- Produces: `@Composable fun MainScreen(state: WeatherUiState, searchResults: List<GeocodingResult>, onRefresh: () -> Unit, onUseCurrentLocation: () -> Unit, onSearchQueryChange: (String) -> Unit, onSelectSearchResult: (GeocodingResult) -> Unit, onSettingsClick: () -> Unit)`.

- [ ] **Step 1: Write `MainScreen.kt`**

```kotlin
package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.data.*
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    state: WeatherUiState,
    searchResults: List<GeocodingResult>,
    onRefresh: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSearchResult: (GeocodingResult) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val isRefreshing = state is WeatherUiState.Loading
    val pullState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = locationTitle(state),
                    color = colors.text,
                    fontFamily = font,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.Search, "Search for a city", tint = colors.muted)
                }
                IconButton(onClick = onUseCurrentLocation, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.MyLocation, "Use current location", tint = colors.muted)
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.Settings, "Settings", tint = colors.muted)
                }
            }

            if (showSearch) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it; onSearchQueryChange(it) },
                        textStyle = TextStyle(color = colors.text, fontFamily = font, fontSize = 16.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(12.dp),
                    )
                    searchResults.forEach { result ->
                        TextButton(onClick = {
                            showSearch = false
                            query = ""
                            onSelectSearchResult(result)
                        }) {
                            Text(
                                text = listOfNotNull(result.name, result.country).joinToString(", "),
                                color = colors.text,
                                fontFamily = font,
                            )
                        }
                    }
                }
            }

            Divider()

            Box(modifier = Modifier.weight(1f).pullRefresh(pullState)) {
                when (state) {
                    is WeatherUiState.Loading -> LoadingBody()
                    is WeatherUiState.NeedsLocation -> NeedsLocationBody(onUseCurrentLocation)
                    is WeatherUiState.Error -> ErrorBody(state.message, onRefresh)
                    is WeatherUiState.Success -> ForecastBody(state.snapshot, staleBanner = null)
                    is WeatherUiState.Stale -> ForecastBody(state.snapshot, staleBanner = staleLabel(state.fetchedAt))
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = colors.surface,
                    contentColor = colors.accent,
                )
            }
        }
    }
}

private fun locationTitle(state: WeatherUiState): String = when (state) {
    is WeatherUiState.Success -> state.snapshot.locationName
    is WeatherUiState.Stale -> state.snapshot.locationName
    else -> "Ventus"
}

private fun staleLabel(fetchedAt: Long): String {
    val minutesAgo = (System.currentTimeMillis() - fetchedAt) / 60000
    return when {
        minutesAgo < 1 -> "Updated just now — pull to refresh"
        minutesAgo < 60 -> "Updated ${minutesAgo}m ago — pull to refresh"
        else -> "Updated ${minutesAgo / 60}h ago — pull to refresh"
    }
}

@Composable
private fun LoadingBody() {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Spinner(color = colors.accent, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun NeedsLocationBody(onUseCurrentLocation: () -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Find your weather", color = colors.text, fontFamily = font, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Use your current location or search for a city above.",
            color = colors.muted,
            fontFamily = font,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onUseCurrentLocation) {
            Text("Use my location", color = colors.accent, fontFamily = font, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = colors.error, fontFamily = font, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = colors.accent, fontFamily = font, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ForecastBody(snapshot: WeatherSnapshot, staleBanner: String?) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    val isImperial = snapshot.units == Units.IMPERIAL

    fun displayTemp(c: Double): String {
        val v = if (isImperial) celsiusToFahrenheit(c) else c
        return "${v.toInt()}°${if (isImperial) "F" else "C"}"
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (staleBanner != null) {
            Box(modifier = Modifier.fillMaxWidth().background(colors.surface2).padding(8.dp)) {
                Text(staleBanner, color = colors.muted, fontFamily = font, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val info = weatherCodeInfo(snapshot.currentWeatherCode)
            Text(info.icon, fontSize = 48.sp)
            Text(displayTemp(snapshot.currentTempC), color = colors.text, fontFamily = font, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(info.description, color = colors.muted, fontFamily = font)
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Feels like ${displayTemp(snapshot.currentApparentTempC)}", color = colors.muted, fontFamily = font, fontSize = 13.sp)
                Spacer(Modifier.width(16.dp))
                Text("Humidity ${snapshot.currentHumidity}%", color = colors.muted, fontFamily = font, fontSize = 13.sp)
                Spacer(Modifier.width(16.dp))
                val wind = if (isImperial) kmhToMph(snapshot.currentWindKmh) else snapshot.currentWindKmh
                Text("Wind ${wind.toInt()} ${if (isImperial) "mph" else "km/h"}", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            }
        }

        Divider()

        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            items(snapshot.hourly) { hour ->
                Column(
                    modifier = Modifier.width(64.dp).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hourLabel(hour.epochSeconds), color = colors.muted, fontFamily = font, fontSize = 12.sp)
                    Text(weatherCodeInfo(hour.weatherCode).icon, fontSize = 20.sp)
                    Text(displayTemp(hour.tempC), color = colors.text, fontFamily = font, fontSize = 13.sp)
                }
            }
        }

        Divider()

        Column(modifier = Modifier.fillMaxWidth()) {
            snapshot.daily.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayLabel(day.epochSeconds), color = colors.text, fontFamily = font, modifier = Modifier.weight(1f))
                    Text(weatherCodeInfo(day.weatherCode).icon, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(displayTemp(day.tempMinC), color = colors.muted, fontFamily = font)
                    Spacer(Modifier.width(8.dp))
                    Text(displayTemp(day.tempMaxC), color = colors.text, fontFamily = font, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun hourLabel(epochSeconds: Long): String =
    SimpleDateFormat("ha", Locale.getDefault()).format(Date(epochSeconds * 1000))

private fun dayLabel(epochSeconds: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochSeconds * 1000))
```

Note: the header `Row`'s `.windowInsetsPadding(WindowInsets.statusBars)` (before the `.padding(16.dp)`)
is required, not optional polish. `MainActivity` calls `enableEdgeToEdge()`, which draws app content
underneath the system status bar. Without this inset padding, the header row's icon buttons render
with their top ~2/3 physically underneath the status bar — the system status bar window captures
touches in that overlapping region, so only a thin sliver at the bottom of each button is actually
tappable. This is easy to miss in a screenshot (the icons still *look* fine) and only shows up when
you actually try to tap them on a device/emulator — confirmed by a manual smoke test where taps at
the visual center of the icons did nothing until this inset padding was added.

- [ ] **Step 2: Add the pull-refresh dependency**

Modify `app/build.gradle.kts` — add this line inside the `dependencies { }` block, next to the
other `androidx.compose.foundation` line:

```kotlin
    implementation("androidx.compose.material:material") // pullrefresh lives in the classic material artifact, not material3
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/ui/screens/MainScreen.kt app/build.gradle.kts
git commit -m "Add MainScreen: current/hourly/daily forecast with pull-to-refresh"
```

---

### Task 13: SettingsScreen + navigation + MainActivity wiring

**Files:**
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/screens/SettingsScreen.kt`
- Create: `app/src/main/java/com/fossisawesome/ventus/ui/navigation/AppNavGraph.kt`
- Modify: `app/src/main/java/com/fossisawesome/ventus/VentusApplication.kt`
- Modify: `app/src/main/java/com/fossisawesome/ventus/MainActivity.kt`

**Interfaces:**
- Consumes: everything from Tasks 2–12.
- Produces: `@Composable fun SettingsScreen(themeId: String, fontFamily: String, unitsMode: String, availableThemes: List<AppTheme>, onThemeSelected: (String) -> Unit, onFontSelected: (String) -> Unit, onUnitsModeSelected: (String) -> Unit, onImportTheme: () -> Unit, onBack: () -> Unit)`, `@Composable fun AppNavGraph(weatherViewModel: WeatherViewModel, settingsViewModel: SettingsViewModel, onImportTheme: () -> Unit)`. `VentusApplication` gains lazy singletons; `MainActivity` requests location permission and hosts `VentusTheme { AppNavGraph(...) }`.

- [ ] **Step 1: Write `SettingsScreen.kt`**

```kotlin
package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.AppFontKey
import com.fossisawesome.ventus.ui.theme.AppTheme
import com.fossisawesome.ventus.ui.theme.FONT_OPTIONS
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily

@Composable
fun SettingsScreen(
    themeId: String,
    fontFamily: String,
    unitsMode: String,
    availableThemes: List<AppTheme>,
    onThemeSelected: (String) -> Unit,
    onFontSelected: (String) -> Unit,
    onUnitsModeSelected: (String) -> Unit,
    onImportTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                AppIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.muted)
            }
            Text("Settings", color = colors.text, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Divider()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Theme", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(240.dp),
            ) {
                items(availableThemes) { theme ->
                    ThemeSwatch(theme = theme, selected = theme.id == themeId, onClick = { onThemeSelected(theme.id) })
                }
            }
            TextButton(onClick = onImportTheme) {
                Text("Import theme…", color = colors.accent, fontFamily = font)
            }

            Spacer(Modifier.height(24.dp))
            Text("Font", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FONT_OPTIONS.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFontSelected(option) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        option,
                        color = if (option == fontFamily) colors.accent else colors.text,
                        fontFamily = font,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Units", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            listOf("auto" to "Auto (by location)", "metric" to "Metric (°C, km/h)", "imperial" to "Imperial (°F, mph)").forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUnitsModeSelected(value) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(label, color = if (value == unitsMode) colors.accent else colors.text, fontFamily = font)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(theme.bg)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.accent))
        Spacer(Modifier.height(4.dp))
        Text(
            theme.name,
            color = if (selected) theme.accent else theme.text,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}
```

- [ ] **Step 2: Write `AppNavGraph.kt`**

```kotlin
package com.fossisawesome.ventus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fossisawesome.ventus.ui.screens.MainScreen
import com.fossisawesome.ventus.ui.screens.SettingsScreen
import com.fossisawesome.ventus.viewmodel.SettingsViewModel
import com.fossisawesome.ventus.viewmodel.WeatherViewModel

@Composable
fun AppNavGraph(
    weatherViewModel: WeatherViewModel,
    settingsViewModel: SettingsViewModel,
    onImportTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val weatherState by weatherViewModel.state.collectAsStateWithLifecycle()
    val searchResults by weatherViewModel.searchResults.collectAsStateWithLifecycle()
    val themeId by settingsViewModel.themeId.collectAsStateWithLifecycle()
    val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
    val unitsMode by settingsViewModel.unitsMode.collectAsStateWithLifecycle()
    val availableThemes by settingsViewModel.availableThemes.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                state = weatherState,
                searchResults = searchResults,
                onRefresh = { weatherViewModel.refresh() },
                onUseCurrentLocation = { weatherViewModel.useCurrentLocation() },
                onSearchQueryChange = { weatherViewModel.search(it) },
                onSelectSearchResult = { weatherViewModel.selectLocation(it) },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                themeId = themeId,
                fontFamily = fontFamily,
                unitsMode = unitsMode,
                availableThemes = availableThemes,
                onThemeSelected = { settingsViewModel.selectTheme(it) },
                onFontSelected = { settingsViewModel.selectFont(it) },
                onUnitsModeSelected = { settingsViewModel.selectUnitsMode(it) },
                onImportTheme = onImportTheme,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
```

- [ ] **Step 3: Add singletons to `VentusApplication.kt`**

```kotlin
package com.fossisawesome.ventus

import android.app.Application
import com.fossisawesome.ventus.data.api.OpenMeteoGeocodingApi
import com.fossisawesome.ventus.data.api.OpenMeteoWeatherApi
import com.fossisawesome.ventus.data.location.FusedLocationSource
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences

// Manual DI container — holds app-wide singletons shared across ViewModels.
class VentusApplication : Application() {
    val prefs by lazy { AppPreferences(this) }
    val weatherApi by lazy { OpenMeteoWeatherApi() }
    val geocodingApi by lazy { OpenMeteoGeocodingApi() }
    val weatherRepository by lazy { WeatherRepository(weatherApi, prefs) }
    val locationSource by lazy { FusedLocationSource(this) }
}
```

- [ ] **Step 4: Wire it all together in `MainActivity.kt`**

```kotlin
package com.fossisawesome.ventus

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fossisawesome.ventus.ui.navigation.AppNavGraph
import com.fossisawesome.ventus.ui.theme.VentusTheme
import com.fossisawesome.ventus.ui.theme.allThemes
import com.fossisawesome.ventus.ui.theme.deleteImportedTheme
import com.fossisawesome.ventus.ui.theme.importThemeFromUri
import com.fossisawesome.ventus.viewmodel.SettingsViewModel
import com.fossisawesome.ventus.viewmodel.WeatherViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val app get() = application as VentusApplication

    // Set from inside setContent once SettingsViewModel exists; read by the launcher callback.
    private var onThemeUriPicked: ((Uri) -> Unit)? = null

    private val themeImportLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onThemeUriPicked?.invoke(it) } }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: WeatherViewModel checks permission itself via LocationSource each time the
          user taps "Use my location", so a later grant is picked up on the next tap. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val countryCode = resources.configuration.locales[0].country.ifBlank { Locale.getDefault().country }

        setContent {
            val weatherViewModel: WeatherViewModel = viewModel(
                factory = viewModelFactory {
                    WeatherViewModel(app.weatherRepository, app.locationSource, app.geocodingApi, app.prefs, countryCode)
                }
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(
                        prefs = app.prefs,
                        loadThemes = { allThemes(applicationContext) },
                        importTheme = { uriString -> importThemeFromUri(applicationContext, Uri.parse(uriString)) },
                        deleteTheme = { file -> deleteImportedTheme(applicationContext, file) },
                    )
                }
            )

            // Registered once per composition; importThemeFile() re-reads availableThemes on success.
            onThemeUriPicked = { uri -> settingsViewModel.importThemeFile(uri.toString()) }

            LaunchedEffect(Unit) { weatherViewModel.loadInitial() }

            val themeId by settingsViewModel.themeId.collectAsStateWithLifecycle()
            val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()

            VentusTheme(themeId = themeId, fontFamily = fontFamily) {
                AppNavGraph(
                    weatherViewModel = weatherViewModel,
                    settingsViewModel = settingsViewModel,
                    onImportTheme = { themeImportLauncher.launch("*/*") },
                )
            }
        }
    }

    private inline fun <VM : ViewModel> viewModelFactory(crossinline creator: () -> VM) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }
}
```

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Manual smoke test**

Run: `./gradlew installDebug` with an emulator or device attached, then launch Ventus.
Expected: app requests location permission; on grant, the main screen fetches and displays
current/hourly/daily forecast for the device's location; pull-to-refresh re-fetches; the settings
gear opens the theme grid/font list/units rows and switching a theme immediately re-skins the
whole app (background, text, accent).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/fossisawesome/ventus/ui/screens/SettingsScreen.kt app/src/main/java/com/fossisawesome/ventus/ui/navigation app/src/main/java/com/fossisawesome/ventus/VentusApplication.kt app/src/main/java/com/fossisawesome/ventus/MainActivity.kt
git commit -m "Add SettingsScreen, navigation graph, and wire the app together end-to-end"
```
