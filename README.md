# 🧪 BRD Rickest Rick Relay (v1.0)

> *“Listen to me, Morty. Just enter a shop name and a 4-digit PIN. Don’t overthink it.”*
[![Download APK](https://img.shields.io/badge/Download_APK-v1.0.0-green?style=for-the-badge&logo=android)](https://github.com/blackbird-citadel/BRD_RICKEST_RICK_RELAY/releases/download/v1.0.0/app-debug.apk)
![App Size](https://img.shields.io/badge/App%20Size-15.95%20MB-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-orange?style=for-the-badge)

---

## 📖 The Origin Story

It all started on a Friday morning. I was left in charge of my mom's shop. Every time a customer paid via **M-PESA / Pochi la Biashara**, the confirmation SMS went directly to her phone.

To verify payment, I had to call her every single time:
> *"Mom, did a Ksh 450 come through?"*

After the 5th call, customers started looking at me like I was running a secondary interrogation unit. They thought I didn't trust them! I stood there sweating under suspicious stares, calling my mom like a lost kid while trying to hold down a business.

That weekend, I snapped. I opened Android Studio, put on *Rick and Morty*, and engineered **BRD Rickest Rick Relay**—an automated SMS interceptor and forwarding gateway that broadcasts M-PESA payment receipts directly to enrolled shop employees in real time.

---

## ⚙️ Core Features

1. **Automated M-PESA & Pochi Parsing**: Captures payment SMS broadcasts, parses Transaction IDs (`TX: UIHCM72SXC`), amounts, and sender names, and dispatches clean alerts to enrolled shop attendants.
2. **Multi-Worker Enrollment**: Add, activate, or revoke shop staff dynamically from a clean UI dashboard.
3. **Daily & Monthly Revenue Counters**: Tracks daily transaction count and aggregate shop revenue locally without extra backend overhead.
4. **Multi-SIM Failover Dispatch**: Automatically detects active dual SIM slots to ensure continuous SMS dispatch during carrier network dips.
5. **Security PIN Layer**: Rick-themed authentication gate protecting device settings and staff configurations.
6. **Local Quota Tracker**: Built-in SMS balance tracking to prevent relay stalls.

---

## 📸 Screenshots

| Gateway Security | Merchant Dashboard | Proof of Relay (SMS) |
| :---: | :---: | :---: |
| Lock screen with PIN protection | Live revenue tracking & worker enrollment | Formatted SMS payload received by attendant |

---

## 🛠️ Technical Overview

* **Language**: Java / Android SDK (API 21+)
* **Architecture**: Event-driven `BroadcastReceiver` listening to `android.provider.Telephony.SMS_RECEIVED`
* **Concurrency**: Thread-safe atomic updates in `PreferencesManager`
* **Size**: Lightweight footprint (~16 MB total storage)

---

## 🚀 Architectural Improvements (Roadmap)

To elevate this project from a local utility to an enterprise merchant engine:

- [ ] **SMS Regex Engine Expansion**: Add robust regex matchers for broader carrier formats (Airtel Money, Equity Bank, T-Kash).
- [ ] **Dual-SIM Slot Selection**: Allow manual binding of default outgoing relay SIM slot.
- [ ] **Cloud Webhook Relay**: Send structured JSON payloads to a remote server/Telegram Bot alongside local SMS relay.
- [ ] **Encrypted Preference Storage**: Migrate `SharedPreferences` to AndroidX `EncryptedSharedPreferences` for improved PIN and credential security.
