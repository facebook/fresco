/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
