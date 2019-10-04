/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlin

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class MediaStoreData {
  companion object {
    private val IMAGE_ID_COLUMN_NAME = MediaStore.Images.Media._ID
    private val CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
  }

  fun loadPhotoUris(context: Context): List<Uri> {
    val uris = mutableListOf<Uri>()
    context.contentResolver.query(
        CONTENT_URI,
        arrayOf(IMAGE_ID_COLUMN_NAME),
        null,
        null,
        null)?.use {
      val dataIndex = it.getColumnIndexOrThrow(IMAGE_ID_COLUMN_NAME)
      while (it.moveToNext()) {
        uris.add(ContentUris.withAppendedId(CONTENT_URI, it.getLong(dataIndex)))
      }
    }

    return uris
  }
}
