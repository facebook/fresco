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

open class EncodedImageOptions(builder: Builder<*>) {
  val priority: Priority? = builder.priority
  val cacheChoice: CacheChoice? = builder.cacheChoice

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
    return Objects.equal(priority, other.priority) && Objects.equal(cacheChoice, other.cacheChoice)
  }

  override fun hashCode(): Int {
    val result = priority?.hashCode() ?: 0
    return 31 * result + (cacheChoice?.hashCode() ?: 0)
  }

  override fun toString(): String = toStringHelper().toString()

  protected open fun toStringHelper(): Objects.ToStringHelper =
      Objects.toStringHelper(this).add("priority", priority).add("cacheChoice", cacheChoice)

  open class Builder<T : Builder<T>> {
    internal var priority: Priority? = null
    internal var cacheChoice: CacheChoice? = null

    protected constructor()

    protected constructor(defaultOptions: EncodedImageOptions) {
      priority = defaultOptions.priority
      cacheChoice = defaultOptions.cacheChoice
    }

    fun priority(priority: Priority?): T = modify { this.priority = priority }

    fun cacheChoice(cacheChoice: CacheChoice?): T = modify { this.cacheChoice = cacheChoice }

    open fun build(): EncodedImageOptions = EncodedImageOptions(this)

    protected fun getThis(): T = this as T

    private inline fun modify(block: Builder<T>.() -> Unit): T {
      block()
      return getThis()
    }
  }
}
