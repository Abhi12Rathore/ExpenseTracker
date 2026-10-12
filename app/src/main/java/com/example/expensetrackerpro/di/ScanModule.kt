package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.scan.GmsBillScannerAvailability
import com.example.expensetrackerpro.data.scan.MlKitBillTextReader
import com.example.expensetrackerpro.domain.scan.BillScannerAvailability
import com.example.expensetrackerpro.domain.scan.BillTextReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanModule {

    @Binds
    @Singleton
    abstract fun bindBillTextReader(impl: MlKitBillTextReader): BillTextReader

    @Binds
    @Singleton
    abstract fun bindBillScannerAvailability(impl: GmsBillScannerAvailability): BillScannerAvailability
}
