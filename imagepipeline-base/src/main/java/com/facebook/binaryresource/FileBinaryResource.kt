/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.binaryresource

import com.facebook.common.internal.Files
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/*
 * Implementation of BinaryResource based on a real file. @see BinaryResource for more details.
 */

class FileBinaryResource private constructor(val file: File) : BinaryResource {

  @Throws(IOException::class) override fun openStream(): InputStream = FileInputStream(file)

  override fun size(): Long = file.length() // 0L if file doesn't exist

  @Throws(IOException::class) override fun read(): ByteArray = Files.toByteArray(file)

  override fun equals(other: Any?): Boolean {
    if (other == null || other !is FileBinaryResource) {
      return false
    }
    return file == other.file
  }

  override fun hashCode(): Int = file.hashCode()

  companion object {
    /*
     * Factory method to create a wrapping BinaryResource without explicitly taking care of null.
     * If the supplied file is null, instead of BinaryResource, null is returned.
     */
    @JvmStatic
    fun createOrNull(file: File?): FileBinaryResource? {
      return file?.let { FileBinaryResource(it) }
    }

    @JvmStatic
    fun create(file: File): FileBinaryResource {
      return FileBinaryResource(file)
    }
  }
}
