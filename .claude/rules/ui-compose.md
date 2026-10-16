---
paths:
  - "**/presentation/**"
  - "**/ui/**"
---

# Compose UI

- Each screen is split in two: `XxxScreen` obtains the ViewModel and collects state, `XxxContent(state, onEvent, navigation lambdas)` is the stateless layout. Only purely visual state (animations, scroll, dropdown expansion) stays in `remember`.
- Money: format only through the injected `utils/CurrencyFormatter`, which writes the currency `CurrencyProvider` gives, or one passed in (the Currency screen's previews). Composables get it from `LocalCurrencyFormatter.current` (provided in `MainActivity`; previews get an INR/English default) and never format money without it. Never use string concatenation like `"₹$amount"`. Domain code that writes files (the CSV) keeps its plain decimal (`1130.00`) and doesn't use the formatter.
- A Composable that keeps formatted amounts in `remember` keys it on `amountFormatKey()`, so a currency change with the screen open writes them again.
- Dates: format via the shared date utility, `utils/DateFormatter` (an injected class that writes in the app's language). Never show raw millis.
- Colors: no Color literals outside `ui/theme`; use `MaterialTheme.colorScheme` or `MaterialTheme.extendedColors` (income/expense, chart and category palette colors). `Color.Transparent` is fine. Charts take theme colors directly as `Color`.
- Collect screen state with `collectAsStateWithLifecycle()`, never `collectAsState()`.

## Motion

- Every animation is skipped when `MotionPreferences` says animations are off: Composables read `LocalAnimationsEnabled` (use `snap()` / `EnterTransition.None`, or start at the end value), plain classes take `MotionPreferences`.
- An animation never blocks input: overlays are only drawn, never take touches, and a second change ends the running one at once.
- The effective theme reads the phone's dark mode through `phoneDarkTheme()` / `phoneInDarkTheme()`, never `isSystemInDarkTheme()`.
