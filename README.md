# Tinward / Kosh monorepo

One repository for the native Android finance tracker and the Tinward marketing website. Tinward is the working marketing name; the Android app retains its existing Kosh identity, package IDs, signing setup and database.

## Repository layout

```text
apps/
  android/              Independent Gradle project; open this in Android Studio
    app/                Kotlin, Compose, Room, parsers and tests
    gradle/             Gradle wrapper
    sample-data/        Synthetic Android fixtures
    gradlew
    settings.gradle.kts
    README.md
  web/                  Next.js marketing website and early-access backend
    src/                Pages, components, content and server routes
    public/             Website assets
    tests/              Sample-data and waitlist tests
    supabase/           Waitlist SQL migrations
    scripts/            Website-specific verification utilities
    README.md
scripts/
  android.mjs           Root command adapter for the Android Gradle project
docs/                   Shared project documentation and Android roadmap
.github/workflows/      Separate Android and web validation jobs
package.json            Root npm workspace and developer commands
package-lock.json       Single JavaScript dependency lockfile
```

Each app owns its implementation and build configuration. npm workspaces manage the web dependencies; Gradle manages Android. There is no duplicated web project or ZIP source archive. Shared libraries should only be introduced when there is actual reusable code.

## Website: start here

Use Node.js 24 or newer and npm 11 or newer. `.node-version` selects Node 24 for CI.

From the repository root:

```bash
npm ci
cp apps/web/.env.example apps/web/.env.local
npm run dev
```

Open **http://localhost:3001**. Existing local configuration is preserved when upgrading this checkout; do not overwrite an existing `.env.local`.

The development waitlist uses temporary process memory and a local email outbox. It does not deliver external email. Production requires Supabase, Resend, real secrets and an explicitly configured site URL; see [apps/web/README.md](apps/web/README.md). No domain or production deployment is configured by this repository restructure.

## Android

Install JDK 17, Android SDK platform 34 and build-tools 34. Configure `ANDROID_HOME` or create the ignored `apps/android/local.properties` with `sdk.dir` pointing to the SDK.

Open **`apps/android`** in Android Studio. Android development does not require npm.

```bash
cd apps/android
bash gradlew testDebugUnitTest lintDebug assembleDebug
```

From the repository root, the equivalent convenience command is:

```bash
npm run android:check
```

The debug APK is written to `apps/android/app/build/outputs/apk/debug/app-debug.apk`. Keep installed-device data safe: an in-place update requires the original signing key. Never uninstall or clear storage as a signing workaround. The separate Preview variant uses its own app ID.

See [apps/android/README.md](apps/android/README.md) for features, permissions, data integrity and native setup. Automatic bank-message support remains regional; broader financial planning and wealth capabilities are not universally implemented.

## Root commands

| Command | Scope |
| --- | --- |
| `npm run dev` | Web development server on localhost:3001 |
| `npm run check:web` | Web type checks, lint, tests, SQL migration checks and production build |
| `npm run build:web` | Web production build |
| `npm run android:check` | Android debug tests, lint and APK build |
| `npm run android:preview` | Android Preview tests, lint and APK build |

Both Android commands pass through to the Gradle wrapper in `apps/android`. They inherit `JAVA_HOME` and Android SDK environment variables. On Windows, Android can also be built directly using `apps\android\gradlew.bat` from the Android project directory.

## CI and contribution

- [Android workflow](.github/workflows/android.yml): runs when the Android project or its workflow/adapter changes.
- [Web workflow](.github/workflows/web.yml): installs from the root lockfile and verifies the web workspace when its sources, workspace configuration or workflow change.
- [Contributing](CONTRIBUTING.md), [Android roadmap](docs/ROADMAP.md), [security policy](SECURITY.md).

The public site uses synthetic financial examples. Keep real messages, bank statements, device databases, secrets, SDK paths and signing keys out of commits. Local caches, generated outputs and environment files are ignored.

## License

[MIT](LICENSE). Third-party dependencies retain their respective licenses.
