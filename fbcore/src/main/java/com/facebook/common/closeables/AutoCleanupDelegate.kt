/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.closeables

import java.io.Closeable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate to call a cleanup function when the value changes (including being set to
 * null). It will NOT trigger the cleanup function if the same value is set again.
 */
open class AutoCleanupDelegate<T>(
    private var currentValue: T? = null,
    private val cleanupFunction: (T) -> Unit
) : ReadWriteProperty<Any?, T?> {

  override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
    val cur = currentValue
    if (cur != null && cur !== value) {
      cleanupFunction(cur)
    }
    currentValue = value
  }

  override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = currentValue
}

class AutoCloseableDelegate<T : Closeable>(initialValue: T? = null) :
    AutoCleanupDelegate<T>(initialValue, closeableCleanupFunction)

private val closeableCleanupFunction: (Closeable) -> Unit = { it.close() }
