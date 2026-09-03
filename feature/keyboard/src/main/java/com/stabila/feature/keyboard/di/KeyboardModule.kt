package com.stabila.feature.keyboard.di

import com.stabila.feature.keyboard.suggestion.BigramModel
import com.stabila.feature.keyboard.suggestion.HybridSuggestionEngine
import com.stabila.feature.keyboard.suggestion.SuggestionEngine
import com.stabila.feature.keyboard.suggestion.TrieSuggestionEngine
import com.stabila.feature.keyboard.suggestion.UserFrequencyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the keyboard feature's internal dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object KeyboardModule {

    @Provides
    @Singleton
    fun provideTrieSuggestionEngine(): TrieSuggestionEngine = TrieSuggestionEngine()

    @Provides
    @Singleton
    fun provideBigramModel(): BigramModel = BigramModel()

    @Provides
    @Singleton
    fun provideUserFrequencyStore(): UserFrequencyStore = UserFrequencyStore()

    @Provides
    @Singleton
    fun provideSuggestionEngine(
        trie: TrieSuggestionEngine,
        bigram: BigramModel,
        userStore: UserFrequencyStore,
    ): SuggestionEngine = HybridSuggestionEngine(trie, bigram, userStore)
}
