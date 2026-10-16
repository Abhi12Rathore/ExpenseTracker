package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.repository.CategoryMemoryRepository
import com.example.expensetrackerpro.domain.repository.QuickAddPreferencesRepository
import com.example.expensetrackerpro.domain.scan.BillScannerAvailability
import com.example.expensetrackerpro.domain.scan.BillTextReader
import com.example.expensetrackerpro.domain.speech.VoiceInputAvailability
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.categorymemory.ObserveCategoryMemoryUseCase
import com.example.expensetrackerpro.domain.usecase.categorymemory.RememberCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.IsVoiceInputAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.MarkQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ObserveQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ParseQuickAddUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.RuleBasedQuickAddParser
import com.example.expensetrackerpro.domain.usecase.scan.IsBillScannerAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ParseBillUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ResolveCategoryHintUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ScanBillUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FormatAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.assist.DraftAssistController
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/** What quick add remembered, kept in memory; [failWith] makes every write fail. */
class FakeCategoryMemoryRepository(initial: List<CategoryMemory> = emptyList()) : CategoryMemoryRepository {

    val memory = MutableStateFlow(initial)
    var failWith: DomainError? = null

    override fun observeAll(): Flow<List<CategoryMemory>> = memory

    override suspend fun remember(memory: CategoryMemory): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        this.memory.value = this.memory.value.filterNot { it.titleKey == memory.titleKey && it.type == memory.type } + memory
        return Outcome.Success(Unit)
    }
}

class FakeQuickAddPreferencesRepository(used: Boolean = false) : QuickAddPreferencesRepository {

    val used = MutableStateFlow(used)

    override fun hasUsedQuickAdd(): Flow<Boolean> = used

    override suspend fun setUsedQuickAdd() {
        used.value = true
    }
}

class FakeVoiceInputAvailability(var available: Boolean = true) : VoiceInputAvailability {

    override fun isAvailable(): Boolean = available
}

/** The form's helper as the app builds it, from fakes that each test can reach. */
fun draftAssistController(
    timeProvider: TimeProvider,
    billReader: BillTextReader = FakeBillTextReader(),
    scannerAvailability: BillScannerAvailability = FakeBillScannerAvailability(),
    memory: FakeCategoryMemoryRepository = FakeCategoryMemoryRepository(),
    preferences: FakeQuickAddPreferencesRepository = FakeQuickAddPreferencesRepository(),
    voice: FakeVoiceInputAvailability = FakeVoiceInputAvailability(),
    locale: Locale = Locale.ENGLISH
) = DraftAssistController(
    scanBill = ScanBillUseCase(billReader, ParseBillUseCase(), timeProvider),
    isBillScannerAvailable = IsBillScannerAvailableUseCase(scannerAvailability),
    parseQuickAdd = ParseQuickAddUseCase(RuleBasedQuickAddParser(), timeProvider, FakeCurrencyProvider()),
    observeCategoryMemory = ObserveCategoryMemoryUseCase(memory),
    rememberCategory = RememberCategoryUseCase(memory),
    observeQuickAddUsed = ObserveQuickAddUsedUseCase(preferences),
    markQuickAddUsed = MarkQuickAddUsedUseCase(preferences),
    isVoiceInputAvailable = IsVoiceInputAvailableUseCase(voice),
    appLocaleProvider = FakeAppLocaleProvider(locale),
    resolveTransactionDate = ResolveTransactionDateUseCase(timeProvider),
    draftReducer = TransactionDraftReducer(
        FormatAmountInputUseCase(),
        ResolveTransactionDateUseCase(timeProvider),
        ResolveCategoryHintUseCase()
    )
)
