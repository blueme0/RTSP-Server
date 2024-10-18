/*
 * Copyright (C) 2023 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pedro.sample

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/**
 * Created by pedro on 21/06/17.
 * Get absolute path from onActivityResult
 * https://stackoverflow.com/questions/33295300/how-to-get-absolute-path-in-android-for-file
 */
object PathUtils {
  @JvmStatic
  fun getRecordPath(): File {
    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    return File(storageDir.absolutePath + "/RootEncoder")
  }

  @JvmStatic
  fun updateGallery(context: Context, path: String) {
    MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
  }

  @JvmStatic
  fun getPath(context: Context, uri: Uri): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(
        context, uri
      )) {
      if (isExternalStorageDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]

        var storageDefinition: String = ""

        if ("primary".lowercase() == type.lowercase()) {
          return "${Environment.getExternalStorageDirectory()}/${split[1]}"
        } else {
          if (Environment.isExternalStorageRemovable()) {
            storageDefinition = "EXTERNAL_STORAGE"
          } else {
            storageDefinition = "SECONDARY_STORAGE"
          }

          return "${System.getenv(storageDefinition)}/${split[1]}"
        }
      } else if (isDownloadsDocuent(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = ContentUris.withAppendedId(
          Uri.parse("content://downloads/public_downloads"),
          id.toLong()
        )

        return getDataColumn(context, contentUri, null, null)
      } else if (isMediaDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]

        var contentUri: Uri? = null
        if ("image" == type) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if ("video" == type) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else if ("audio" == type) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val selection: String = "_id=?"
        val selectionArgs: Array<String> = arrayOf(split[1])

        return getDataColumn(context, contentUri, selection, selectionArgs)
      }
    } else if ("content" == uri.scheme?.lowercase()) {
      if (isGooglePhotosUri(uri)) return uri.lastPathSegment
      return getDataColumn(context, uri, null, null)
    } else if ("file" == uri.scheme?.lowercase()) {
      return uri.path
    }
    return null
  }

  @JvmStatic
  fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
    var cursor: Cursor? = null
    val column: String = "_data"
    val projection: Array<String> = arrayOf(column)

    if (uri == null) return null
    try {
      cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
      if (cursor != null && cursor.moveToFirst()) {
        val column_index = cursor.getColumnIndexOrThrow(column)
        return cursor.getString(column_index)
      }
    } finally {
      cursor?.close()
    }
    return null
  }

  @JvmStatic
  fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstroage.documents" == uri.authority
  }

  @JvmStatic
  fun isDownloadsDocuent(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
  }

  @JvmStatic
  fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
  }

  @JvmStatic
  fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
  }
}