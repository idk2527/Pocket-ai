package com.pocketai.app.di

import android.content.Context
import com.pocketai.app.data.database.AppDatabase
import com.pocketai.app.data.repository.ExpenseRepository
import com.pocketai.app.services.LlamaCppService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(appDatabase: AppDatabase): ExpenseRepository {
        return ExpenseRepository(appDatabase.expenseDao())
    }

    @Provides
    @Singleton
    fun provideLlamaCppService(
        @ApplicationContext context: Context,
        preferencesManager: com.pocketai.app.data.preferences.PreferencesManager
    ): LlamaCppService {
        return LlamaCppService(context, preferencesManager)
    }
    
    // ReceiptPipeline is now @Inject constructor, no manual provision needed
}