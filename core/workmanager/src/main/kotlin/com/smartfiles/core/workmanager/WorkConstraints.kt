package com.smartfiles.core.workmanager

import androidx.work.Constraints

/** Reusable constraint presets (LLD §4.10). */
object WorkConstraints {
    val none: Constraints = Constraints.NONE

    /** Background deep processing: never hammer a low battery. */
    val batteryNotLow: Constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    /** Larger batches are only worth running while charging and idle. */
    val chargingAndIdle: Constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .build()
}
