# Architecture decisions

Settled during the LLD refactor and the features after it. Change one only with a reason that beats the one given here. `CLAUDE.md` keeps a one-line summary of each.

## Money is whole minor units in a `Long`

**Money is whole minor units in a `Long`** (`Money.minorUnits`, hundredths of the currency's unit, `MINOR_PER_UNIT = 100` for every currency), in the domain and in the database. `Double` loses the decimals when amounts are summed. The Room column is still named `amountPaise` (and the backup field too), so the schema didn't change.

## The currency formatter is an injected class

**The currency formatter is an injected class**, not an `object`: it reads the currency from `CurrencyProvider` on every call, so a currency setting needs no change to the screens.

## The currency is a display setting; nothing is converted

**The currency is a display setting; nothing is converted.** Stored amounts keep their numbers; only the symbol and how the number is written change, which the confirmation says with the user's latest transaction. The CSV gets a `Currency` column (ISO code) and keeps two decimals in every currency, and a backup carries an optional `currency` field (no version bump): restoring a file made in another currency offers to switch to it, and an unknown code is ignored, never a reason to refuse the file.

## The currency is read under the splash

**The currency is read under the splash.** `CurrencyProvider.current()` is synchronous, so `PrepareAppUseCase` loads it into memory before the first frame; `MainViewModel` follows it through `ObserveDisplaySettingsUseCase` (theme + currency), which keeps it at five dependencies.

## Category name and icon are read through a JOIN

**Category name and icon are read through a JOIN**, never copied onto the transaction, so renaming a category can't leave old names behind.

## Expected failures are `Outcome<T>` with a `DomainError`

**Expected failures are `Outcome<T>` with a `DomainError`**, not `Boolean`, a nullable or `kotlin.Result`: the caller has to handle the failure and can tell why it happened. Only `SQLiteException` is caught (`storageOutcome { }`); anything else is a bug and crashes.

## One DAO per aggregate

**One DAO per aggregate**, and a repository takes only the DAOs it uses, so a change to one table's queries doesn't touch the others.

## `AppLockController` owns the app lock

**`AppLockController` owns the app lock.** It is a plain injectable class, one per `MainViewModel`, so the lock rules are tested without a ViewModel and `MainViewModel` stays at five dependencies.

## A backup holds transactions and categories only

**A backup holds transactions and categories only**, never theme, profile or app lock: a file must not be able to turn the lock off. The file is versioned JSON (`Backup.VERSION`); its DTOs stay in `data/backup/`, and `ValidateBackupUseCase` checks the whole file before anything is written.

## A restore keeps only the picked file's Uri

**A restore keeps only the picked file's Uri** (in `SavedStateHandle`), re-reads and re-validates it on confirm, and replaces both tables in one Room transaction (`AppDataDao.replaceAll`), so a failure at any point leaves the current data as it was.

## A scanned bill becomes a `TransactionDraft`, and only a draft reaches the form

**A scanned bill becomes a `TransactionDraft`, and only a draft reaches the form.** Reading (`BillTextReader`), parsing (`ParseBillUseCase`, pure Kotlin) and filling the form (`TransactionDraftReducer`) are separate steps, so another source of drafts reuses the last one unchanged. A draft never replaces what the user entered, and nothing is saved without the user tapping Save.

## The photo of a bill is never kept

**The photo of a bill is never kept.** The reader deletes it once it has been read; only its Uri is held, in `SavedStateHandle`, until then, so a scan survives the process being killed. This is why the banner shows an icon and not the photo.

## A doubtful value is left out rather than guessed

**A doubtful value is left out rather than guessed**: a total next to "Sub Total", a date in the future or over a year old, an amount of ₹10,00,000 or more. Bill words and limits live only in `BillKeywords`; the parsers hold rules, not words.

## A built-in category is known by its key, and its name follows the app's language

**A built-in category is known by its key, and its name follows the app's language.** The name is still stored, so every query and JOIN reads it as before; `LocalizeDefaultCategoriesUseCase` rewrites it when the language changes. A rename clears the key, which is how the user's name is never replaced; a built-in whose translated name another category of its type already has is left as it is. Backups carry the key as an optional field, so older files still restore.

## The language change is applied after the Language screen has closed

**The language change is applied after the Language screen has closed**, and the previous choice is kept in memory for the Undo. The switch recreates the activity while `MainViewModel` stays, and the app is never reported as backgrounded, so the app lock is not triggered.

## Theme and language changes are shown with a snapshot at the root

**Theme and language changes are shown with a snapshot at the root.** The whole tree recomposes with new colors in one frame, so `AppMotionHost` draws a picture of the old look over the new one and takes it away: a circle from the tapped option, or a cross-fade when the phone switched dark mode. A theme change doesn't recreate the activity (`configChanges="uiMode"`); a language change does, so its picture is kept in the activity-retained `ScreenSnapshotHolder` for at most a second. The effective theme reads the phone's own dark mode (`UiModeManager`), because from Android 12 the app's configuration reports the app's own night mode for a moment after a change.

## Quick add is another source of drafts

**Quick add is another source of drafts.** A sentence becomes a `TransactionDraft` (with a type and a category of its own) and goes through the same `TransactionDraftReducer` as a bill: nothing typed is overwritten, Clear takes it out (the type too), nothing is saved without Save. `ParseQuickAddUseCase` checks whatever the parser returns (amount, date, category), so another parser (a language, an on-device model) can go behind `QuickAddParser` without new checks. Words live only in `QuickAddKeywords`.

## What quick add learns is a cache, not the user's data

**What quick add learns is a cache, not the user's data.** `category_memory` (title key and type → category) is written on Save when the user picked another category than the draft's, or one where it found none; it is not in backups, "Delete all data" empties it, and it goes with its category (`ON DELETE CASCADE`), so a restore clears it too.

## Several transactions from one sentence are saved all or none

**Several transactions from one sentence are saved all or none.** `SaveTransactionsUseCase` checks each with the form's rules (`ValidateTransactionDraftUseCase`), then one `TransactionDao.insertTransactions`, which Room runs in one transaction; no Room in the domain and no exception for control flow. "Save all" needs every row to have a title and a category, all on screen; "Review one by one" fills the form with each in turn.

## An entry mode is launched once

**An entry mode is launched once.** A shortcut's mode becomes `QuickAddUiState.launchRequest`; the screen opens the scanner, the voice or the bar and reports `LaunchHandled`, kept in `SavedStateHandle`, so a recreate doesn't open it again. The NavHost exists only once unlocked, so a shortcut never opens anything over the lock.

## IDs are typed in the domain and `Int` in Room

**IDs are typed in the domain and `Int` in Room.** `TransactionId` and `CategoryId` can't be mixed up; entities keep `Int`, so typing them needed no schema change.
