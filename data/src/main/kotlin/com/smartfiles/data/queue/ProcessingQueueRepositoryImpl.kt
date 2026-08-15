package com.smartfiles.data.queue

import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.database.dao.QueueDao
import com.smartfiles.core.database.entity.ProcessingQueueEntity
import com.smartfiles.domain.ProcessingQueueRepository
import com.smartfiles.domain.ProcessingStatusSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** SQLite-backed [ProcessingQueueRepository] (LLD §6.1). */
class ProcessingQueueRepositoryImpl @Inject constructor(
    private val queueDao: QueueDao,
    private val fileDao: FileDao,
) : ProcessingQueueRepository {

    override suspend fun enqueue(fileIds: List<Long>, targetLevel: Int, priority: Int) {
        val items = fileIds
            .filter { queueDao.existingPendingForFile(it).isEmpty() }
            .map { ProcessingQueueEntity(fileId = it, targetLevel = targetLevel, priority = priority) }
        if (items.isNotEmpty()) {
            queueDao.insertAll(items)
        }
    }

    override suspend fun statusSnapshot(): ProcessingStatusSnapshot {
        val pending = queueDao.pendingCount(now())
        val indexed = fileDao.observeIndexedCount().first()
        return ProcessingStatusSnapshot(
            pendingCount = pending,
            inProgressCount = 0,
            indexedCount = indexed,
        )
    }

    override fun observeStatus(): Flow<ProcessingStatusSnapshot> =
        combine(queueDao.observePendingCount(), fileDao.observeIndexedCount()) { pending, indexed ->
            ProcessingStatusSnapshot(
                pendingCount = pending,
                inProgressCount = 0,
                indexedCount = indexed,
            )
        }

    private fun now(): Long = System.currentTimeMillis()
}
