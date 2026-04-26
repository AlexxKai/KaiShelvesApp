package com.example.kaishelvesapp.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DeviceLibraryFile(
    val name: String,
    val location: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val modifiedAtMillis: Long?,
    val uri: Uri
)

class DeviceFileRepository(
    private val context: Context
) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun listFolderFiles(treeUri: Uri, limit: Int = 500): List<DeviceLibraryFile> = withContext(Dispatchers.IO) {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val files = mutableListOf<DeviceLibraryFile>()
        collectTreeFiles(
            treeUri = treeUri,
            documentUri = rootDocumentUri,
            relativePath = "",
            files = files,
            limit = limit,
            depth = 0
        )
        files
    }

    private fun collectTreeFiles(
        treeUri: Uri,
        documentUri: Uri,
        relativePath: String,
        files: MutableList<DeviceLibraryFile>,
        limit: Int,
        depth: Int
    ) {
        if (files.size >= limit || depth > 8) return

        val parentDocumentId = DocumentsContract.getDocumentId(documentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext() && files.size < limit) {
                val documentId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val mimeType = cursor.getString(mimeIndex)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                val childPath = if (relativePath.isBlank()) name else "$relativePath/$name"

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectTreeFiles(treeUri, childUri, childPath, files, limit, depth + 1)
                } else if (isReadableBookFile(name, mimeType)) {
                    files.add(
                        DeviceLibraryFile(
                            name = name,
                            location = relativePath.ifBlank { "/" },
                            mimeType = mimeType,
                            sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
                            modifiedAtMillis = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else null,
                            uri = childUri
                        )
                    )
                }
            }
        }
    }

    private fun isReadableBookFile(name: String, mimeType: String?): Boolean {
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerMime = mimeType.orEmpty().lowercase(Locale.ROOT)
        return lowerName.endsWith(".pdf") ||
            lowerName.endsWith(".epub") ||
            lowerName.endsWith(".txt") ||
            lowerName.endsWith(".mobi") ||
            lowerName.endsWith(".azw") ||
            lowerName.endsWith(".azw3") ||
            lowerName.endsWith(".fb2") ||
            lowerName.endsWith(".cbz") ||
            lowerMime == "application/pdf" ||
            lowerMime == "application/epub+zip" ||
            lowerMime.startsWith("text/")
    }
}
