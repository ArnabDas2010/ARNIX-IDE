package com.arnix.ide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.arnix.ide.data.model.FileManagerItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SafStorageManager {

    private const val PREFS_NAME = "arnix_saf_prefs"
    private const val KEY_DATA_TREE = "data_tree_uri"
    private const val KEY_OBB_TREE = "obb_tree_uri"
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun saveGrantedTree(context: Context, isObb: Boolean, treeUri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (isObb) KEY_OBB_TREE else KEY_DATA_TREE
        prefs.edit().putString(key, treeUri.toString()).apply()
    }

    fun getTreeUri(context: Context, isObb: Boolean): Uri? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (isObb) KEY_OBB_TREE else KEY_DATA_TREE
            val uriStr = prefs.getString(key, null)
            if (uriStr != null) {
                val uri = Uri.parse(uriStr)
                if (isPermissionPersisted(context, uri)) {
                    return uri
                }
            }

            // Fallback: search system persisted permissions
            val targetSegment = if (isObb) "Android%2Fobb" else "Android%2Fdata"
            val altSegment = if (isObb) "Android/obb" else "Android/data"
            val persisted = context.contentResolver.persistedUriPermissions
            for (perm in persisted) {
                if (perm.isReadPermission) {
                    val str = perm.uri.toString()
                    if (str.contains(targetSegment, ignoreCase = true) || str.contains(altSegment, ignoreCase = true)) {
                        saveGrantedTree(context, isObb, perm.uri)
                        return perm.uri
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun hasPermission(context: Context, isObb: Boolean): Boolean {
        return try {
            getTreeUri(context, isObb) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun isPermissionPersisted(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates an ACTION_OPEN_DOCUMENT_TREE Intent pre-targeting Android/data or Android/obb
     * so on Android 11 the system file picker opens right on the folder with "USE THIS FOLDER" button!
     */
    fun createOpenFolderIntent(isObb: Boolean): Intent {
        val documentId = if (isObb) "primary:Android/obb" else "primary:Android/data"

        // Build document Uri for Android 11 EXTRA_INITIAL_URI
        val initialUri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId)

        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
    }

    /**
     * Converts a filesystem path like /storage/emulated/0/Android/data/com.whatsapp to document ID:
     * primary:Android/data/com.whatsapp
     */
    fun pathToDocumentId(path: String, isObb: Boolean): String {
        val rootPrefix = "/storage/emulated/0/"
        val cleanPath = path.removePrefix(rootPrefix).trim('/')
        return if (cleanPath.isEmpty()) {
            if (isObb) "primary:Android/obb" else "primary:Android/data"
        } else {
            "primary:$cleanPath"
        }
    }

    /**
     * Queries files directly from the Storage Access Framework DocumentProvider cursor!
     * 100% works without root on Android 11.
     */
    fun listDirectory(
        context: Context,
        path: String,
        showHidden: Boolean = false,
        searchQuery: String = ""
    ): List<FileManagerItem> {
        val isObb = path.contains("Android/obb", ignoreCase = true)
        val treeUri = getTreeUri(context, isObb) ?: return emptyList()

        val docId = pathToDocumentId(path, isObb)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val items = mutableListOf<FileManagerItem>()
        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val dateIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                val normalizedPath = path.trimEnd('/')

                while (cursor.moveToNext()) {
                    val childDocId = if (idIdx != -1) cursor.getString(idIdx) else ""
                    val displayName = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val mimeType = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else ""
                    val sizeBytes = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
                    val lastMod = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L

                    if (displayName.isEmpty()) continue

                    val isHidden = displayName.startsWith(".")
                    if (!showHidden && isHidden) continue
                    if (searchQuery.isNotBlank() && !displayName.contains(searchQuery, ignoreCase = true)) continue

                    val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val childPath = "$normalizedPath/$displayName"
                    val ext = if (isDir) "" else displayName.substringAfterLast(".", "")

                    items.add(
                        FileManagerItem(
                            name = displayName,
                            path = childPath,
                            isDirectory = isDir,
                            sizeFormatted = if (isDir) "Folder" else formatSize(sizeBytes),
                            lastModifiedFormatted = if (lastMod > 0) dateFormat.format(Date(lastMod)) else "Recently",
                            extension = ext,
                            isHidden = isHidden
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /**
     * Reads file content directly using SAF ContentResolver input stream.
     */
    fun readFile(context: Context, path: String): String? {
        val isObb = path.contains("Android/obb", ignoreCase = true)
        val treeUri = getTreeUri(context, isObb) ?: return null
        val docId = pathToDocumentId(path, isObb)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

        return try {
            context.contentResolver.openInputStream(docUri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Writes file content directly using SAF ContentResolver output stream.
     */
    fun writeFile(context: Context, path: String, content: String): Boolean {
        val isObb = path.contains("Android/obb", ignoreCase = true)
        val treeUri = getTreeUri(context, isObb) ?: return false
        val docId = pathToDocumentId(path, isObb)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

        return try {
            context.contentResolver.openOutputStream(docUri, "wt")?.bufferedWriter()?.use {
                it.write(content)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
