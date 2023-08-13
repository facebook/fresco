/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.ContactsContract
import android.provider.MediaStore
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.util.UriUtil
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executor

/** Represents a local content Uri fetch producer. */
class LocalContentUriFetchProducer(
    executor: Executor,
    pooledByteBufferFactory: PooledByteBufferFactory,
    private val contentResolver: ContentResolver
) : LocalFetchProducer(executor, pooledByteBufferFactory) {

  @Throws(IOException::class)
  override fun getEncodedImage(imageRequest: ImageRequest): EncodedImage? {
    val uri = imageRequest.sourceUri
    if (UriUtil.isLocalContactUri(uri)) {
      val inputStream: InputStream?
      if (uri.toString().endsWith("/photo")) {
        inputStream = contentResolver.openInputStream(uri)
      } else if (uri.toString().endsWith("/display_photo")) {
        inputStream =
            try {
              val fd = contentResolver.openAssetFileDescriptor(uri, "r")
              checkNotNull(fd)
              fd!!.createInputStream()
            } catch (e: IOException) {
              throw IOException("Contact photo does not exist: $uri")
            }
      } else {
        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, uri)
        if (inputStream == null) {
          throw IOException("Contact photo does not exist: $uri")
        }
      }
      checkNotNull(inputStream)
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getEncodedImage(inputStream!!, EncodedImage.UNKNOWN_STREAM_SIZE)
    }
    if (UriUtil.isLocalCameraUri(uri)) {
      val cameraImage = getCameraImage(uri)
      if (cameraImage != null) {
        return cameraImage
      }
    }
    return getEncodedImage(
        checkNotNull(contentResolver.openInputStream(uri)), EncodedImage.UNKNOWN_STREAM_SIZE)
  }

  @Throws(IOException::class)
  private fun getCameraImage(uri: Uri): EncodedImage? {
    val parcelFileDescriptor: ParcelFileDescriptor? =
        try {
          contentResolver.openFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
          return null
        }
    checkNotNull(parcelFileDescriptor)
    val fd = parcelFileDescriptor!!.fileDescriptor
    val encodedImage =
        this.getEncodedImage(FileInputStream(fd), parcelFileDescriptor.statSize.toInt())
    parcelFileDescriptor.close()
    return encodedImage
  }

  override fun getProducerName(): String = PRODUCER_NAME

  companion object {
    const val PRODUCER_NAME = "LocalContentUriFetchProducer"
    private val PROJECTION =
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.DATA)
  }
}
