# Build verification notes (what was actually tested)

## Environment at build time

- VM had **no Java, no Gradle, no Android SDK** at the start.
- JDK 17 (Temurin 17.0.21) and the standalone Kotlin compiler 1.9.24 were
  downloaded to a tools folder outside this project and used only for
  JVM testing — they are not part of this app.
- Android SDK install was attempted twice and failed both times:
  `sdkmanager` threw a proxy-related exception while fetching Google's
  repository list (`HttpURLConnection.doTunneling` / `NoSuchElementException`),
  so `platform-tools`, `platforms;android-34` and `build-tools;34.0.0`
  could not be installed in that sandbox. That is why **no APK is
  included**. Do not claim an APK exists until Android Studio (or a
  working SDK) has actually built `app-debug.apk`.

## What WAS verified

The pure trading engine (`TradingEngine.kt`, no Android dependency) was
compiled with the standalone Kotlin compiler and run on the JVM. All
checks passed:

- Starting cash = ₹10,00,000
- NSE charges on ₹2,403.60 gross = ₹3.70 (brokerage ₹0.72, STT ₹2.40)
- BSE − NSE charge difference on ₹1,00,000 = ₹0.35
  (exchange-txn difference ₹0.30 + 18% GST on it = ₹0.354, rounded)
- Crypto buy charges on ₹5,000 = ₹12.50; sell charges = ₹62.50 (fee + 1% TDS)
- Oversell rejected; insufficient cash rejected; valid trades accepted
- Average price: 2 @ 1200 + 2 @ 1300 = 1250
- Realised P&L: sell 1 @ 1300, avg 1200, charges 10 → ₹90
- Full simulation (2× RELIANCE NSE @ 1201.80, 1× RELIANCE BSE @ 1202.00,
  ₹5,000 BTC, then sell 1× RELIANCE NSE at the same price):
  cash ₹9,92,576.47, realised −₹1.67 (sell charges only) — matching the
  web platform's simulation within ₹0.02 of rounding.

Kotlin source files were also checked for balanced braces/parens/brackets
(all balanced) and the codebase was grepped for order-placement code:
the only matches are comments in `UpstoxFeed.kt` explaining that no
such code exists, by design. No API keys, tokens or personal data are
in the code.

## What was NOT verified

- A full Gradle / Android build (no SDK in the sandbox — see above).
  The first Android Studio Gradle sync is the real build test.
- Kraken websocket live streaming (outbound `wss:` blocked in the
  sandbox); polling fallback is implemented and labelled.
- Upstox live quotes against a real token (no token was shared, correctly).
- Yahoo / Kraken live quotes from a phone (network-dependent; the app
  handles failures by keeping the last cached price, labelled delayed).

## Reproducing the JVM test

Copy `TradingEngine.kt` and a small `main()` harness that asserts the
checks above, compile with `kotlinc` 1.9.24, and run with
`kotlin-stdlib.jar` on the classpath. The JUnit equivalent lives in
`app/src/test/java/com/dummytrading/app/TradingEngineTest.kt` and runs
with `./gradlew :app:testDebugUnitTest` in Android Studio.
