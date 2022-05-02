/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.binaryresource

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/** A trivial implementation of BinaryResource that wraps a byte array */
class ByteArrayBinaryResource(private val bytes: ByteArray) : BinaryResource {

  override fun size(): Long = bytes.size.toLong()

  @Throws(IOException::class) override fun openStream(): InputStream = ByteArrayInputStream(bytes)

  /**
   * Get the underlying byte array
   *
   * @return the underlying byte array of this resource
   */
  override fun read(): ByteArray = bytes
}
