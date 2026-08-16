package com.smartfiles.core.workmanager

/** Unique-work names shared by schedulers and workers (LLD §4.10). */
object WorkNames {
    const val METADATA_SCAN = "metadata_scan"
    /** One-time immediate scan; distinct name so it never cancels the periodic one. */
    const val METADATA_SCAN_ONCE = "metadata_scan_once"
    const val DEEP_PROCESSING = "deep_processing"
    const val USER_PROCESSING = "user_processing"
    const val PERMISSION_VALIDATION = "permission_validation"
}
