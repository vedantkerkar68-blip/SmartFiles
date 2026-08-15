package com.smartfiles.data.di

import com.smartfiles.data.files.FileRepositoryImpl
import com.smartfiles.data.queue.ProcessingQueueRepositoryImpl
import com.smartfiles.data.settings.SettingsRepositoryImpl
import com.smartfiles.domain.FileRepository
import com.smartfiles.domain.ProcessingQueueRepository
import com.smartfiles.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProcessingQueueRepository(impl: ProcessingQueueRepositoryImpl): ProcessingQueueRepository
}
