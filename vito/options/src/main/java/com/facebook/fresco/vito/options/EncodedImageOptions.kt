/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import com.facebook.common.internal.Objects
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.request.ImageRequest.CacheChoice
import com.facebook.imagepipeline.request.ImageRequestBuilder.BuilderException

open class EncodedImageOptions(builder: Builder<*>) {
  val priority: Priority? = builder.priority
  val cacheChoice: CacheChoice? = builder.cacheChoice
  val diskCacheId: String? = builder.diskCacheId

  init {
    if (builder.cacheChoice == CacheChoice.DYNAMIC) {
      if (diskCacheId == null) {
        throw BuilderException("Disk cache id must be set for dynamic cache choice")
      }
    } else if (!diskCacheId.isNullOrEmpty()) {
      throw BuilderException(
          "Ensure that if you want to use a disk cache id, you set the CacheChoice to DYNAMIC")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    return equalEncodedOptions(other as EncodedImageOptions)
  }

  protected fun equalEncodedOptions(other: EncodedImageOptions): Boolean {
    return Objects.equal(priority, other.priority) &&
        Objects.equal(cacheChoice, other.cacheChoice) &&
        Objects.equal(diskCacheId, other.diskCacheId)
  }

  override fun hashCode(): Int {
    var result = priority?.hashCode() ?: 0
    result = 31 * result + (cacheChoice?.hashCode() ?: 0)
    result = 31 * result + (diskCacheId?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String = toStringHelper().toString()

  protected open fun toStringHelper(): Objects.ToStringHelper =
      Objects.toStringHelper(this)
          .add("priority", priority)
          .add("cacheChoice", cacheChoice)
          .add("diskCacheId", diskCacheId)

  open class Builder<T : Builder<T>> {
    internal var priority: Priority? = null
    internal var cacheChoice: CacheChoice? = null
    internal var diskCacheId: String? = null

    protected constructor()

    protected constructor(defaultOptions: EncodedImageOptions) {
      priority = defaultOptions.priority
      cacheChoice = defaultOptions.cacheChoice
      diskCacheId = defaultOptions.diskCacheId
    }

    fun priority(priority: Priority?): T = modify { this.priority = priority }

    fun cacheChoice(cacheChoice: CacheChoice?): T = modify { this.cacheChoice = cacheChoice }

    fun diskCacheId(diskCacheId: String?): T = modify { this.diskCacheId = diskCacheId }

    open fun build(): EncodedImageOptions = EncodedImageOptions(this)

    protected fun getThis(): T = this as T

    private inline fun modify(block: Builder<T>.() -> Unit): T {
      block()
      return getThis()
    }
  }
}
