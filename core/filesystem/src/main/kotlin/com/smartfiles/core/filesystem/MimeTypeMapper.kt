package com.smartfiles.core.filesystem

import com.smartfiles.core.model.DocType

/** Maps MIME types to the coarse [DocType] used by the processing pipeline. */
object MimeTypeMapper {
    fun docTypeFor(mime: String): DocType = when {
        mime == "application/pdf" -> DocType.PDF
        mime.startsWith("image/") -> DocType.IMAGE
        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocType.DOCX
        else -> DocType.OTHER
    }
}
