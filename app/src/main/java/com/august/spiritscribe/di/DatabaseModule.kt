package com.august.spiritscribe.di

import android.content.Context
import androidx.room.Room
import com.august.spiritscribe.data.local.AppDatabase
import com.august.spiritscribe.data.local.dao.DiaryEntryDao
import com.august.spiritscribe.data.local.dao.TranslationDao
import com.august.spiritscribe.data.local.dao.WordCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDiaryEntryDao(db: AppDatabase): DiaryEntryDao = db.diaryEntryDao()

    @Provides
    fun provideTranslationDao(db: AppDatabase): TranslationDao = db.translationDao()

    @Provides
    fun provideWordCardDao(db: AppDatabase): WordCardDao = db.wordCardDao()
}
