package com.smartfiles.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartfiles.core.workmanager.WorkConstraints
import com.smartfiles.core.workmanager.WorkNames
import com.smartfiles.domain.BackgroundWorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Central scheduler for background work (LLD §4.10). */
@Singleton
class WorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BackgroundWorkScheduler {

    /** One background deep-processing run; KEEP guarantees a single instance. */
    fun scheduleDeepProcessing() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WorkNames.DEEP_PROCESSING,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DeepProcessingWorker>()
                .setConstraints(WorkConstraints.batteryNotLow)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_INITIAL_SECONDS, TimeUnit.SECONDS)
                .build(),
        )
    }

    /** Immediate metadata scan; REPLACE restarts any queued one-time scan. */
    override fun scheduleImmediateMetadataScan() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WorkNames.METADATA_SCAN_ONCE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MetadataScanWorker>().build(),
        )
    }

    /** User-triggered "process now": no constraints, runs promptly. */
    override fun scheduleUserTriggeredProcessing() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WorkNames.USER_PROCESSING,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UserTriggeredProcessingWorker>().build(),
        )
    }

    fun schedulePeriodicMetadataScan() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkNames.METADATA_SCAN,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MetadataScanWorker>(4, TimeUnit.HOURS).build(),
        )
    }

    fun schedulePeriodicPermissionValidation() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkNames.PERMISSION_VALIDATION,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PermissionValidationWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    companion object {
        private const val RETRY_INITIAL_SECONDS = 30L
    }
}
