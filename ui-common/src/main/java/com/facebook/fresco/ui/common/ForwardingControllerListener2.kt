/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import android.util.Log
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import java.lang.Exception
import java.util.ArrayList

open class ForwardingControllerListener2<I> : BaseControllerListener2<I>() {

  private val listeners: MutableList<ControllerListener2<I>> = ArrayList(2)

  @Synchronized
  fun addListener(listener: ControllerListener2<I>) {
    listeners.add(listener)
  }

  @Synchronized
  fun removeListener(listener: ControllerListener2<I>) {
    listeners.remove(listener)
  }

  @Synchronized
  fun removeAllListeners() {
    listeners.clear()
  }

  private inline fun forEachListener(methodName: String, block: (ControllerListener2<I>) -> Unit) {
    for (i in listeners.indices) {
      val listener =
          try {
            listeners[i]
          } catch (ignore: IndexOutOfBoundsException) {
            break
          }

      try {
        block(listener)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        Log.e(TAG, "InternalListener exception in $methodName", exception)
      }
    }
  }

  override fun onSubmit(id: String, callerContext: Any?, extras: Extras?) {
    forEachListener("onSubmit") { it.onSubmit(id, callerContext, extras) }
  }

  override fun onFinalImageSet(id: String, imageInfo: I?, extraData: Extras?) {
    forEachListener("onFinalImageSet") { it.onFinalImageSet(id, imageInfo, extraData) }
  }

  override fun onFailure(id: String, throwable: Throwable?, extras: Extras?) {
    forEachListener("onFailure") { it.onFailure(id, throwable, extras) }
  }

  override fun onRelease(id: String, extras: Extras?) {
    forEachListener("onRelease") { it.onRelease(id, extras) }
  }

  override fun onIntermediateImageSet(id: String, imageInfo: I?) {
    forEachListener("onIntermediateImageSet") { it.onIntermediateImageSet(id, imageInfo) }
  }

  override fun onIntermediateImageFailed(id: String) {
    forEachListener("onIntermediateImageFailed") { it.onIntermediateImageFailed(id) }
  }

  override fun onEmptyEvent(callerContext: Any?) {
    forEachListener("onEmptyEvent") { it.onEmptyEvent(callerContext) }
  }

  companion object {
    private const val TAG = "FwdControllerListener2"
  }
}
