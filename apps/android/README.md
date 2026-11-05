<div align="center">

# Kosh

**Your money, understood. On your device.**

A native Android personal finance tracker for Indian bank SMS and statements.

![Platform: Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Language: Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](../../LICENSE)
![Status: Early development](https://img.shields.io/badge/status-early_development-D4AF37)

</div>

Kosh turns financial messages into a searchable transaction history, monthly spending summaries, and a queue for reviewing categories. Parsing, categorization, and storage happen on the device, without an account or cloud finance backend.

> **Development status:** Early development. Classification remains incomplete. Statement import is additive and atomic: original observations are retained, and uniquely matched source records contribute to totals once. Ambiguous matches stay visible and are reported; broader account identity still needs work.

## What Kosh does

- **Import financial SMS:** scan existing messages and receive new messages through Android's SMS broadcast.
- **Parse Indian bank formats:** dedicated HDFC, Kotak, and Slice parsers, plus a generic UPI fallback.
- **Explore monthly activity:** income and expense summaries, category breakdowns, daily spending trends, and per-account debit/credit activity.
- **Search your transaction trail:** browse transactions by date and filter the history.
- **Review categories:** classify unresolved transactions and remember category choices for matching merchants.
- **Understand the source:** inspect the original message and how a transaction was interpreted.
- **Import bank statements:** experimental parsers for Kotak CSV, HDFC XLS, and Slice XLSX, with an import confirmation step.

Parser coverage depends on the exact bank message or export format. This is not universal support for every SMS template or statement variant.

## A clearer view of your money

| Screen | Purpose |
| --- | --- |
| Overview | Monthly summaries, category spending, recent activity, and account activity |
| Trail | Searchable, filterable transaction history |
| Insights | Spending trends and comparisons |
| Review | Resolve uncategorized transactions |
| Transaction detail | Inspect source data and edit the category |

## Privacy and permissions

- Transactions, source SMS text, learned merchant categories, and source-reconciliation links are stored in a local Room database.
- The app manifest requests **READ_SMS** for historical import and **RECEIVE_SMS** for live capture.
- The app manifest declares no **INTERNET** permission. There is no finance-data upload or analytics integration in the current app code.
- Android application backup is disabled.
- The UI uses Google's downloadable Manrope font through Google Play services, with a system-font fallback. Font retrieval is separate from financial-data processing.
- Local storage is not currently encrypted by Kosh. Device security still matters, and uninstalling the app normally removes its local data.

Keep real SMS, statements, account details, database exports, and signing credentials out of issues, screenshots, and commits.

## Build and run

All commands below run from `apps/android`. The website and npm workspace are documented in the [root README](../../README.md).

### Requirements

- Android Studio with JDK **17**.
- Android SDK Platform **34** and the corresponding build tools.
- An Android **8.0 / API 26** or newer device or emulator.
- Network access for the initial Gradle and Maven dependency downloads.

### Get the implementation

Clone the repository and use the default `main` branch:

```bash
git clone https://github.com/theohalden/Kosh.git
cd Kosh/apps/android
```

Open `apps/android` (not the monorepo root) in Android Studio, let Gradle sync, and run the `app` configuration. Android Studio can configure the SDK path in the untracked `local.properties` file.

For a command-line build, configure `ANDROID_HOME` or `sdk.dir` in `local.properties`, then run:

```bash
bash gradlew testDebugUnitTest
bash gradlew lintDebug
bash gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `bash gradlew`.

To install on a connected device with Android platform tools:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant SMS permissions when prompted to use message import and live capture. Statement imports use Android's document picker.

## Architecture

Kosh uses Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, coroutines/Flow, and Room. HDFC XLS import uses Apache POI; Slice XLSX import reads worksheet XML from the ZIP container.

The main data path is:

**SMS or statement → bank parser → transaction mapping → local repository → categorization and transfer reconciliation → Compose UI**

```text
app/src/main/kotlin/dev/kosh/financetracker/
  core/          Models, SMS parsers, statement parsers, classifiers, finance calculations
  data/          Room entities and DAOs, repository, SMS reader and receiver
  di/            Dependency injection
  feature/       Overview, Trail, Insights, Review, detail, sync, statement import
  navigation/    Destinations and shared navigation state
  ui/            Components, theme, and rupee formatting
```

Money is represented using `BigDecimal`. SMS source identifiers support idempotent rescans of the same message; this does not provide full cross-source deduplication.

## Known limitations

- **Statement import boundaries:** imports add source observations without deleting existing records. Unique reference-backed SMS matches and one-to-one statement matches are linked; uncertain observations remain separate and may still inflate totals. Import results report unresolved SMS and older statement records. Two accounts at the same bank with identical suffixes cannot yet be distinguished.
- **Transfer detection is heuristic:** matching amount, direction, timing, and account suffix does not prove both accounts belong to the user.
- **Classification is incomplete:** debits generally become expenses; salary heuristics identify some income. EMI, investment, refund, and credit-card-payment semantics need further work.
- **Cross-source reconciliation is conservative:** SMS needs a compatible bank/account, amount, direction, nearby date, and shared reference. Unknown-bank alerts and ambiguous matches stay separate. Original source rows remain available in storage. Notification capture is not implemented.
- **Statement row identity:** identifiers use a hash of row fields plus an occurrence counter, preserving rows with missing/repeated references and exact duplicates on repeated import.
- **Import validation:** running balances and chronological order are checked before import. Malformed transaction rows reject the import. Slice workbooks follow worksheet relationships and have XML/ZIP safety limits. Additional bank export variants still need coverage.
- **Database upgrades:** schema 7 upgrades to schema 8 through an explicit migration preserving existing rows. Unsupported older schemas fail instead of clearing data. The migration does not automatically alter existing source links or categories.
- **Automated validation:** GitHub Actions runs unit tests (including Room database regression tests), Android lint, and the debug build. Prior PR notes report device testing; device behavior still needs separate validation.

See the [roadmap](../../docs/ROADMAP.md) for the next engineering priorities. Treat app totals as experimental and verify them against bank records.

## Contributing

Read [CONTRIBUTING.md](../../CONTRIBUTING.md) for setup, test expectations, and privacy-safe fixtures. Focused fixes and reproducible reports are welcome.

For vulnerabilities or financial-data exposure, follow [SECURITY.md](../../SECURITY.md).

## License

Kosh is licensed under the [MIT License](../../LICENSE).

Third-party dependencies retain their respective licenses.
