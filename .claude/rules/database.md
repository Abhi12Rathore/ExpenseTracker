---
paths:
  - "**/data/local/**"
  - "**/data/repository/**"
  - "**/data/backup/**"
---

# Database

- Pre-release: every schema change bumps the version; `fallbackToDestructiveMigration` resets the data. From the first public release, write a tested Migration instead.
- Current version: 5 (added `category_memory`).
- Default categories (`domain/models/DefaultCategories`, one per `BuiltInCategory`) are seeded by `ExpenseDatabaseCallback`, in the app's language, in `onCreate` and again after a destructive migration, which empties the tables without calling `onCreate`.
- Debug builds have Settings → Developer → "Load sample data" (`data/sample/`), gated by `BuildInfo.isDebug`. "Delete all data" (Settings → Danger zone, `AppDataRepository`) is a user feature in every build.

Decisions that apply here (reasoning in `docs/architecture-decisions.md`): category name and icon through a JOIN; one DAO per aggregate; `Outcome` and `storageOutcome { }`; what a backup holds; a restore in one transaction; quick-add memory is a cache; IDs `Int` in Room.
