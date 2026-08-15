package com.smartfiles.core.common

import com.smartfiles.core.model.ChangeSignature
import com.smartfiles.core.model.DiscoveredFile

/**
 * Pure change-detection logic (LLD §4.1). A file is treated as new/changed only
 * when its size or modification time differs from the cached signature; SHA-256
 * is computed lazily elsewhere and never used as the primary change signal
 * (that would require reading the whole file every scan).
 */
object FileChangeDetector {
    fun shouldReprocess(cached: ChangeSignature?, current: DiscoveredFile): Boolean {
        if (cached == null) return true
        if (cached.sizeBytes != current.sizeBytes) return true
        if (cached.dateModifiedSource != current.dateModifiedSource) return true
        return false
    }
}
