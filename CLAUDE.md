# ExpenseTrackerPro

Android app for tracking income and expenses: transactions, custom categories, dashboard with charts and insights.
Goal: production-quality portfolio project.

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Run all three before saying a task is done.

## Tech stack
- Kotlin 2.0.21, Jetpack Compose (Material 3), minSdk 24, targetSdk 36
- applicationId is `dev.pebblework.expensetracker`. It is permanent once the app is published, so never change it. It is separate from `namespace` and the Kotlin packages, which are still `com.example.expensetrackerpro`.
- Room 2.6.1 (KSP), Navigation Compose, Hilt 2.56.1
- Languages: English and Hindi, chosen per app with AppCompat (`AppCompatDelegate.setApplicationLocales`). `MainActivity` is an `AppCompatActivity` and the app theme an AppCompat one for this; `res/xml/locales_config.xml` and `AppLanguage` list the languages; below Android 13 AppCompat stores the choice itself (`autoStoreLocales` in the manifest). Debug builds have the pseudo-locales (en-XA) for finding clipped layouts.
- App lock: androidx.biometric (`BiometricPrompt` behind `presentation/lock/BiometricAuthenticator`), lock state in `presentation/lock/AppLockController`, settings in DataStore
- Charts: custom Compose Canvas components (no chart library) in `presentation/homeScreen/component/charts/`; shared drawing helpers in `ChartDrawing.kt`
- Bill scanning: ML Kit Document Scanner and Text Recognition v2 (Latin), both the Google Play services (unbundled) variants; Tasks are awaited with `kotlinx-coroutines-play-services`. No camera permission.
- Dependencies are declared in `gradle/libs.versions.toml`. Add new ones there, never hardcode versions in `app/build.gradle.kts`.

## Package structure (`com.example.expensetrackerpro`)
- `data/local/` → Room: `entity/`, `dao/`, `expenseDatabase/`, `model/` (query result classes)
- `data/local/dao/` → one DAO per aggregate: `TransactionDao` (transaction CRUD, range and search reads), `CategoryDao` (category CRUD, usage, rename, reassign-then-delete), `DashboardDao` (report queries, reads only); plus `AppDataDao` ("Delete all data", and the whole-table read and replace behind backup and restore) and `SampleDataDao` (debug sample data)
- `data/mapper/` → entity ↔ domain mappers; `StorageErrorMapper` (`storageOutcome { }`) turns a `SQLiteException` into a `DomainError`
- `data/repository/` → repository implementations
- `data/files/` → `DocumentFiles`: reads and writes the documents the user picks in the system file screens, through `ContentResolver`; the one place a picked Uri is opened
- `data/export/` → `FileExportRepositoryImpl`: writes the exported CSV (UTF-8 with a BOM) through `DocumentFiles`
- `data/backup/` → the backup file: `BackupDto` (the JSON format, kotlinx.serialization), its mappers, and `BackupRepositoryImpl` (write, read, restore)
- `data/scan/` → `MlKitBillTextReader` (text recognition, then deletes the photo), `BillLineAssembler` (joins recognized pieces into the bill's lines), `GmsBillScannerAvailability`
- `data/language/` → `AppLanguageRepositoryImpl` (the one place the app's language is read from and handed to AppCompat), `AndroidDeviceLanguages` (the phone's own languages), `AndroidAppLocaleProvider`; `data/category/` → `ResourceDefaultCategoryNames` (built-in category names from string resources, in any language asked for)
- `data/preferences/` → DataStore-backed repositories (theme, app lock, user profile); `data/security/` → Android biometric availability
- `domain/` → `models/` (typed IDs in `Ids.kt`), `error/` (`DomainError`, `Outcome`), `repository/` (interfaces), `security/` (device capability interfaces), `scan/` (`BillTextReader`, `BillScannerAvailability`), `locale/` (`AppLocaleProvider`, `DeviceLanguages`), `category/` (`DefaultCategoryNames`), `usecase/<feature>/` (one class per operation, `operator fun invoke`)
- `domain/usecase/scan/` → `ScanBillUseCase` (read, then `ParseBillUseCase`), the parsers it delegates to (one per rule: amount, title, date, note), and `BillKeywords`: every word list and limit the parsers use, en-IN for now
- `presentation/<feature>/` → Screen, ViewModel, `components/`, `uistate/`
- `presentation/common/category/editor/` → the add/edit category sheet shared by Manage Categories and Add/Edit Transaction: its state, events and `CategoryEditorReducer`; the host ViewModel keeps the state and wraps the events
- `presentation/transactions/addEditTransactions/draft/` → `TransactionDraftReducer`: puts a `TransactionDraft` into the form and tracks which fields still hold its values (`DraftFill` in `uistate/`); `scan/` → `rememberBillScanner`, the one place Google's scanner screen is launched
- `presentation/common/error/` → `DomainError.toMessageRes()`, the one place an error becomes a message
- `presentation/exportBackup/` → Settings → Export & backup: the period to export and the "Save as" picker; the CSV itself is built in `domain/usecase/export/`. `BackupController` owns creating a backup and restoring from one (the Backup card and the confirmation sheet); the ViewModel shows its state and forwards its events
- `presentation/language/` → Settings → Language: the list, the Preview card (`LocalizedContent` draws it in another language without switching the app) and the bilingual apply button. The Undo snackbar is on Settings, owned by `presentation/settings/LanguageChangeController`
- `presentation/common/date/` → `LocalDateFormatter`: how Composables reach the injected `DateFormatter`
- `presentation/lock/` → lock screen, the biometric prompt wrapper, and `AppLockController` (lock state, prompt flow, background timing); `MainViewModel` shows its state and forwards the lock events
- `ui/theme/` → colors, typography, theme
- `utils/` → shared helpers

## Architecture rules (target state)
- **DI:** Hilt only. Use `@HiltViewModel` + `hiltViewModel()`. No ViewModel factories, no manual singletons, no dependencies created in `Application`.
- **Annotation processing:** KSP only (no kapt).
- **Layers:** presentation → domain ← data. The domain layer is pure Kotlin with no Android or Room imports.
- Repository interfaces live in `domain/repository`, implementations in `data/repository`.
- Room entities never reach the UI. Map them to domain models.
- Business logic (totals, savings %, insights) goes in use cases or the domain layer, not in Composables.
- Each screen has one ViewModel exposing `StateFlow<XxxUiState>`. User actions go through a sealed `XxxEvent` interface. Both types live in the feature's `uistate/` folder; the ViewModel takes events through `onEvent(XxxEvent)` and depends only on use cases.
- Each screen is split in two: `XxxScreen` obtains the ViewModel and collects state, `XxxContent(state, onEvent, navigation lambdas)` is the stateless layout. Only purely visual state (animations, scroll, dropdown expansion) stays in `remember`.
- Composables are stateless where possible: state in, lambdas out.

## Low-level design rules
Apply these to all new and changed code. Existing violations are tracked under Known issues; fix them only when the task is about them.
- **SOLID:** one responsibility per class. Depend on interfaces from `domain/`, never on implementations.
- **Use cases:** one operation each. When a ViewModel needs more than ~5 dependencies, add a facade use case that combines them (e.g. `GetDashboardUseCase`).
- **Value types over primitives:** money is `Money` (whole paise as `Long`, never `Double`), IDs are typed (`TransactionId`, `CategoryId`: `@JvmInline value class` over `Int`; wrap in `data/mapper/`, unwrap only for Room, navigation arguments and lazy-list keys), dates go through `DateRange` / `TimeProvider`.
- **No stringly-typed data:** fixed sets are enums or sealed types (e.g. `CategoryIcon` enum stored via a Room `TypeConverter`), not free strings.
- **Data access:** one DAO per aggregate (`TransactionDao`, `CategoryDao`, `DashboardDao`), and a repository depends only on the DAOs it reads or writes. Don't store the same data twice; get related data through a JOIN or `@Relation`.
- **Errors:** expected failures are typed. Writes return `Outcome<T>` (`Success(value)` / `Failure(DomainError)`, both in `domain/error/`), not `kotlin.Result`, `Boolean` or a nullable. Repositories wrap database writes in `storageOutcome { }`, which catches `SQLiteException` only; ViewModels turn the error into a `@StringRes` with `toMessageRes()`. No silent `catch {}`, no catch-all of `Throwable`, and no exceptions used for control flow.
- **Time:** never call `System.currentTimeMillis()` or `Calendar.getInstance()` outside `data/time/`. Inject `TimeProvider`.
- **Global objects:** `object` is only for stateless pure helpers. Anything that reads the clock, locale or other state is an injectable class.
- **Immutability:** domain models and UI state are immutable `data class`es with `val`s and read-only `List`s.

## Conventions
- Money: format only through the shared en-IN currency formatter, `utils/CurrencyFormatter` (e.g. `-₹1,00,000.00`). Never use string concatenation like `"₹$amount"`.
- Dates: format via the shared date utility, `utils/DateFormatter` (an injected class that writes in the app's language). Never show raw millis.
- Colors: no Color literals outside `ui/theme`; use `MaterialTheme.colorScheme` or `MaterialTheme.extendedColors` (income/expense, chart and category palette colors). `Color.Transparent` is fine. Charts take theme colors directly as `Color`.
- UI strings go in `strings.xml`.
- Collect screen state with `collectAsStateWithLifecycle()`, never `collectAsState()`.
- No `!!`. Handle nulls explicitly.
- Every new ViewModel or use case gets unit tests in `app/src/test`.

## Languages (i18n)
- Every new string needs a translation in each `values-<language>/strings.xml`. Lint (`MissingTranslation` is an error) and `StringResourcesTest` (same keys, same format arguments, the language's plural quantities) fail otherwise. Translations are everyday words, not literal ones; `values-hi/strings.xml` starts with its glossary.
- No text in Composables, not even one word: `stringResource` only. Don't build a sentence from pieces in code; give the string positional arguments (`%1$s`) so a translation can reorder them.
- Mark `translatable="false"` what reads the same everywhere: the app name, formats made only of arguments (`%1$s · %2$s`), and the strings that stay English on purpose (the Language row's subtitle, the English half of the apply button and of the "Language changed" snackbar, its Undo), so that a language someone can't read can always be left.
- Never translated, because they are file formats or match printed text: CSV headers and Type words, the backup JSON, `BillKeywords`, the `DELETE` confirmation word.
- Built-in categories carry `builtInKey` (`BuiltInCategory`, stored by its `key`) and no name of their own: `LocalizeDefaultCategoriesUseCase` gives them the name of the app's language (`category_default_<key>`) at start and after every language change. Renaming one clears its key, so the user's name stays. Find a built-in by its key, never by its name.
- Text follows the app's language, not the phone's: read the locale through `AppLocaleProvider`, never `Locale.getDefault()`. Dates go through the injected `DateFormatter` (`LocalDateFormatter.current` in Composables). Digits stay Western; `formatIsoDate` stays `Locale.US`.
- To add a language: an `AppLanguage` entry, a line in `locales_config.xml`, its `values-<tag>/strings.xml`, and its plural quantities in `StringResourcesTest`.
- Check new screens in Hindi and in en-XA (long text): nothing clipped, nothing wrapped into its neighbour.

## Database
- Pre-release: every schema change bumps the version; `fallbackToDestructiveMigration` resets the data. From the first public release, write a tested Migration instead.
- Current version: 4 (added `categories.builtInKey`).
- Default categories (`domain/models/DefaultCategories`, one per `BuiltInCategory`) are seeded by `ExpenseDatabaseCallback`, in the app's language, in `onCreate` and again after a destructive migration, which empties the tables without calling `onCreate`.
- Debug builds have Settings → Developer → "Load sample data" (`data/sample/`), gated by `BuildInfo.isDebug`. "Delete all data" (Settings → Danger zone, `AppDataRepository`) is a user feature in every build.

## Architecture decisions
Settled during the LLD refactor. Change one only with a reason that beats the one given.
- **Money is whole paise in a `Long`** (`Money`), in the domain and in the database. `Double` loses paise when amounts are summed.
- **Category name and icon are read through a JOIN**, never copied onto the transaction, so renaming a category can't leave old names behind.
- **Expected failures are `Outcome<T>` with a `DomainError`**, not `Boolean`, a nullable or `kotlin.Result`: the caller has to handle the failure and can tell why it happened. Only `SQLiteException` is caught (`storageOutcome { }`); anything else is a bug and crashes.
- **One DAO per aggregate**, and a repository takes only the DAOs it uses, so a change to one table's queries doesn't touch the others.
- **`AppLockController` owns the app lock.** It is a plain injectable class, one per `MainViewModel`, so the lock rules are tested without a ViewModel and `MainViewModel` stays at five dependencies.
- **A backup holds transactions and categories only**, never theme, profile or app lock: a file must not be able to turn the lock off. The file is versioned JSON (`Backup.VERSION`); its DTOs stay in `data/backup/`, and `ValidateBackupUseCase` checks the whole file before anything is written.
- **A restore keeps only the picked file's Uri** (in `SavedStateHandle`), re-reads and re-validates it on confirm, and replaces both tables in one Room transaction (`AppDataDao.replaceAll`), so a failure at any point leaves the current data as it was.
- **A scanned bill becomes a `TransactionDraft`, and only a draft reaches the form.** Reading (`BillTextReader`), parsing (`ParseBillUseCase`, pure Kotlin) and filling the form (`TransactionDraftReducer`) are separate steps, so another source of drafts reuses the last one unchanged. A draft never replaces what the user entered, and nothing is saved without the user tapping Save.
- **The photo of a bill is never kept.** The reader deletes it once it has been read; only its Uri is held, in `SavedStateHandle`, until then, so a scan survives the process being killed. This is why the banner shows an icon and not the photo.
- **A doubtful value is left out rather than guessed**: a total next to "Sub Total", a date in the future or over a year old, an amount of ₹10,00,000 or more. Bill words and limits live only in `BillKeywords`; the parsers hold rules, not words.
- **A built-in category is known by its key, and its name follows the app's language.** The name is still stored, so every query and JOIN reads it as before; `LocalizeDefaultCategoriesUseCase` rewrites it when the language changes. A rename clears the key, which is how the user's name is never replaced; a built-in whose translated name another category of its type already has is left as it is. Backups carry the key as an optional field, so older files still restore.
- **The language change is applied after the Language screen has closed**, and the previous choice is kept in memory for the Undo. The switch recreates the activity while `MainViewModel` stays, and the app is never reported as backgrounded, so the app lock is not triggered.
- **IDs are typed in the domain and `Int` in Room.** `TransactionId` and `CategoryId` can't be mixed up; entities keep `Int`, so typing them needed no schema change.

## Known issues (remove each line once fixed)
- `AddEditTransactionViewModel` has 14 dependencies. Fold them into facades.
- The Add/Edit transaction form isn't kept in saved state, so typed values are lost on process death.

## Workflow
- Never commit to `main`. Use branches like `refactor/hilt` or `feature/theme-toggle`.
- One task per session. Propose a plan before large refactors.
- Refactors must not change behavior or UI.
