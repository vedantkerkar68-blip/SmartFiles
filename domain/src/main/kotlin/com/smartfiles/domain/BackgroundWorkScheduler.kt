package com.smartfiles.domain

/**
 * Minimal UI-facing contract for background work (LLD §4.10). Implemented in
 * the data layer by the WorkManager scheduler; keeps the UI layer unaware of
 * WorkManager specifics.
 */
interface BackgroundWorkScheduler {
    /** Kicks off a metadata scan right away (e.g. after a folder grant). */
    fun scheduleImmediateMetadataScan()
    /** "Process now" pass over the whole queue with no battery constraints. */
    fun scheduleUserTriggeredProcessing()
}
