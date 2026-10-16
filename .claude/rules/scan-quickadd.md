---
paths:
  - "**/domain/usecase/scan/**"
  - "**/domain/usecase/quickadd/**"
  - "**/addEditTransactions/**"
---

# Bill scanning, quick add and the Add/Edit form

- Bill scanning: ML Kit Document Scanner and Text Recognition v2 (Latin), both the Google Play services (unbundled) variants; Tasks are awaited with `kotlinx-coroutines-play-services`. No camera permission.
- Quick add: rules in pure Kotlin (no model, no network). Voice goes through the system's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` screen, seen through a `<queries>` entry in the manifest; no microphone permission.
- `domain/usecase/scan/`: `ScanBillUseCase` (read, then `ParseBillUseCase`), one parser per rule (amount, title, date, note), and `BillKeywords`: every word list and limit they use, en-IN for now.
- `domain/usecase/quickadd/`: `ParseQuickAddUseCase` (runs the `QuickAddParser`, then checks its result), `SplitQuickAddUseCase` (several in one sentence), `GetFrequentTransactionsUseCase` (title + category at least 3 times in 60 days, median amount), `RuleBasedQuickAddParser` and its rule parsers, and `QuickAddKeywords`: every word they know, English and Hinglish (en-IN) for now.
- `presentation/transactions/addEditTransactions/`: `draft/TransactionDraftReducer` puts a `TransactionDraft` into the form and tracks which fields still hold its values and where they came from (`DraftFill`, `DraftSource`); `scan/rememberBillScanner` and `voice/rememberVoiceInput` are the one place the scanner and the speech recognizer are launched; `assist/DraftAssistController` does everything that fills the form for the user (scan, quick-add bar and preview, frequent spends, a batch and its review one by one, learning from corrections, entry mode), and the ViewModel gives it the form through `DraftAssistHost`.
- `AddEntryMode` (manual, quick add, scan, voice) is the `mode` route argument, set by a long press on the bottom bar's + or a launcher shortcut (`res/xml-v25/shortcuts.xml`, deep link `expensetracker://add?mode=…`, no intent filter).

Decisions that apply here (reasoning in `docs/architecture-decisions.md`): a scanned bill becomes a `TransactionDraft`; the photo of a bill is never kept; a doubtful value is left out; quick add is another source of drafts; what quick add learns is a cache; several from one sentence are saved all or none; an entry mode is launched once.
