package com.smartfiles.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartfiles.core.database.dao.AlbumDao
import com.smartfiles.core.database.dao.EmbeddingDao
import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.database.dao.FolderDao
import com.smartfiles.core.database.dao.QueueDao
import com.smartfiles.core.database.dao.SearchDao
import com.smartfiles.core.database.entity.AlbumEntity
import com.smartfiles.core.database.entity.DuplicateGroupEntity
import com.smartfiles.core.database.entity.DuplicateGroupMemberEntity
import com.smartfiles.core.database.entity.EmbeddingEntity
import com.smartfiles.core.database.entity.FileAlbumCrossRef
import com.smartfiles.core.database.entity.FileEntity
import com.smartfiles.core.database.entity.FileFtsEntity
import com.smartfiles.core.database.entity.FileTagCrossRef
import com.smartfiles.core.database.entity.IndexedFolderEntity
import com.smartfiles.core.database.entity.ProcessingQueueEntity
import com.smartfiles.core.database.entity.TagEntity
import com.smartfiles.core.database.entity.UserCorrectionEntity

@Database(
    entities = [
        FileEntity::class,
        AlbumEntity::class,
        FileAlbumCrossRef::class,
        TagEntity::class,
        FileTagCrossRef::class,
        EmbeddingEntity::class,
        ProcessingQueueEntity::class,
        UserCorrectionEntity::class,
        DuplicateGroupEntity::class,
        DuplicateGroupMemberEntity::class,
        IndexedFolderEntity::class,
        FileFtsEntity::class,
    ],
    version = 1,
    // Schema JSON is exported to core/database/schemas (KSP room.schemaLocation)
    // and is the basis for migrations + migration tests.
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun albumDao(): AlbumDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun queueDao(): QueueDao
    abstract fun searchDao(): SearchDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val NAME = "smartfiles.db"
    }
}
