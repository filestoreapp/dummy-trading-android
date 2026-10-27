# Get the APK without Android Studio — via GitHub Actions

You do **not** need Android Studio for this path. GitHub builds the APK
for you in the cloud, and you download it to your phone.

This is a **debug APK for your own learning only**. It is not a Play
Store release, and it is not signed for publishing.

## Step 1 — Put this project on GitHub

1. On github.com, create a **new, empty repository** (for example
   `dummy-trading-android`). Do **not** add a README, licence or
   `.gitignore` when creating it — this folder already has its own.
2. On your computer, in this project folder, run:
   ```
   git init
   git add .
   git commit -m "Dummy trading Android app"
   git branch -M main
   git remote add origin https://github.com/<your-username>/dummy-trading-android.git
   git push -u origin main
   ```
   Replace `<your-username>` and the repository name with yours.
   If you do not have git on your computer, use GitHub Desktop, or
   upload the whole folder through the GitHub website (Add file →
   Upload files).

## Step 2 — Let GitHub build the APK

1. Pushing to `main` automatically starts the workflow
   `.github/workflows/android.yml` (or go to your repo → **Actions** →
   **Build Android debug APK** → **Run workflow**).
2. Wait for the green tick. It takes a few minutes the first time,
   because it downloads the Android SDK, Gradle and all libraries.
3. If a step fails, open that failed step in the Actions log — the
   error there is the real error. Do not guess; copy it to Muse.

## Step 3 — Download the APK

1. In your repo → **Actions** → open the successful run
   **Build Android debug APK**.
2. Scroll to **Artifacts** and download
   `dummy-trading-debug-apk`.
3. Unzip it if needed. Inside is `app-debug.apk`.

## Step 4 — Install it on your phone

1. Copy `app-debug.apk` to your Android phone (USB, WhatsApp to
   yourself, Google Drive, or any file transfer you already use).
2. On the phone, tap the APK file to install.
3. If Android asks, allow **Install unknown apps** for the app you
   are installing from (for example, your file manager or browser).
   This is normal for sideloading — you are installing an app
   outside the Play Store, so only install an APK you built yourself
   from this repository.
4. Open **Dummy Trading**. On first run you get a fresh virtual
   wallet of **₹10,00,000** and empty holdings.

## Important — read this before you rely on it

- **Stock prices without Upstox are delayed about 15 minutes.**
  The app labels every price. Delayed is not live.
- **Crypto websocket may fall back to 15-second polling** and the
  app labels the active mode — check the label at night.
- **Upstox is optional and feed-only.** Enter its API key and daily
  token only in the app's Settings, on the phone. Never put a key or
  token in this repository, in a screenshot, or in chat.
- **Uninstalling the app deletes your dummy portfolio.** Data lives
  only on the phone (Room database). There is no cloud backup and no
  sync to the web version.
- **No real money, no real orders.** Dummy trades never leave the
  phone. There is no broker order-placement code in this project.
- The APK artifact on GitHub is kept for **30 days**. After that,
  re-run the workflow (Actions → Run workflow) to build a fresh one.
- If you ever want Play Store publishing, that is a separate step
  needing your own Google Play developer account ($25 one-time),
  signing key and privacy policy. This workflow does not do that.

## If you do get Android Studio later

The original path still works: open this folder in Android Studio
(JDK 17), let Gradle sync finish, then Build → Build App Bundle(s) /
APK(s) → Build APK(s). The debug APK is then at
`app/build/outputs/apk/debug/app-debug.apk`.
