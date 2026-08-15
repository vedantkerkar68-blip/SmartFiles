package com.smartfiles.domain

import com.smartfiles.core.model.DuplicateGroupStatus
import com.smartfiles.core.model.DuplicateGroupType

data class DuplicateGroup(
    val groupId: Long,
    val groupType: DuplicateGroupType,
    val members: List<DuplicateMember>,
    val status: DuplicateGroupStatus,
)

data class DuplicateMember(
    val fileId: Long,
    val displayName: String,
    val similarityScore: Float,
)

interface DuplicateDetectionRepository {
    suspend fun scanForDuplicates(): List<DuplicateGroup>
    suspend fun scanForExactDuplicates(): Long
    suspend fun scanForPerceptualDuplicates(): Long
    suspend fun pendingGroups(): List<DuplicateGroup>
    suspend fun resolveGroup(groupId: Long, keepFileId: Long?)
    suspend fun dismissPair(fileIdA: Long, fileIdB: Long)
}
