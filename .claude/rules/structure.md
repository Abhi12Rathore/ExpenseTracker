---
paths:
  - "app/src/**/*.kt"
---

# Package structure (`com.example.expensetrackerpro`)

Only what the folder names don't tell.
- DAOs: `TransactionDao` (CRUD, range and search reads), `CategoryDao` (CRUD, usage, rename, reassign-then-delete), `DashboardDao` (report reads only), `CategoryMemoryDao` (what quick add learnt), `AppDataDao` ("Delete all data"; the whole-table read and replace behind backup and restore), `SampleDataDao` (debug). Query result classes in `data/local/model/`.
- `data/mapper/StorageErrorMapper` (`storageOutcome { }`) turns a `SQLiteException` into a `DomainError`.
- `data/files/DocumentFiles` is the one place a Uri the user picked is opened (through `ContentResolver`). The CSV (UTF-8 with a BOM) and the backup go through it.
- `data/scan/`: `MlKitBillTextReader` (text recognition, then deletes the photo), `BillLineAssembler` (joins recognized pieces into the bill's lines).
- `data/language/AppLanguageRepositoryImpl` is the one place the app's language is read from and handed to AppCompat. String-resource lookups in any language: `data/currency/ResourceCurrencyNames`, `data/category/ResourceDefaultCategoryNames`. `AndroidNumberSymbolsProvider` gives a language's separators (`DecimalFormatSymbols`); `AndroidRegionCurrency` the phone region's currency for the first-run default.
- `data/preferences/CurrencyRepositoryImpl` is also the `CurrencyProvider`. `AndroidMotionPreferences`: animations off when the animator duration scale is 0.
- `domain/models/Ids.kt`: typed IDs. `domain/usecase/<feature>/`: one class per operation, `operator fun invoke`.
- `presentation/common/category/editor/`: the add/edit category sheet shared by Manage Categories and Add/Edit Transaction (`CategoryEditorReducer`); the host ViewModel keeps the state and wraps the events.
- Controllers keep ViewModels small: `BackupController` (Export & backup), `RegionalSettingsController` (Settings' Language and Currency rows and their Undo snackbars), `AppLockController` (lock state, prompt flow, background timing; `MainViewModel` shows it and forwards the events).
- `presentation/common/error/`: `DomainError.toMessageRes()`, the one place an error becomes a message.
- Composition locals: `LocalDateFormatter`, `LocalCurrencyFormatter`, `LocalAppCurrency` (provided in `MainActivity` from `MainUiState.currency`, so a change recomposes every screen) with `amountFormatKey()`, `LocalAnimationsEnabled`.
- `presentation/language/components/LocalizedContent` draws the Preview card in another language without switching the app. The Language and Currency pickers share `presentation/common/list/`.
- `presentation/common/motion/AppMotionHost`: the app root (theme reveal, cross-fade, the fade after a recreate). `rememberHaptics()` gives confirm and toggle feedback through the View (Compose 1.7 has no such `HapticFeedbackType`).
