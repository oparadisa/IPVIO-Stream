# Mobile plan: Couchless (IPVIO Stream fork) on phones and tablets

Status: **Approved** (2026-11-09) with the review notes in section 8. Phases 1–4 merged; phase 5 in progress.

## 1. What the codebase gives us today

| Area | Today | What it means for mobile |
|---|---|---|
| Router | `AppContent()` in `MainActivity.kt:177` holds all navigation state (`tab`, `top`/`trail`, `browse`, `cats`, `playing`, `adding`) and one big `when` that picks the screen. | Keep this router and its logic (`start()`, `open`, resume, reminders, phone remote). Branch **per screen** inside the `when`, so on mobile the same state drives mobile screens. No second router. |
| Scaling | `App()` scales `LocalDensity` with `ThemeMath.uiFactor` (design is 960 dp wide; phones get 0.72–1.0). `Design.k` = width / 1658 for `dpx()`. | Mobile screens re-provide the **real device density** (no TV scaling) and use plain `dp`. Any TV screen still shown on a phone keeps today's scaling unchanged. |
| Navigation | `Sidebar(...)` is drawn by 4 screens (`HomeScreen`, `BrowseScreen`, `CategoriesScreen`, `SearchScreen`). `SidebarPrefs` (left/right/top), `sidebarPad()`. Menu order/hidden come from `MenuLogic` + `AppSettings.menuOrder/menuHidden`. | On mobile those 4 screens are replaced, so `Sidebar` stays untouched. A new bottom bar / rail reads the same `MenuLogic.visible(...)` so the user's menu settings still apply. |
| Focus | D-pad focus everywhere (`FocusCompat.kt`, `Keys.kt`, `ContentFocus`, `LastFocus`, `restoreFocus`). | Mobile screens don't use focus requesters. Taps and scrolling only. |
| Text input | `Field` (`Ui.kt:233`) already uses `BasicTextField` and the system IME. `SearchKeyboard` (built-in keyboard) appears in `SearchScreen`, `LiveTab` (channel search) and `GuideSearch`. `AppSettings.systemKeyboard` defaults to `false` (`Store.kt:111`). | Mobile search screens use a plain `BasicTextField` with the system keyboard. The setting's default becomes `true` on mobile (one-line change), and the toggle is hidden on mobile. |
| Phone support | `MobilePrefs.isMobile()` (no leanback + touch). `MobilePrefs.apply()` forces `SENSOR_LANDSCAPE` (toggle in Settings > General). Player tap / double-tap at `PlayerScreen.kt:1160`. | Extend `MobilePrefs`: no forced landscape outside the player. Keep the gesture code and add drag gestures next to it. |
| Insets | None anywhere (no `WindowInsets`, no `enableEdgeToEdge`). With targetSdk 37 the app already draws under the status and navigation bars on phones. | Mobile shell pads with `WindowInsets.safeDrawing`. TV fallback screens on mobile get the same padding through one wrapper. Player goes immersive. |
| Rotation | `configChanges` already includes `orientation|screenSize|…`, so rotating does not recreate the activity. | Rotation is cheap and state survives it. The player keeps running when it rotates. |
| Player lifecycle | `PlayerScreen.kt:796` pauses on `ON_PAUSE`. | PiP puts the activity in paused state, so that pause must be skipped while in PiP (mobile only). |
| Build | Local machine: Android SDK has only `android-34` / build-tools 34, **no JDK**. The project needs compileSdk 37 and Gradle 9.6. | I can't build here without installing a JDK 21 and SDK platform 37 (~1.5 GB). Unless you want me to install them, I rely on GitHub Actions (`build.yml` runs on push to `main` and on PRs). Make sure Actions is enabled on the fork. |

## 2. Approach

### 2.1 Where mobile code lives
New package `com.ipvio.stream.mobile` (`app/src/main/java/com/ipvio/stream/mobile/`):

| New file | Purpose |
|---|---|
| `MobileLogic.kt` | Pure, unit-tested: window size class from width dp (compact < 600, medium < 840, expanded), bottom-bar items + "More" overflow from `MenuLogic.visible`, swipe delta → brightness/volume step, player orientation decision (auto-landscape vs lock). |
| `MobileUi.kt` | `MobileUi.active` (reactive, see 8.1), `rememberSizeClass()`, `NativeDensity { }` (undo TV scaling), `Modifier.mobileInsets()`, shared touch atoms (48 dp row, chip, icon button). Built on the existing tokens (`Tok`, `Accent2`, `Bg`, `AppIcon`, `Text`) so the look matches. |
| `MobileShell.kt` | Scaffold: top bar (title + search icon), content, **bottom bar** (compact) or **navigation rail** (medium/expanded), insets. |
| `MobileHome.kt` | Start, Movies, Series, Favorites, My list, History tabs: vertical `LazyColumn` of horizontal `LazyRow`s, reusing the row data HomeScreen already computes (see risk R3). |
| `MobileLive.kt` | Live TV: category chips + channel list (phone); list + now/next preview pane (tablet). |
| `MobileBrowse.kt` | Grid (`LazyVerticalGrid`, `GridCells.Adaptive(110.dp)`) for `BrowseScreen` and `CategoriesScreen`. |
| `MobileDetail.kt` | Movie/series detail: header, play/resume, season picker + episode list; two panes (seasons │ episodes) on tablets. |
| `MobileSearch.kt` | System-keyboard search on top of the existing `SearchLogic`. |
| `MobileSettings.kt` | Settings category list → page (phone), list │ page side by side (tablet). Reuses the existing page composables. |
| `MobilePlayer.kt` | Player overlay: gestures (keeps tap/double-tap, adds brightness/volume swipes), draggable seek bar, rotation-lock button, immersive mode, PiP. |

Each new user-visible string goes through `Lang.tr()` with German source text and gets entries in the `Dict*.kt` files (`DictConsistencyTest` checks them).

### 2.2 Edits to existing files (kept small)
| File | Change |
|---|---|
| `app/build.gradle.kts` | `applicationId`, `appLabel` (housekeeping). No new dependencies planned: window size classes are 3 thresholds in `MobileLogic`, and `androidx.core` (insets controller) is already there through `activity-compose`. |
| `AndroidManifest.xml` | `MainActivity`: `android:supportsPictureInPicture="true"` (does nothing until code calls `enterPictureInPictureMode`, which only the mobile player does). |
| `MainActivity.kt` | (a) `App()`: hand the unscaled density to `MobileUi`; (b) in `AppContent`'s `when`: `if (MobileUi.active) MobileX(...) else X(...)` for Home/Search/Browse/Categories/Detail (one line each); (c) `onUserLeaveHint`: enter PiP when mobile and playing; (d) TV fallback screens on mobile get `mobileInsets()`. |
| `MobilePrefs.kt` | Mobile is no longer forced to landscape: `apply()` uses `UNSPECIFIED` (follows the device). The "landscape" setting goes away; the player handles its own orientation. |
| `Store.kt` | `systemKeyboard` default = `MobilePrefs.isMobile(c)` (stored values win). |
| `SettingsHub.kt` | Hide TV-only rows when mobile (autostart + overlay permission, built-in keyboard toggle, "confirm exit", landscape toggle). Make the page composables `internal` so `MobileSettings` can call them. |
| `HomeScreen.kt` | "Back twice to exit" is skipped on mobile (`MobileHome` handles back itself, so this may need no change). |
| `PlayerScreen.kt` | One branch at the gesture box (~line 1160): `if (MobileUi.active) MobilePlayerOverlay(...) else <existing box>`. Lifecycle `ON_PAUSE`: don't pause while in PiP. Hide the TV control bar in PiP. |
| `AddPlaylistScreen.kt`, `ListImportUi.kt`, `TransferUi.kt`, `RemoteUi.kt` | Hide the "send from phone" QR / local server section on mobile (don't start the server). |
| `AppUpdate.kt` | `DEFAULT_URL` → `https://api.github.com/repos/oparadisa/IPVIO-Stream/releases/latest`. |
| `README.md` | Unofficial-fork notice, credit + link to upstream, mobile notes. LICENSE and THIRD_PARTY_NOTICES stay as they are. |

Rule check: every behaviour change sits behind `MobileUi.active` / `isMobile()` or a size class, except housekeeping (ID, label, update URL) and the inert PiP manifest flag.

## 3. Screens, one by one

Legend: **P** = phone (compact), **T** = tablet (medium/expanded). "TV fallback" = existing TV screen, shown with today's scaling plus inset padding, and briefly locked to landscape on compact portrait so its fixed widths fit.

| Screen (file) | Phone | Tablet |
|---|---|---|
| Profile picker (`Profiles.kt`) | Simple list of profiles, 48 dp rows. If that grows big: TV fallback in phase 2, mobile version later. | Same, centred, max width 480 dp. |
| Add playlist (`AddPlaylistScreen.kt`) | Keep the screen; it already uses `Field` (system IME). Hide phone import. Make the form scroll; insets. Portrait is checked on a device; TV fallback if it doesn't fit. | Same, max width 640 dp. |
| Loading overlay (`LoadingOverlay.kt`) | Unchanged, plus insets. | Unchanged. |
| Navigation (`Sidebar.kt`) | **Bottom bar**: first 4 visible menu entries (default Home, Live TV, Movies, Series) + "More" (Favorites, My list, History, Sport, EPG, Settings). Search in the top bar. | **Navigation rail** with all visible entries; Settings pinned at the bottom. |
| Home (`HomeScreen.kt` tab 0) | Hero card (tap to play) + vertical list of horizontal rows (continue watching, favorites, top 10, recently added…), same blocks and order as `HomeLayout`. Pull-free, normal fling. | Same, bigger cards, 2 rows of hero. |
| Movies / Series (tabs 1, 2) | Category chips row + rows; "Show all" → grid. | Same with wider grids. |
| Categories (`CategoriesScreen.kt`) | List of categories → grid. | Category list │ grid preview (two panes). |
| Browse grid (`BrowseScreen.kt`) | Adaptive poster grid, sort/filter in a bottom sheet-style panel (hand-made, no Material). | Same, more columns. |
| Detail (`DetailScreen.kt`) | Backdrop, title, meta, Play/Resume, My list, trailer; series: season chips + episode list (tap = play, long-press = mark seen). Person links → TV fallback `PersonScreen` first, mobile later. | **Two panes**: seasons list │ episodes; movie: info │ cast/related. |
| Live TV (`LiveTab.kt`) | Category chips (Favorites, All, groups) + channel list (logo, name, now/next + progress). Tap = play. Long-press = channel menu (`ChannelMenu` reused). Search field with system keyboard. | **Two panes**: channel list │ preview (now/next, EPG for the selected channel via `LiveNowPanel`). Tap selects, tap again / play button plays. |
| TV guide / EPG (`EpgFull.kt`, `PlayerGuide.kt`) | Opens in the player as today. Phase 4: make the guide grid scroll with touch (check how it handles drag). | Same. |
| Favorites / My list / History (tabs 7, 8, 9) | Grids inside `MobileHome`. | Same. |
| Sport (`SportHub.kt`, tab 10) | TV fallback (in "More"). Mobile version is out of scope for now. | TV fallback (landscape tablets fit fine). |
| Search (`SearchScreen.kt`, tab 4) | Top text field (system keyboard, voice button if available), results grouped by type. No built-in keyboard. | Results grid wider. |
| Settings (`SettingsHub.kt`) | Category list → page, back returns to list. TV-only rows and categories hidden ("Boomer Mode" stays). | **Two panes**: category list │ page. |
| Settings pages | Reuse existing page composables. They use `dp` with a few fixed widths (e.g. `StoragePage` `width(420.dp)`); fix those with `Modifier.widthIn(max=…)`/`weight` only where they break on a phone. | Fit as is. |
| Boomer mode (`Boomer.kt`) | TV fallback. | TV fallback. |
| Player (`PlayerScreen.kt`) | See section 4. | Same, no forced landscape on tablets in landscape already. |
| Multi-view / dual view / PiP-in-player (`MultiView.kt`, `DualView.kt`) | Hidden on phones (too small); unchanged on tablets. | TV behaviour (touch works through clickable). |
| Phone remote / transfer (`RemoteUi.kt`, `TransferUi.kt`, `PhoneImport.kt`) | Hidden. | Hidden. |
| Update prompt, reminders, sport alerts | Unchanged, plus insets. | Unchanged. |

### Hidden on mobile (TV-only)
Phone import (QR + local server), built-in on-screen keyboard (setting hidden, system keyboard default), boot autostart + overlay permission rows, "confirm exit / back twice", the landscape toggle. Leanback banner and `LEANBACK_LAUNCHER` stay in the manifest: phones ignore both, and removing them would change the TV app.

## 4. Player on mobile

| Feature | How |
|---|---|
| Auto-landscape | On entering the player: `requestedOrientation = SENSOR_LANDSCAPE`; on leaving: back to `UNSPECIFIED`. Tablets that are already landscape just stay. |
| Rotation lock | Button in the top bar of the controls: toggles between `SENSOR_LANDSCAPE` and `LOCKED` (current orientation). Remembered in `MobilePrefs`. |
| Immersive | `WindowInsetsControllerCompat.hide(systemBars())`, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` while the player is open; restored on exit. Cutout: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` while playing. |
| Tap / double-tap | Kept exactly (moved into `MobilePlayerOverlay`, same `detectTapGestures` code). |
| Brightness (left half) / volume (right half) | `detectVerticalDragGestures`; brightness via `window.attributes.screenBrightness` (player only, reset on exit); volume via `AudioManager.setStreamVolume(STREAM_MUSIC)`. Small vertical level indicator. Drag start zones avoid the top/bottom 48 dp (system gesture areas). |
| Seek bar | Draggable bar in the bottom controls (`pointerInput` + `detectHorizontalDragGestures`/tap) reusing `PSeekTrack` visuals; seeks on release, shows target time while dragging. Live with timeshift: same bar over the timeshift window; plain live: no bar. |
| Picture-in-picture | `onUserLeaveHint` (+ `setAutoEnterEnabled` on API 31+) → `enterPictureInPictureMode` with the video aspect ratio, only when mobile, playing and the setting is on (new toggle in Settings > Player, default on). Lifecycle pause is skipped while in PiP; controls hidden in PiP; `AppNav.leftByUser` isn't set in PiP so returning doesn't jump to home. |
| Back | Back closes the player as today. |

## 5. Phases and commits

Every phase ends with `./gradlew testDebugUnitTest assembleDebug` green on GitHub Actions and one commit with "what to test on the device".

0. **This plan** (no code).
1. **Housekeeping**: new `applicationId` (**your choice; see question below**), new label, README fork notice + upstream credit, `AppUpdate.DEFAULT_URL` → your fork. `upstream` remote: already set up (`Emrodev/IPVIO-Stream`).
2. **Navigation shell & insets**: `mobile/` package, `MobileLogic` + tests, `MobileUi`, `MobileShell` with bottom bar / rail, insets, native density, drop forced landscape, system keyboard default, hide TV-only settings and phone import. TV fallback screens wrapped (landscape lock on compact portrait). At the end, every screen is reachable on a phone, with mobile navigation.
3. **Main screens**: Home/Movies/Series/Favorites/My list/History, Live TV, Browse/Categories grid, Detail, Search, Settings list (phone layouts).
4. **Player**: overlay, gestures, seek bar, auto-landscape, rotation lock, immersive, PiP + lifecycle fix.
5. **Tablet layouts**: rail polish, two-pane Live TV, Detail (seasons │ episodes), Categories, Settings.
6. **Portrait TV guide and player leftovers** (deferred from phases 4 and 5):
   - Portrait TV guide (see 8.3).
   - TV guide on touch: dragging along the time axis doesn't scroll yet, and it still shows the TV key legend.
   - Channel list in the player: long-press for favorite.
   - Archive / catch-up panel: it can't be opened on mobile (no button), and it has no close X.
   - PiP window: its own play/pause button (needs a media session).

## 6. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | **No local build.** No JDK, SDK platform 37 missing. Compile errors show up only in CI, and every phase is a round trip through GitHub Actions and your phone. | Small commits; push each phase to a branch, open a PR so CI runs; or I install JDK 21 + platform 37 locally if you approve. |
| R2 | **TV regressions** from shared code paths (density, insets, Store default, SettingsHub rows). | All branches use `MobileUi.active`, which is `false` on any device with leanback/Fire TV features. Housekeeping is the only unconditional change. I'll list every edited line of existing files in each commit message. |
| R3 | **Home row data is computed inside `HomeScreen`** (~500 lines mixing data and UI). Reusing it without copying needs a small extraction (e.g. a `HomeRows` function) into a shared file, which touches upstream code. | Prefer extracting pure row-building into a new `HomeRowsLogic.kt` called by both (small, testable). If upstream churn makes that costly, the mobile home calls the same stores directly (`WatchStore`, `History`, `Library`, `PlaylistStore`) and builds a simpler row set. |
| R4 | **Upstream merges**: `MainActivity`, `SettingsHub`, `PlayerScreen` change often upstream. | Edits are single-line branch points; mobile code is in its own package. |
| R5 | **Fixed TV widths in reused composables** (settings pages, dialogs like `GuideSearch` at `1180.dp`, `ChannelMenu`). | Fix only what breaks on the device, with `widthIn(max)`; otherwise TV fallback. |
| R6 | **PiP + existing lifecycle code** (pause on `ON_PAUSE`, `leftByUser` → home reset, `ScreenAwake`, `Recorder`). | Guard with `isInPictureInPictureMode`; test Home key, recents, notification tap, phone lock. |
| R7 | **Portrait for TV fallback screens** (Sport, Boomer, Person, EPG grid) is still cramped. | Landscape lock while those screens are open on compact portrait; mobile versions later if wanted. |
| R8 | **New applicationId = new install**: settings, playlists and history don't carry over from the official app (different data dir). | Existing Backup/transfer works across apps (export from the official app, import in the fork). Mention in README. |
| R9 | **Self-updater**: the fork's release APK must be signed with your own key and tagged `vX.Y`, or updates fail to install. | Documented in README (follows `docs/RELEASE.md`). |
| R10 | **Translations**: new strings need 10 languages; machine-quality wording for some. | Keep new strings few and short; `DictConsistencyTest` guards completeness. |

## 7. Open questions for you
1. **applicationId** for the fork (e.g. `io.github.oparadisa.ipvio`)? The debug build keeps the `.test` suffix.
2. **App label** (e.g. "IPVIO Mobile")?
3. Build: install JDK 21 + Android SDK platform 37 locally so I can compile before pushing, or CI only?
4. Branch/PR workflow: one PR per phase into your `main`, or commit straight to `main`?

## 8. Review notes (approved with these)

### 8.1 Reactive mobile state
`MobileUi.active` and phone vs tablet are **Compose state derived from the current window size** (`LocalConfiguration.screenWidthDp/screenHeightDp`, which updates on every config change because `MainActivity` handles `orientation|screenSize|smallestScreenSize|screenLayout` itself). They are never computed once at startup. Rotation, foldables, split-screen and freeform resizing switch layouts live, without a restart. The device check (`MobilePrefs.isMobile`: no leanback + touch) stays the gate for "is this the TV app", and the size class picks phone/tablet layouts.

### 8.2 System font size
Mobile screens use the device density **and** `fontScale` as the system gives them (the TV text factor `General.textFactor()` doesn't apply there). Layouts must survive 1.3× and 2× font scale (checked on the emulator with `adb shell settings put system font_scale`).

### 8.3 Portrait TV guide (candidate phase 6)
Locking Sport, Boomer, the person page and the TV guide to landscape is fine for now. The TV guide is used a lot from Live TV, so a **portrait TV guide** is a candidate phase after the tablet work.

### 8.4 Home rows extraction
The move of row building out of `HomeScreen` is a **pure move with no behaviour change**, into a new `*Logic.kt` with unit tests, so upstream changes to `HomeScreen` stay easy to merge.

### 8.5 Testing per phase
Local build with Android Studio's JBR + SDK platform 37 (`local.properties`, gitignored): `./gradlew testDebugUnitTest assembleDebug` before every push. Then check on the emulators: Pixel_9 (portrait + landscape), Pixel_Tablet, the Resizable device (resized while open), and an Android TV AVD (≤ 4 GB storage) to confirm the TV layout is unchanged. Screenshots go to `/screenshots/` (gitignored, never committed). The official app on Pixel_9 is left alone. One PR per phase into `main`; each PR lists what was tested and what still needs a real phone.

### 8.6 Package name findings (phase 1)
- Backup content is package-independent: it is a JSON of SharedPreferences by file name (`Backup.exportText`/`restoreText`), plus optionally the playlists.
- **Tied to the package**: the plain "Create backup" / "Restore backup" buttons use `getExternalFilesDir()` (`Android/data/<package>/files/ipvio_backup.json`), so the fork can't see the official app's file. Use "Save backup as …" / "Save backup with playlists …" (system file picker) and "Load backup from file …", or Wi-Fi transfer.
- Google Drive sync is a stub in this version (`DriveSync.kt` is empty), so no OAuth client is tied to the package.
- `FileProvider` authority is `${applicationId}.fileprovider` and the code uses `packageName`, so both follow the new ID.
- Kotlin package / `namespace` stay `com.ipvio.stream` (no source moves, which keeps upstream merges cheap).
- `README.md` asks forks to use their own name and logo: the fork is named **Couchless** (`io.github.oparadisa.couchless`) with its own launcher icon and TV banner (vector drawables, sofa + play mark). The in-app header logo (`ic_brand_mark.png`, `Brand.kt`) and the "IPVIO Stream" wording inside the app are still upstream's (see open points).
