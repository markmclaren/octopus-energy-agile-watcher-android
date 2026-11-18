# ⚡ Octopus Energy Agile Watcher (Android)

An Android recreation of the iOS **[Octopus Energy Agile Watcher](https://www.robertsammons.com/agile-watcher/)** app ([App Store `id1499880851`](https://apps.apple.com/gb/app/octopus-energy-agile-watcher/id1499880851)), written in **Kotlin** with **Jetpack Compose (Material 3)**, powered by the **[Octopus Energy Kraken REST & GraphQL APIs](https://developer.oegb-kraken.energy/rest/)**, and built with **Docker, Docker Compose, and GitHub Actions** following the zero-SDK architecture of [`markmclaren/tuya-water-heater-app`](https://github.com/markmclaren/tuya-water-heater-app).

> **Note on Open Source Status:** The original iOS *Octopus Energy Agile Watcher* by Rob Sammons is a proprietary, closed-source commercial iOS/macOS/watchOS app. This repository is a clean-room native Android implementation built from the public Octopus Energy Kraken API documentation and the iOS app's published screenshots and feature specifications.

---

## ✨ Features (Modeled on Agile Watcher v12.0.1 for iOS)

1. **🔑 Onboarding & Automatic Account/Meter Discovery (`OnboardingScreen`)**
   - Enter only your Octopus API key (`sk_live_...`) and tap **Find my details →** — the app uses the Kraken GraphQL API (`obtainKrakenToken` → `viewer.accounts`) and REST API (`/v1/accounts/{account_number}/`) to automatically discover your home address, Electricity MPAN & meter serial, active Agile tariff & UK DNO region, and optional Gas MPRN, meter serial & tariff.
   - **No API Key Needed for Live Prices:** You can also skip straight to watching public half-hourly Agile rates for any of the 14 UK GSP regions (`A`–`P`).

2. **⚡ Agile Prices Dashboard (`DashboardScreen`)**
   - **Hero Current Price Card:** Live half-hour price (`p/kWh`), Next Half Hour price, Average for Day, last-checked timestamp, **↓ Earlier Prices** toggle, and 1-tap Share button.
   - **Half-Hour Rates List:** Grouped by day (`Sun, Oct 25`, `Mon, Oct 26`) with automatic highlighting for the current slot (magenta-pink), lowest price of the day (green), plunge pricing below `0p/kWh` (cyan bolt `⚡`), and high-cost threshold alerts. Tap any slot to schedule a local reminder or share that slot's rate.

3. **⏱️ Best Times Finder & Price Chart (`BestTimesScreen`)**
   - Find the cheapest consecutive **1 hour**, **2 hours**, or **4 hours** window across **Today** and **Tomorrow**.
   - One-tap **🔔 Remind Me** button schedules an Android notification alarm for the start of your chosen window.
   - Interactive **48-bar Half-Hour Price Chart** (`Today` / `Tomorrow` toggle) with zero-baseline support for negative plunge rates and deep magenta-pink highlighting over the cheapest complete window.

4. **📊 Electricity Consumption — 30-Day Overview & Half-Hour Day Detail (`ConsumptionScreen`)**
   - **Last 30 Days Hero Card:** Total 30-day cost (`£`) & usage (`kWh`), Last 7 Days summary, Average reported day, standing charge inclusion note, and Share button.
   - **Usage over 30 days Bar Chart** + **Daily usage** list with automatic labelling for partial readings (`Partial readings · 3 received`) and full days (`Avg. 7.90p/kWh · 48 readings`).
   - Tap any day to drill down into the **Day Detail View** featuring the **Usage through the day** 48-slot bar chart and individual **Half-hour readings** paired with the exact Agile unit rate (`p/kWh`) and cost (`£`) at that half hour.

5. **🔥 Optional Gas Consumption & Tariff Setup (`ConsumptionScreen` & `GasTariffSetupDialog`)**
   - View 30 days of gas usage (`m³` for SMETS2 with standard UK calorific conversion `m³ × 1.02264 × 39.5 / 3.6 → kWh`, or `kWh` for SMETS1) and estimated daily/30-day costs.
   - Built-in **Setup Tariff** dialog inside the Hero card to customize your gas unit rate (`p/kWh`), standing charge (`p/day`), and meter units.

6. **📱 Android Home Screen Widget (`AgileRateWidgetProvider`)**
   - Displays Current Agile Rate, Next Half-Hour Rate, and Today's Average right on your Android Home Screen.

---

## 🛠️ Setup & Zero-SDK Docker Build

### Prerequisites

- **Docker Desktop** (for building locally — no local Java, Gradle, or Android SDK needed)
- Optional: An Octopus Energy account & API key from [Octopus Developer Settings](https://octopus.energy/dashboard/new/accounts/personal-details/api-access)

### 1. Optional Build-Time Configuration

You can enter your API key directly inside the app UI on first launch, or optionally pre-populate it at build time via `local.properties` (gitignored):

```bash
cp local.properties.example local.properties
```

### 2. Run Python Logic & Live API Verification Tests

```bash
python3 test_app_logic.py
```

### 3. Build the APK with Docker Compose

**Debug APK:**
```bash
./build_with_docker.sh
```
Outputs `app-debug.apk` in the project root.

**Release APK:**
```bash
./build_release_with_docker.sh
```
Outputs `app-release.apk` in the project root.

---

## 📁 Project Structure

```text
octopus-energy-agile-watcher-android/
├── .github/workflows/
│   └── build.yml                       # GitHub Actions CI & Release APK workflow
├── app/src/main/java/com/example/agilewatcher/
│   ├── data/
│   │   ├── AgileModels.kt              # GSP regions (A-P), Best Times, SMETS2 gas conversion
│   │   ├── OctopusApiClient.kt         # Kraken GraphQL auto-discovery & REST v1 client
│   │   └── AgileRepository.kt          # SharedPreferences persistence & concurrent refresh
│   ├── ui/
│   │   ├── AgileViewModel.kt           # StateFlow UI state & Best Times calculator
│   │   ├── AgileReminderReceiver.kt    # AlarmManager notification receiver for Remind Me
│   │   ├── AgileRateWidgetProvider.kt  # Android Home Screen AppWidgetProvider
│   │   ├── screens/
│   │   │   ├── OnboardingScreen.kt     # Welcome & API key auto-discovery (Screenshot 01)
│   │   │   ├── DashboardScreen.kt      # Current price & half-hour list (Screenshot 02)
│   │   │   ├── BestTimesScreen.kt      # 1h/2h/4h finder & 48-bar chart (Screenshot 03)
│   │   │   ├── ConsumptionScreen.kt    # 30-day & Day Detail Electric/Gas (Screenshots 04-06)
│   │   │   └── SettingsScreen.kt       # Account, Region, Gas Tariff & Theme preferences
│   │   └── theme/
│   │       ├── Color.kt                # Signature Agile Watcher Pink & Light/Dark palettes
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── MainActivity.kt                 # Floating 4-tab capsule navigation bar
├── Dockerfile                          # Isolated JDK 17 + Gradle 8.11.1 + Android SDK 35
├── docker-compose.yml                  # Persistent Gradle cache volume
├── build_with_docker.sh                # One-command Debug APK builder
├── build_release_with_docker.sh        # One-command Release APK builder
├── local.properties.example            # Template for optional build-time credentials
└── test_app_logic.py                   # Unit & live Octopus API verification suite
```
