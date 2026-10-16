# ExpenseTrackerPro

Android app for tracking income and expenses: transactions, custom categories, dashboard with charts and insights.
Goal: production-quality portfolio project.

Path-scoped rules in `.claude/rules/` load with the files they cover: `i18n.md`, `ui-compose.md`, `database.md`, `scan-quickadd.md`, `structure.md`.

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Run all three before saying a task is done.

## Tech stack
- Kotlin 2.0.21, Jetpack Compose (Material 3), minSdk 24, targetSdk 36
- applicationId is `dev.pebblework.expensetracker`. It is permanent once the app is published, so never change it. It is separate from `namespace` and the Kotlin packages, which are still `com.example.expensetrackerpro`.
- Room 2.6.1 (KSP), Navigation Compose, Hilt 2.56.1
- Languages: English, Hindi, Spanish, French, German and Portuguese (Brazil), chosen per app with AppCompat.
- App lock: androidx.biometric (`BiometricPrompt` behind `presentation/lock/BiometricAuthenticator`), lock state in `presentation/lock/AppLockController`, settings in DataStore
- Charts: custom Compose Canvas components (no chart library) in `presentation/homeScreen/component/charts/`; shared drawing helpers in `ChartDrawing.kt`
- Bill scanning: ML Kit (Google Play services). Quick add: rules in pure Kotlin. No camera or microphone permission.
- Dependencies are declared in `gradle/libs.versions.toml`. Add new ones there, never hardcode versions in `app/build.gradle.kts`.

## Architecture rules (target state)
- **DI:** Hilt only. Use `@HiltViewModel` + `hiltViewModel()`. No ViewModel factories, no manual singletons, no dependencies created in `Application`.
- **Annotation processing:** KSP only (no kapt).
- **Layers:** presentation → domain ← data. The domain layer is pure Kotlin with no Android or Room imports.
- Repository interfaces live in `domain/repository`, implementations in `data/repository`.
- Room entities never reach the UI. Map them to domain models.
- Business logic (totals, savings %, insights) goes in use cases or the domain layer, not in Composables.
- Each screen has one ViewModel exposing `StateFlow<XxxUiState>`. User actions go through a sealed `XxxEvent` interface. Both types live in the feature's `uistate/` folder; the ViewModel takes events through `onEvent(XxxEvent)` and depends only on use cases.
- Composables are stateless where possible: state in, lambdas out.

## Low-level design rules
Apply these to all new and changed code. Existing violations are tracked under Known issues; fix them only when the task is about them.
- **SOLID:** one responsibility per class. Depend on interfaces from `domain/`, never on implementations.
- **Use cases:** one operation each. When a ViewModel needs more than ~5 dependencies, add a facade use case that combines them (e.g. `GetDashboardUseCase`).
- **Value types over primitives:** money is `Money` (whole minor units, hundredths of the currency's unit, as `Long`; never `Double`), IDs are typed (`TransactionId`, `CategoryId`: `@JvmInline value class` over `Int`; wrap in `data/mapper/`, unwrap only for Room, navigation arguments and lazy-list keys), dates go through `DateRange` / `TimeProvider`.
- **No stringly-typed data:** fixed sets are enums or sealed types (e.g. `CategoryIcon` enum stored via a Room `TypeConverter`), not free strings.
- **Data access:** one DAO per aggregate, and a repository depends only on the DAOs it reads or writes. Don't store the same data twice; get related data through a JOIN or `@Relation`.
- **Errors:** expected failures are typed. Writes return `Outcome<T>` (`Success(value)` / `Failure(DomainError)`, both in `domain/error/`), not `kotlin.Result`, `Boolean` or a nullable. Repositories wrap database writes in `storageOutcome { }`, which catches `SQLiteException` only; ViewModels turn the error into a `@StringRes` with `toMessageRes()`. No silent `catch {}`, no catch-all of `Throwable`, and no exceptions used for control flow.
- **Time:** never call `System.currentTimeMillis()` or `Calendar.getInstance()` outside `data/time/`. Inject `TimeProvider`.
- **Global objects:** `object` is only for stateless pure helpers. Anything that reads the clock, locale or other state is an injectable class.
- **Immutability:** domain models and UI state are immutable `data class`es with `val`s and read-only `List`s.

## Conventions
- Money: format only through the injected `utils/CurrencyFormatter` (`LocalCurrencyFormatter.current` in Composables), never by concatenation like `"₹$amount"`. The CSV keeps its plain decimal (`1130.00`).
- Currency: one for the whole app, a display setting (Settings → Currency, DataStore `currency_code`); `AppCurrency` lists 24, with 0 or 2 decimals. The currency decides the symbol, grouping, decimals and short units; the app's language decides the separators and the symbol's place. Digits are always Western.
- No `!!`. Handle nulls explicitly.
- Every new ViewModel or use case gets unit tests in `app/src/test`.

## Architecture decisions
Read `docs/architecture-decisions.md` before changing one of these. Each has its reasoning there; change one only with a reason that beats it.
- Money is whole minor units in a `Long` (column still `amountPaise`).
- The currency formatter is an injected class, reading `CurrencyProvider` on every call.
- The currency is a display setting; nothing is converted.
- The currency is read under the splash (`PrepareAppUseCase`).
- Category name and icon are read through a JOIN, never copied onto the transaction.
- Expected failures are `Outcome<T>` with a `DomainError`.
- One DAO per aggregate.
- `AppLockController` owns the app lock.
- A backup holds transactions and categories only, never settings or the app lock.
- A restore re-reads and re-validates the file on confirm and replaces both tables in one transaction.
- A scanned bill becomes a `TransactionDraft`, and only a draft reaches the form.
- The photo of a bill is never kept.
- A doubtful value is left out rather than guessed.
- A built-in category is known by its key; its name follows the app's language.
- The language change is applied after the Language screen has closed.
- Theme and language changes are shown with a snapshot at the root.
- Quick add is another source of drafts: a draft never overwrites what the user typed, and nothing is saved without Save (Save all included).
- What quick add learns is a cache, not the user's data.
- Several transactions from one sentence are saved all or none.
- An entry mode is launched once.
- IDs are typed in the domain and `Int` in Room.

## Known issues (remove each line once fixed)
- Bill scanning reads rupee words only (₹, Rs, INR in `BillKeywords`). With another currency it still reads bare numbers, but a total written with another symbol may be missed.
- `AddEditTransactionViewModel` has 12 dependencies. Fold them into facades.
- `SettingsViewModel` has 12 dependencies. Fold them into facades.
- The Add/Edit transaction form isn't kept in saved state, so typed values are lost on process death. So are the quick-add batch (its row choices) and the rest of a review one by one.
- `DraftAssistController` has 14 dependencies. Fold them into helpers.

## Workflow
- Never commit to `main`. Use branches like `refactor/hilt` or `feature/theme-toggle`.
- One task per session. Propose a plan before large refactors.
- Refactors must not change behavior or UI.
