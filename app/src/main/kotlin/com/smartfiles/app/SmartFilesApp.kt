package com.smartfiles.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartfiles.data.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt entry point. Also owns WorkManager's worker factory so @HiltWorker
 * classes (metadata scan, deep processing, permission validation) receive
 * injected dependencies. Periodic background work is registered lazily from
 * [registerBackgroundWork] to avoid touching WorkManager before the first
 * configuration.
 */
@HiltAndroidApp
class SmartFilesApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        workScheduler.schedulePeriodicMetadataScan()
        workScheduler.schedulePeriodicPermissionValidation()
    }
}
