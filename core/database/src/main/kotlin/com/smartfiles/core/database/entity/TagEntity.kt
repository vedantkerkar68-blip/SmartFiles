package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** LLD §2.2 — TagEntity. */
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0,
    val name: String,
    val category: String? = null,
)
