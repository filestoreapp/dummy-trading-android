# Dummy Trading — Android App (phone-local)

A real, phone-local Android app for learning trading with **dummy money only**.
It is **not** a WebView wrapper around a website, it does **not** sync to the
web version, and it **never places a real order** — there is no broker
order-placement code anywhere in this project. Dummy buys and sells are
recorded in a local Room database on the phone only.

## What it does

- Fresh virtual wallet: **₹10,00,000**, empty holdings on first run
- Search NSE stocks, BSE stocks and crypto; NSE / BSE / CRYPTO selector
- Dummy buy/sell — stocks by quantity, crypto by ₹ amount (fractional)
- Holdings **separated per exchange**: RELIANCE on NSE and RELIANCE on BSE
  are two separate positions at their own prices
- Portfolio value / invested / total P&L, per-holding P&L
- Simulated charges:
  - Stocks: brokerage 0.03% capped at ₹20, STT 0.1%, exchange txn
    0.00345% (NSE) / 0.00375% (BSE), SEBI 0.0001%, stamp 0.015% on buys,
    GST 18% on brokerage + exchange txn
  - Crypto: 0.25% fee, 1% TDS on sells only
- Order history with realised P&L, per-trade journal note, watchlist,
  portfolio reset
- Screens: Portfolio, Market, Stock detail (buy/sell), Holdings,
  Orders/Journal, Watchlist, Settings

## Prices — read the label on every price

| Source | What you get | Label in the app |
|---|---|---|
| Yahoo Finance (`.NS` / `.BO`) | NSE/BSE quotes, **delayed ~15 min**, refreshed every 30s in market hours | Delayed price — not live |
| Kraken public ticker / websocket | Crypto, 24/7 | Live via Kraken (websocket) or Live polling (15s) |
| Upstox (optional, Settings) | Live NSE/BSE quotes **only** when connected and the token is valid | Live via Upstox |

If a source fails, the app keeps the last cached price (still labelled
delayed) or shows "Price unavailable" — it never invents a price, and a
Yahoo price is never labelled "Upstox live".

NSE/BSE market hours are 09:15–15:30 IST, Monday–Friday. Outside those
hours stock prices are the last close / delayed quote, and the app says so.

## Upstox (optional, feed only)

1. In the app, go to **Settings**.
2. Create an Upstox API app in your Upstox developer console and generate
   an access token (it expires at 03:30 IST the next morning).
3. Paste the API key and access token in Settings and tap **Save Upstox**,
   then **Sync Upstox instruments**. This downloads Upstox's public
   instrument master and builds the symbol → instrument-key map needed
   for live quotes.
4. The token is stored **only on this device**, in EncryptedSharedPreferences.
   Nothing is hardcoded in this code, and nothing is sent anywhere except
   `api.upstox.com` and the public instrument-master download.
5. A 401 response marks the token expired and the app falls back to
   delayed data. Enter a fresh token each morning.

**Never** put a real API key or token in this repo, in a screenshot, or
in chat. If Upstox is not connected, the app works fully on delayed /
Kraken data.

## Building and installing (exact steps)

You need **Android Studio** (Hedgehog or newer) and JDK 17. No APK is
included in this repo — see "Why no APK?" below.

1. Open Android Studio → **Open** → select this folder
   (`trading-android-app/`).
2. Let Gradle sync finish (it downloads Android Gradle Plugin 8.5.2,
   Kotlin 1.9.24, Compose BOM 2024.06.00, Room 2.6.1, OkHttp 4.12.0).
   You need internet for this first sync.
3. Connect an Android phone with **USB debugging** on, or start an
   emulator (API 34 recommended).
4. Click **Run ▶** (or Build → Build App Bundle(s) / APK(s) →
   Build APK(s) for a debug APK).
5. For a debug APK file: after building, it is at
   `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone
   and tap it to install (allow "Install unknown apps" for your file
   manager if asked). This is a **debug** build for your own learning —
   it is not a Play Store release.

To run the unit tests (trading maths only, no phone needed):
`./gradlew :app:testDebugUnitTest` — or see `jvm-test` notes in BUILD.md.

## Why no APK? — and how to get one without Android Studio

This code was written and its trading logic was verified on a machine
with **no Android SDK** (the SDK manager could not reach Google's
servers through that machine's proxy, so platform tools / API 34 could
not be installed there). A later attempt to build a debug APK on that
same machine also failed: the Android SDK packages were downloaded
successfully, but every Java process in that environment fails to talk
to a local Gradle daemon (a plain Java localhost socket test returns
byte 109 instead of the data sent, while the same test in Python
works) — so a local Gradle build is not possible there. Faking an APK
would have been dishonest, so none is included. The trading engine
itself was compiled with the standalone Kotlin compiler and all of
its tests passed (see BUILD.md).

**You do not need Android Studio to get an APK:** see
`GITHUB_ACTIONS_APK.md`. Push this project to GitHub and the workflow
in `.github/workflows/android.yml` builds `app-debug.apk` in the
cloud; you download it from the Actions run's Artifacts and install
it on your phone by sideloading. Open the project in Android Studio
as above if you ever get Android Studio — that path still works.

## Play Store publishing — NOT included

Publishing to Google Play needs a Google Play developer account
(one-time $25 fee, your own account, your own signing key and privacy
policy). That is a separate step for you, if you ever want it. This
project is set up for sideloading / personal learning use, not for
store release.

## Expanding the catalog

Edit `app/src/main/java/com/dummytrading/app/data/Catalog.kt`:

- Stocks: add `stock("SYMBOL", "Company Name")` to the `stocks` list.
  The Yahoo symbols are generated automatically (`.NS` / `.BO`).
- Crypto: add a `CatalogItem(..., krakenPair = "XXXUSD")` — the pair must
  be a pair Kraken actually lists, otherwise that coin will show
  "Price unavailable" (one bad pair never poisons the whole feed).

The full NSE (2,629) / BSE (5,245) lists are deliberately **not** bundled
in v1: a starter list of ~100 popular names + 16 cryptos keeps the app
fast on a phone and keeps Yahoo quote calls small. Add names as you
study them.

## Limitations, honestly

- Stock prices without Upstox are delayed ~15 minutes. Delayed ≠ live.
- Crypto websocket could not be verified in the build sandbox
  (outbound `wss:` was blocked there); the app falls back to 15s
  polling and labels the active mode — check the label on your phone
  at night.
- Upstox live quotes depend on the public instrument master download
  and a valid daily token; they are coded but were not tested against
  a real token (correctly — no token was shared).
- Price sources can throttle datacentre / bulk requests; the app
  falls back to the last cached price and says so.
- Data lives only on the phone (Room). Uninstalling the app deletes
  your dummy portfolio. There is no cloud backup and no sync.
- Charge parts are individually rounded, so they can differ from the
  unrounded total by ₹0.01.

## Project layout

```
settings.gradle.kts, build.gradle.kts, gradle.properties
gradle/wrapper/gradle-wrapper.properties   (Gradle 8.5)
app/build.gradle.kts
app/src/main/AndroidManifest.xml           (INTERNET only)
app/src/main/java/com/dummytrading/app/
  MainActivity.kt, DummyTradingApp.kt
  trading/TradingEngine.kt                 (pure maths, JVM-tested)
  data/AppDatabase.kt                      (Room entities + DAO)
  data/Catalog.kt                          (starter catalog)
  prices/PriceModels.kt                    (Quote, PriceMode, market hours)
  prices/YahooPriceProvider.kt             (delayed stocks)
  prices/KrakenPriceProvider.kt            (crypto WS + polling)
  prices/UpstoxFeed.kt                     (FEED ONLY, encrypted token)
  prices/PriceRepository.kt                (single price cache)
  ui/TradingViewModel.kt                   (portfolio logic)
  ui/Screens.kt, ui/MoreScreens.kt         (Compose screens)
  ui/Theme.kt
app/src/main/res/                          (theme, strings, launcher icon)
app/src/test/.../TradingEngineTest.kt      (JUnit tests)
```
