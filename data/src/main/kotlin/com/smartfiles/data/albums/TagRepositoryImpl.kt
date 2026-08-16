package com.smartfiles.data.albums

import com.smartfiles.core.database.dao.TagDao
import com.smartfiles.domain.TagCandidate
import com.smartfiles.domain.TagRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [TagRepository]: deduplicates tags by name and keeps the FTS
 * `tagsConcat` denormalization in sync (LLD §2.5). */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override suspend fun replaceTagsForFile(fileId: Long, tags: List<TagCandidate>) {
        tagDao.clearLinks(fileId)
        for (tag in tags) {
            tagDao.insertIfAbsent(tag.name, category = null)
            val tagId = tagDao.idByName(tag.name) ?: continue
            tagDao.link(fileId, tagId, tag.confidence)
        }
        tagDao.refreshTagsConcat(fileId)
    }
}