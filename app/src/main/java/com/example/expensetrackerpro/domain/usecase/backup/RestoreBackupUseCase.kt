package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.RestoredBackup
import com.example.expensetrackerpro.domain.repository.BackupRepository
import com.example.expensetrackerpro.domain.usecase.category.LocalizeDefaultCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.currency.SetCurrencyUseCase
import javax.inject.Inject

/** Replaces everything in the app with what a backup file holds. */
class RestoreBackupUseCase @Inject constructor(
    private val readBackup: ReadBackupUseCase,
    private val backupRepository: BackupRepository,
    private val localizeDefaultCategories: LocalizeDefaultCategoriesUseCase,
    private val currencyProvider: CurrencyProvider,
    private val setCurrency: SetCurrencyUseCase
) {

    /**
     * The file is read and checked again here, as it may have changed since it was previewed.
     * On any failure nothing stored has changed. The built-in categories come back under the
     * names of the language the backup was made in, so they are put into the app's language
     * afterwards. With [switchCurrency], amounts are then shown in the currency the backup was
     * made in, when it says and that isn't the current one; nothing is converted either way.
     */
    suspend operator fun invoke(source: DocumentUri, switchCurrency: Boolean): Outcome<RestoredBackup> =
        when (val read = readBackup(source)) {
            is Outcome.Failure -> read
            is Outcome.Success -> {
                val backup = read.value.second
                when (val restored = backupRepository.restore(backup)) {
                    is Outcome.Failure -> restored
                    is Outcome.Success -> {
                        // The restore itself has worked; names left in the other language are put right at the next start
                        localizeDefaultCategories()
                        val switchedTo = backup.currency
                            ?.takeIf { switchCurrency && it != currencyProvider.current() }
                            ?.also { setCurrency(it) }
                        Outcome.Success(RestoredBackup(backup.transactions.size, switchedTo))
                    }
                }
            }
        }
}
