package com.smartfiles.data.di

import android.content.Context
import androidx.room.Room
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.CoroutineDispatchers
import com.smartfiles.core.common.DefaultCoroutineDispatchers
import com.smartfiles.core.database.AppDatabase
import com.smartfiles.core.database.dao.AlbumDao
import com.smartfiles.core.database.dao.EmbeddingDao
import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.database.dao.QueueDao
import com.smartfiles.core.database.dao.SearchDao
import com.smartfiles.core.datastore.SettingsDataStore
import com.smartfiles.core.filesystem.SafFileSource
import com.smartfiles.core.ml.ContentExtractorImpl
import com.smartfiles.core.ml.PdfBoxTextExtractor
import com.smartfiles.data.logging.AndroidAppLogger
import com.smartfiles.domain.FileSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideFileDao(db: AppDatabase): FileDao = db.fileDao()
    @Provides fun provideAlbumDao(db: AppDatabase): AlbumDao = db.albumDao()
    @Provides fun provideEmbeddingDao(db: AppDatabase): EmbeddingDao = db.embeddingDao()
    @Provides fun provideQueueDao(db: AppDatabase): QueueDao = db.queueDao()
    @Provides fun provideSearchDao(db: AppDatabase): SearchDao = db.searchDao()

    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger = AndroidAppLogger()

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = DefaultCoroutineDispatchers

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideFileSource(@ApplicationContext context: Context, logger: AppLogger): FileSource =
        SafFileSource(context, logger)

    @Provides
    @Singleton
    fun providePdfBoxTextExtractor(
        @ApplicationContext context: Context,
        logger: AppLogger,
    ): PdfBoxTextExtractor = PdfBoxTextExtractor(context, logger)

    @Provides
    @Singleton
    fun provideContentExtractor(
        @ApplicationContext context: Context,
        pdf: PdfBoxTextExtractor,
        logger: AppLogger,
    ): com.smartfiles.domain.ContentExtractor = ContentExtractorImpl(context, pdf, logger)
}
