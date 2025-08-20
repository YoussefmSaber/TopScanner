package net.technical1.topscanner.manager.helper

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import net.technical1.topscanner.constant.StorageFolders
import net.technical1.topscanner.model.DocumentFormat
import net.technical1.topscanner.model.ScannedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreHelper(private val context: Context) {

    suspend fun getAllScannedDocuments(): List<ScannedDocument> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedDocument>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val filesCollection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val imagesCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val fileProj = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.RELATIVE_PATH
                )

                val imageProj = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.RELATIVE_PATH
                )

                val pdfPattern = "%${StorageFolders.BASE_DOCS}/PDF%"
                val docxPattern = "%${StorageFolders.BASE_DOCS}/DOCX%"
                val jpegPattern = "%${StorageFolders.BASE_DOCS}/JPEG%"
                val pngPattern = "%${StorageFolders.BASE_DOCS}/PNG%"

                queryMediaStoreAndCollect(filesCollection, fileProj,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.MIME_TYPE} = ?",
                    arrayOf(pdfPattern, "application/pdf")
                ) { id, name, _, dateModifiedSeconds ->
                    val uri = ContentUris.withAppendedId(filesCollection, id)
                    results.add(
                        ScannedDocument(
                            DocumentFormat.PDF,
                            name,
                            uri,
                            safeDateMillis(dateModifiedSeconds)
                        )
                    )
                }

                queryMediaStoreAndCollect(filesCollection, fileProj,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.MIME_TYPE} = ?",
                    arrayOf(docxPattern, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                ) { id, name, _, dateModifiedSeconds ->
                    val uri = ContentUris.withAppendedId(filesCollection, id)
                    results.add(
                        ScannedDocument(
                            DocumentFormat.DOCX,
                            name,
                            uri,
                            safeDateMillis(dateModifiedSeconds)
                        )
                    )
                }

                queryMediaStoreAndCollect(imagesCollection, imageProj,
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ?",
                    arrayOf(jpegPattern, "image/%jpeg%")
                ) { id, name, _, dateModifiedSeconds ->
                    val uri = ContentUris.withAppendedId(imagesCollection, id)
                    results.add(
                        ScannedDocument(
                            DocumentFormat.JPEG,
                            name,
                            uri,
                            safeDateMillis(dateModifiedSeconds)
                        )
                    )
                }

                queryMediaStoreAndCollect(imagesCollection, imageProj,
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ?",
                    arrayOf(pngPattern, "image/%png%")
                ) { id, name, _,  dateModifiedSeconds ->
                    val uri = ContentUris.withAppendedId(imagesCollection, id)
                    results.add(
                        ScannedDocument(
                            DocumentFormat.PNG,
                            name,
                            uri,
                            safeDateMillis(dateModifiedSeconds)
                        )
                    )
                }
            } else {
                tryCollectFilesFromFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), StorageFolders.PDF_SUB.trimStart('/'), DocumentFormat.PDF, results)
                tryCollectFilesFromFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), StorageFolders.DOCX_SUB.trimStart('/'), DocumentFormat.DOCX, results)
                tryCollectFilesFromFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), StorageFolders.JPEG_SUB.trimStart('/'), DocumentFormat.JPEG, results)
                tryCollectFilesFromFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), StorageFolders.PNG_SUB.trimStart('/'), DocumentFormat.PNG, results)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        results.sortByDescending { it.lastModified }
        results
    }

    private fun queryMediaStoreAndCollect(
        collection: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        collector: (id: Long, displayName: String, mime: String, dateModifiedSeconds: Long) -> Unit
    ) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            if (cursor != null && cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val nameIndex = cursor.getColumnIndexOrThrow(projection[1])
                val mimeIndex = cursor.getColumnIndexOrThrow(projection[2])
                val dateIndex = cursor.getColumnIndexOrThrow(projection[3])

                do {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: "unknown"
                    val mime = cursor.getString(mimeIndex) ?: ""
                    val dateModified = if (!cursor.isNull(dateIndex)) cursor.getLong(dateIndex) else 0L
                    collector(id, name, mime, dateModified)
                } while (cursor.moveToNext())
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            cursor?.close()
        }
    }

    private fun tryCollectFilesFromFolder(baseDir: File, subPath: String, type: DocumentFormat, results: MutableList<ScannedDocument>) {
        try {
            val folder = File(baseDir, subPath)
            if (!folder.exists() || !folder.isDirectory) return
            folder.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val uri = Uri.fromFile(file)
                results.add(ScannedDocument(type, file.name, uri, file.lastModified()))
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun safeDateMillis(dateModifiedValue: Long): Long {
        if (dateModifiedValue == 0L) return 0L
        return if (dateModifiedValue < 1_000_000_000_000L) dateModifiedValue * 1000L else dateModifiedValue
    }
}
