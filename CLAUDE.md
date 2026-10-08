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
- `domain/` → `models/`, `repository/` (interfaces)
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
- Each screen has one ViewModel exposing `StateFlow<XxxUiState>`. User actions go through a sealed `XxxEvent` interface.
- Composables are stateless where possible: state in, lambdas out.

## Conventions
- Money: format only through the shared en-IN currency formatter (e.g. `-₹1,00,000.00`). Never use string concatenation like `"₹$amount"`.
- Dates: format via a shared date utility. Never show raw millis.
- UI strings go in `strings.xml`.
- No `!!`. Handle nulls explicitly.
- Every new ViewModel or use case gets unit tests in `app/src/test`.

## Database
- Any schema change must bump the version and add a Room `Migration`.
- Don't rely on `fallbackToDestructiveMigration()`, because it wipes user data. Remove it once migrations exist.
- Default categories are seeded in `AppDatabase` `onCreate`.

## Known issues (remove each line once fixed)
- `RecentActivityTile.kt`: date shows the literal text "transaction.dateMillis".
- Dashboard header month is wrong, greeting ignores time of day, and currency formatting is inconsistent.
- `play-services-wallet` dependency appears unused.

## Workflow
- Never commit to `main`. Use branches like `refactor/hilt` or `feature/theme-toggle`.
- One task per session. Propose a plan before large refactors.
- Refactors must not change behavior or UI.
