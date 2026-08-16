package com.smartfiles.core.database

import androidx.room.TypeConverter
import com.smartfiles.core.model.AlbumType
import com.smartfiles.core.model.AssignmentSource
import com.smartfiles.core.model.CorrectionType
import com.smartfiles.core.model.DocType
import com.smartfiles.core.model.DuplicateGroupStatus
import com.smartfiles.core.model.DuplicateGroupType
import com.smartfiles.core.model.FolderPermissionState
import com.smartfiles.core.model.ProcessingStatus
import com.smartfiles.core.model.QueueStatus

object Converters {
    @TypeConverter fun processingStatusToString(v: ProcessingStatus) = v.name
    @TypeConverter fun stringToProcessingStatus(v: String) = ProcessingStatus.valueOf(v)

    @TypeConverter fun docTypeToString(v: DocType) = v.name
    @TypeConverter fun stringToDocType(v: String) = DocType.valueOf(v)

    @TypeConverter fun albumTypeToString(v: AlbumType) = v.name
    @TypeConverter fun stringToAlbumType(v: String) = AlbumType.valueOf(v)

    @TypeConverter fun assignmentSourceToString(v: AssignmentSource) = v.name
    @TypeConverter fun stringToAssignmentSource(v: String) = AssignmentSource.valueOf(v)

    @TypeConverter fun queueStatusToString(v: QueueStatus) = v.name
    @TypeConverter fun stringToQueueStatus(v: String) = QueueStatus.valueOf(v)

    @TypeConverter fun correctionTypeToString(v: CorrectionType) = v.name
    @TypeConverter fun stringToCorrectionType(v: String) = CorrectionType.valueOf(v)

    @TypeConverter fun duplicateGroupTypeToString(v: DuplicateGroupType) = v.name
    @TypeConverter fun stringToDuplicateGroupType(v: String) = DuplicateGroupType.valueOf(v)

    @TypeConverter fun duplicateGroupStatusToString(v: DuplicateGroupStatus) = v.name
    @TypeConverter fun stringToDuplicateGroupStatus(v: String) = DuplicateGroupStatus.valueOf(v)

    @TypeConverter fun folderPermissionStateToString(v: FolderPermissionState) = v.name
    @TypeConverter fun stringToFolderPermissionState(v: String) = FolderPermissionState.valueOf(v)
}
