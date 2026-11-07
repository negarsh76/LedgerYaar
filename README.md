<div align="center">

# 💰 LedgerYaar · دفتر یار

**A personal-finance ledger for Android & iOS that records your bank transactions automatically from bank SMS and notifications.**

<img src="https://img.shields.io/badge/Kotlin_Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
<img src="https://img.shields.io/badge/Compose_Multiplatform-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" />
<img src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white" />
<img src="https://img.shields.io/badge/iOS-000000?style=flat-square&logo=apple&logoColor=white" />
<img src="https://img.shields.io/badge/AGP-9-3DDC84?style=flat-square" />
<img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" />

[📖 راهنمای کامل فارسی](README.fa.md)

</div>

---

## ✨ Features

- 🏦 **Automatic import from bank SMS.** Withdrawal and deposit messages from major Iranian banks (Melli, Mellat, Saman, Pasargad, Tejarat and others with a similar format) are parsed and recorded automatically. The parser has unit tests.
- 📲 **Reads banking-app notifications** (e.g. Blu Bank) through a notification listener, since Iranian banks don't offer public APIs for personal accounts.
- 🤖 **"What was this for?" prompt.** After each transaction you reply right from the notification (e.g. "lunch"). The app learns from your corrections, so a merchant like Snapp gets filed under Transport on its own next time.
- 📊 **Dashboard & analytics.** Income, expenses, cash flow, a yearly overview, and charts.
- 💳 **Loans & instalments, debts & receivables, budgets, savings goals, assets.**
- ⏰ **Daily reminders** for due instalments and debts, plus transactions waiting for review.
- 💾 **Backup** by email, with restore from a file.
- 🌐 **Bilingual (فارسی / English)**. The whole layout flips between RTL and LTR. Dates use the Jalali calendar.
- 🌿 **Themes**, including a forest theme, plus a custom icon and splash screen.

> SMS and notification reading is Android-only; iOS doesn't allow reading other apps' messages.

## 🏗️ Architecture

The project follows AGP 9's two-module layout:

```
composeApp/   Shared KMP library: UI, Room database, domain logic, Koin DI, i18n
  commonMain/   domain (model · usecase · analytics · autoimport · period), data (db · repository · backup), ui
  commonTest/   BankMessageParser & Jalali converter tests
androidApp/   Android app: MainActivity, SMS receiver, notification listener, reminders, icons
iosApp/       Xcode project (CocoaPods)
```

**Stack:** Kotlin 2.4 · Compose Multiplatform 1.12 · Room 2.8 (KMP) · Koin 4.2 · KSP · AGP 9.3 · Gradle 9.5

## 🚀 Getting started

```bash
git clone https://github.com/negarsh76/LedgerYaar.git
```

- **Android:** open in Android Studio and run the **androidApp** configuration (not `composeApp`).
- **iOS:** `cd iosApp && pod install`, then open `iosApp.xcworkspace` in Xcode.
- **Tests:** `./gradlew :composeApp:testDebugUnitTest`

## 📄 License

MIT

---

<p align="center">Made by <a href="https://github.com/negarsh76">Negar Shojaei</a></p>
