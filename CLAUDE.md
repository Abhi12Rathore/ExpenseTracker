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
- Languages: English, Hindi, Spanish, French, German and Portuguese (Brazil), chosen per app with AppCompat (`AppCompatDelegate.setApplicationLocales`). `MainActivity` is an `AppCompatActivity` and the app theme an AppCompat one for this; `res/xml/locales_config.xml` and `AppLanguage` list the languages; below Android 13 AppCompat stores the choice itself (`autoStoreLocales` in the manifest). Debug builds have the pseudo-locales (en-XA) for finding clipped layouts.
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
- `data/language/` → `AppLanguageRepositoryImpl` (the one place the app's language is read from and handed to AppCompat), `AndroidDeviceLanguages` (the phone's own languages), `AndroidAppLocaleProvider`; `data/currency/` → `AndroidRegionCurrency` (the phone region's currency, for the first-run default), `AndroidNumberSymbolsProvider` (a language's decimal and grouping separators, from `DecimalFormatSymbols`), `ResourceCurrencyNames` (currency names from string resources, in any language asked for); `data/category/` → `ResourceDefaultCategoryNames` (built-in category names from string resources, in any language asked for)
- `data/preferences/` → DataStore-backed repositories (theme, app lock, user profile, currency: `CurrencyRepositoryImpl` is also the `CurrencyProvider`); `data/security/` → Android biometric availability
- `data/motion/` → `AndroidMotionPreferences`: animations off when the animator duration scale is 0 ("Remove animations")
- `domain/` → `models/` (typed IDs in `Ids.kt`), `error/` (`DomainError`, `Outcome`), `repository/` (interfaces), `security/` (device capability interfaces), `motion/` (`MotionPreferences`), `scan/` (`BillTextReader`, `BillScannerAvailability`), `locale/` (`AppLocaleProvider`, `DeviceLanguages`, `NumberSymbolsProvider`), `currency/` (`AppCurrency`, `CurrencyProvider`, `RegionCurrency`, `CurrencyNames`, `NumberSymbols`), `category/` (`DefaultCategoryNames`), `usecase/<feature>/` (one class per operation, `operator fun invoke`)
- `domain/usecase/scan/` → `ScanBillUseCase` (read, then `ParseBillUseCase`), the parsers it delegates to (one per rule: amount, title, date, note), and `BillKeywords`: every word list and limit the parsers use, en-IN for now
- `presentation/<feature>/` → Screen, ViewModel, `components/`, `uistate/`
- `presentation/common/category/editor/` → the add/edit category sheet shared by Manage Categories and Add/Edit Transaction: its state, events and `CategoryEditorReducer`; the host ViewModel keeps the state and wraps the events
- `presentation/transactions/addEditTransactions/draft/` → `TransactionDraftReducer`: puts a `TransactionDraft` into the form and tracks which fields still hold its values (`DraftFill` in `uistate/`); `scan/` → `rememberBillScanner`, the one place Google's scanner screen is launched
- `presentation/common/error/` → `DomainError.toMessageRes()`, the one place an error becomes a message
- `presentation/exportBackup/` → Settings → Export & backup: the period to export and the "Save as" picker; the CSV itself is built in `domain/usecase/export/`. `BackupController` owns creating a backup and restoring from one (the Backup card and the confirmation sheet); the ViewModel shows its state and forwards its events
- `presentation/language/` → Settings → Language: the list, the Preview card (`LocalizedContent` draws it in another language without switching the app) and the bilingual apply button. The Undo snackbar is on Settings, owned by `presentation/settings/RegionalSettingsController`
- `presentation/currency/` → Settings → Currency: the pinned Preview card (the user's figures in the selected currency), search, Suggested (current + phone region) and all currencies, each with a sample in its own grouping; the confirmation that nothing is converted. Applying sets the currency at once and closes; the Undo snackbar is on Settings (`RegionalSettingsController`, which owns the Language and Currency rows)
- `presentation/common/list/` → `SectionTitle` and `GroupedRow`, the sectioned card list the Language and Currency pickers share
- `presentation/common/date/` → `LocalDateFormatter`: how Composables reach the injected `DateFormatter`
- `presentation/common/currency/` → `LocalCurrencyFormatter`: how Composables reach the injected `CurrencyFormatter`; `LocalAppCurrency` (provided in `MainActivity` from `MainUiState.currency`, so a change recomposes every screen) and `amountFormatKey()`, the key for any `remember` that holds formatted amounts
- `presentation/common/motion/` → `AppMotionHost` (the app root: theme reveal, theme cross-fade, the fade after a recreate), `ThemeTransitionController` (the tapped spot, from the theme option to the root), `ScreenSnapshotHolder` (a picture of the screen across a recreate), `LocalAnimationsEnabled`, `rememberCountUp`, `PressTracker`, `phoneDarkTheme()`
- `presentation/common/haptics/` → `rememberHaptics()`: confirm and toggle feedback through the View (Compose 1.7 has no such `HapticFeedbackType`)
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
- **Value types over primitives:** money is `Money` (whole minor units, hundredths of the currency's unit, as `Long`; never `Double`), IDs are typed (`TransactionId`, `CategoryId`: `@JvmInline value class` over `Int`; wrap in `data/mapper/`, unwrap only for Room, navigation arguments and lazy-list keys), dates go through `DateRange` / `TimeProvider`.
- **No stringly-typed data:** fixed sets are enums or sealed types (e.g. `CategoryIcon` enum stored via a Room `TypeConverter`), not free strings.
- **Data access:** one DAO per aggregate (`TransactionDao`, `CategoryDao`, `DashboardDao`), and a repository depends only on the DAOs it reads or writes. Don't store the same data twice; get related data through a JOIN or `@Relation`.
- **Errors:** expected failures are typed. Writes return `Outcome<T>` (`Success(value)` / `Failure(DomainError)`, both in `domain/error/`), not `kotlin.Result`, `Boolean` or a nullable. Repositories wrap database writes in `storageOutcome { }`, which catches `SQLiteException` only; ViewModels turn the error into a `@StringRes` with `toMessageRes()`. No silent `catch {}`, no catch-all of `Throwable`, and no exceptions used for control flow.
- **Time:** never call `System.currentTimeMillis()` or `Calendar.getInstance()` outside `data/time/`. Inject `TimeProvider`.
- **Global objects:** `object` is only for stateless pure helpers. Anything that reads the clock, locale or other state is an injectable class.
- **Immutability:** domain models and UI state are immutable `data class`es with `val`s and read-only `List`s.

## Conventions
- Money: format only through the injected `utils/CurrencyFormatter`, which writes the currency `CurrencyProvider` gives, or one passed in (the Currency screen's previews). Composables get it from `LocalCurrencyFormatter.current` (provided in `MainActivity`; previews get an INR/English default) and never format money without it. Never use string concatenation like `"₹$amount"`. Domain code that writes files (the CSV) keeps its plain decimal (`1130.00`) and doesn't use the formatter.
- Currency: one for the whole app, chosen in Settings → Currency and stored in DataStore (`currency_code`). On a fresh install it is the phone region's if the app offers it, else INR, and is stored at once. `AppCurrency` lists 24 currencies with 0 or 2 decimals (never 3: `Money` holds hundredths). The currency decides the symbol, the grouping (lakh/crore for INR only, thousands otherwise), the decimals (none for JPY: rounded for display, stored unchanged) and the short units (k/L/Cr for INR, K/M/B otherwise). The app's language decides the separators (`NumberSymbolsProvider`) and the symbol's place (`AppLanguage.symbolPlacement`: after with a space in es/fr/de, before in en/hi/pt-BR, with a space for pt-BR or a symbol made of letters). Digits are always Western.
- A Composable that keeps formatted amounts in `remember` keys it on `amountFormatKey()`, so a currency change with the screen open writes them again.
- The amount field holds plain digits with `.` for the decimal, whatever the language: `SanitizeAmountInputUseCase` takes the language's decimal key (`,` and `.` both, where the language writes a comma) and refuses a decimal point for a currency without decimals; `AmountGroupingTransformation` shows it as the formatter writes it.
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
- Text follows the app's language, not the phone's: read the locale through `AppLocaleProvider`, never `Locale.getDefault()`. Dates go through the injected `DateFormatter` (`LocalDateFormatter.current` in Composables). Digits stay Western; `formatIsoDate` stays `Locale.US`. `DateFormatter` writes times on the clock the phone is set to (`4:20 pm` or `16:20`), read through `HourFormatProvider` (`data/time/SystemHourFormatProvider`), not the one the language usually uses, and gives a month title and a month initial a capital where the language writes months without one.
- To add a language: an `AppLanguage` entry, a line in `locales_config.xml`, its `values-<tag>/strings.xml` (a region goes in the folder as `values-pt-rBR`) starting with a glossary, and its plural quantities in `StringResourcesTest` (Spanish, French and Portuguese also need `many`). `StringResourcesTest` fails if the three lists disagree.
- A language is matched by its language code alone, as Android does for resources: the app's only Portuguese is Brazil's, and a phone set to pt-PT gets it.
- German runs longest. A label that must stay on one line gets `maxLines = 1` with an ellipsis, and a short wording in every language rather than a layout that only fits English.
- Check new screens in Hindi and in en-XA (long text): nothing clipped, nothing wrapped into its neighbour.

## Motion
- Every animation is skipped when `MotionPreferences` says animations are off: Composables read `LocalAnimationsEnabled` (use `snap()` / `EnterTransition.None`, or start at the end value), plain classes take `MotionPreferences`.
- An animation never blocks input: overlays are only drawn, never take touches, and a second change ends the running one at once.
- The effective theme reads the phone's dark mode through `phoneDarkTheme()` / `phoneInDarkTheme()`, never `isSystemInDarkTheme()`.

## Database
- Pre-release: every schema change bumps the version; `fallbackToDestructiveMigration` resets the data. From the first public release, write a tested Migration instead.
- Current version: 4 (added `categories.builtInKey`).
- Default categories (`domain/models/DefaultCategories`, one per `BuiltInCategory`) are seeded by `ExpenseDatabaseCallback`, in the app's language, in `onCreate` and again after a destructive migration, which empties the tables without calling `onCreate`.
- Debug builds have Settings → Developer → "Load sample data" (`data/sample/`), gated by `BuildInfo.isDebug`. "Delete all data" (Settings → Danger zone, `AppDataRepository`) is a user feature in every build.

## Architecture decisions
Settled during the LLD refactor. Change one only with a reason that beats the one given.
- **Money is whole minor units in a `Long`** (`Money.minorUnits`, hundredths of the currency's unit, `MINOR_PER_UNIT = 100` for every currency), in the domain and in the database. `Double` loses the decimals when amounts are summed. The Room column is still named `amountPaise` (and the backup field too), so the schema didn't change.
- **The currency formatter is an injected class**, not an `object`: it reads the currency from `CurrencyProvider` on every call, so a currency setting needs no change to the screens.
- **The currency is a display setting; nothing is converted.** Stored amounts keep their numbers; only the symbol and how the number is written change, which the confirmation says with the user's latest transaction. The CSV gets a `Currency` column (ISO code) and keeps two decimals in every currency, and a backup carries an optional `currency` field (no version bump): restoring a file made in another currency offers to switch to it, and an unknown code is ignored, never a reason to refuse the file.
- **The currency is read under the splash.** `CurrencyProvider.current()` is synchronous, so `PrepareAppUseCase` loads it into memory before the first frame; `MainViewModel` follows it through `ObserveDisplaySettingsUseCase` (theme + currency), which keeps it at five dependencies.
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
- **Theme and language changes are shown with a snapshot at the root.** The whole tree recomposes with new colors in one frame, so `AppMotionHost` draws a picture of the old look over the new one and takes it away: a circle from the tapped option, or a cross-fade when the phone switched dark mode. A theme change doesn't recreate the activity (`configChanges="uiMode"`); a language change does, so its picture is kept in the activity-retained `ScreenSnapshotHolder` for at most a second. The effective theme reads the phone's own dark mode (`UiModeManager`), because from Android 12 the app's configuration reports the app's own night mode for a moment after a change.
- **IDs are typed in the domain and `Int` in Room.** `TransactionId` and `CategoryId` can't be mixed up; entities keep `Int`, so typing them needed no schema change.

## Known issues (remove each line once fixed)
- Bill scanning reads rupee words only (₹, Rs, INR in `BillKeywords`). With another currency it still reads bare numbers, but a total written with another symbol may be missed.
- `AddEditTransactionViewModel` has 14 dependencies. Fold them into facades.
- `SettingsViewModel` has 12 dependencies. Fold them into facades.
- The Add/Edit transaction form isn't kept in saved state, so typed values are lost on process death.

## Workflow
- Never commit to `main`. Use branches like `refactor/hilt` or `feature/theme-toggle`.
- One task per session. Propose a plan before large refactors.
- Refactors must not change behavior or UI.
