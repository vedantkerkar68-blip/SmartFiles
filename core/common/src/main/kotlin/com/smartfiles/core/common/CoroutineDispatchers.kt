package com.smartfiles.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Central, injectable strategy for which [CoroutineDispatcher] classes of work
 * run on. Keeps threading policy explicit and testable (tests can substitute a
 * test dispatcher set). Mirrors LLD §8 threading model.
 */
interface CoroutineDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
    /** Serialized dispatcher for ML Kit / LiteRT inference (limited parallelism = 1). */
    val mlInference: CoroutineDispatcher
}

object DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val mlInference: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
}
