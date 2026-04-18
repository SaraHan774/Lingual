package com.august.spiritscribe.di

import com.august.spiritscribe.data.repository.DiaryRepositoryImpl
import com.august.spiritscribe.data.translation.MlKitTranslationEngine
import com.august.spiritscribe.data.translation.TranslationEngine
import com.august.spiritscribe.domain.repository.DiaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository

    @Binds
    @Singleton
    abstract fun bindTranslationEngine(impl: MlKitTranslationEngine): TranslationEngine
}
