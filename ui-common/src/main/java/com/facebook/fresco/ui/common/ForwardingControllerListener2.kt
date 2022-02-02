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

class ForwardingControllerListener2<I> : BaseControllerListener2<I>() {

  @NonNull private val listeners: MutableList<ControllerListener2<I>> = ArrayList(2)

  @Synchronized
  fun addListener(@NonNull listener: ControllerListener2<I>) {
    listeners.add(listener)
  }

  @Synchronized
  fun removeListener(@NonNull listener: ControllerListener2<I>) {
    val index = listeners.indexOf(listener)
    if (index != -1) {
      listeners.removeAt(index)
    }
  }

  @Synchronized
  fun removeAllListeners() {
    listeners.clear()
  }

  @Synchronized
  private fun onException(@NonNull message: String, @NonNull t: Throwable) {
    Log.e(TAG, message, t)
  }

  override fun onSubmit(@NonNull id: String, callerContext: Any?, extras: Extras?) {
    val numberOfListeners = listeners.size
    for (i in 0 until numberOfListeners) {
      try {
        val listener = listeners[i]
        listener?.onSubmit(id, callerContext, extras)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onSubmit", exception)
      }
    }
  }

  override fun onFinalImageSet(@NonNull id: String, imageInfo: I?, extraData: Extras?) {
    val numberOfListeners = listeners.size
    for (i in 0 until numberOfListeners) {
      try {
        val listener = listeners[i]
        listener?.onFinalImageSet(id, imageInfo, extraData)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onFinalImageSet", exception)
      }
    }
  }

  override fun onFailure(@NonNull id: String, throwable: Throwable?, extras: Extras?) {
    val numberOfListeners = listeners.size
    for (i in 0 until numberOfListeners) {
      try {
        val listener = listeners[i]
        listener?.onFailure(id, throwable, extras)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onFailure", exception)
      }
    }
  }

  override fun onRelease(@NonNull id: String, extras: Extras?) {
    val numberOfListeners = listeners.size
    for (i in 0 until numberOfListeners) {
      try {
        val listener = listeners[i]
        listener?.onRelease(id, extras)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onRelease", exception)
      }
    }
  }

  companion object {
    @NonNull private const val TAG = "FwdControllerListener2"
  }
}
