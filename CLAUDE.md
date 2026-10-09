# ExpenseTrackerPro

Android app for tracking income and expenses: transactions, custom categories, dashboard with charts and insights.
Goal: production-quality portfolio project.

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Run all three before saying a task is done.

## Tech stack
- Kotlin 2.0.21, Jetpack Compose (Material 3), minSdk 24, targetSdk 35
- Room 2.6.1 (KSP), Navigation Compose, Hilt 2.56.1
- Charts: MPAndroidChart (View-based, wrapped in Compose) in `presentation/homeScreen/component/charts/`
- Dependencies are declared in `gradle/libs.versions.toml`. Add new ones there, never hardcode versions in `app/build.gradle.kts`.

## Package structure (`com.example.expensetrackerpro`)
- `data/local/` → Room: `entity/`, `dao/`, `expenseDatabase/`, `model/` (query result classes)
- `data/mapper/` → entity ↔ domain mappers
- `data/repository/` → repository implementations
- `domain/` → `models/`, `repository/` (interfaces), `usecase/<feature>/` (one class per operation, `operator fun invoke`)
- `presentation/<feature>/` → Screen, ViewModel, `components/`, `uistate/`
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
- **Value types over primitives:** money is `Money` (whole paise as `Long`, never `Double`), IDs are typed (`@JvmInline value class TransactionId(val value: Long)`), dates go through `DateRange` / `TimeProvider`.
- **No stringly-typed data:** fixed sets are enums or sealed types (e.g. `CategoryIcon` enum stored via a Room `TypeConverter`), not free strings.
- **Data access:** one DAO per aggregate (`TransactionDao`, `CategoryDao`, `DashboardDao`). Don't store the same data twice; get related data through a JOIN or `@Relation`.
- **Errors:** expected failures are typed. Use cases return `Result<T, DomainError>` (a sealed `DomainError` in `domain/`), ViewModels map them to UI state. No silent `catch {}` and no exceptions used for control flow.
- **Time:** never call `System.currentTimeMillis()` or `Calendar.getInstance()` outside `data/time/`. Inject `TimeProvider`.
- **Global objects:** `object` is only for stateless pure helpers. Anything that reads the clock, locale or other state is an injectable class.
- **Immutability:** domain models and UI state are immutable `data class`es with `val`s and read-only `List`s.

## Conventions
- Money: format only through the shared en-IN currency formatter, `utils/CurrencyFormatter` (e.g. `-₹1,00,000.00`). Never use string concatenation like `"₹$amount"`.
- Dates: format via the shared date utility, `utils/DateFormatter`. Never show raw millis.
- Colors: no Color literals outside `ui/theme`; use `MaterialTheme.colorScheme` or `MaterialTheme.extendedColors` (income/expense, chart and category palette colors). `Color.Transparent` is fine. Charts get theme colors as ARGB via `toArgb()`.
- UI strings go in `strings.xml`.
- No `!!`. Handle nulls explicitly.
- Every new ViewModel or use case gets unit tests in `app/src/test`.

## Database
- Any schema change must bump the version and add a Room `Migration`.
- Don't rely on `fallbackToDestructiveMigration()`, because it wipes user data. Remove it once migrations exist.
- Default categories are seeded in `AppDatabase` `onCreate`.

## Known issues (remove each line once fixed)
Planned for the `refactor/lld` branch:
- `TransactionDao` has 19 methods covering transactions, categories and dashboard queries. Split it per aggregate.
- Money is `Double` in entities and domain models. Migrate to `Money` (paise as `Long`) with a Room `Migration`.
- `Transaction` stores `categoryName` alongside `categoryId`, so a category rename leaves stale names. Read it via JOIN/`@Relation` and drop the column (migration).
- `HomeViewModel` has 11 constructor dependencies. Introduce `GetDashboardUseCase`.
- No shared error model. Introduce `DomainError` + `Result`.
- `DateFormatter.hourOfDay` and `HomeViewModel` (greeting) read the system clock directly. Use `TimeProvider`.
- `Category.iconName` is a free string. Replace with a `CategoryIcon` enum.

## Workflow
- Never commit to `main`. Use branches like `refactor/hilt` or `feature/theme-toggle`.
- One task per session. Propose a plan before large refactors.
- Refactors must not change behavior or UI.
