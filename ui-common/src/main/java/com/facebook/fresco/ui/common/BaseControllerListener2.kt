/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import com.facebook.fresco.ui.common.ControllerListener2.Extras

open class BaseControllerListener2<INFO> : ControllerListener2<INFO> {

  override fun onSubmit(id: String, callerContext: Any?, extras: Extras?): Unit = Unit

  override fun onFinalImageSet(id: String, imageInfo: INFO?, extraData: Extras?): Unit = Unit

  override fun onIntermediateImageSet(id: String, imageInfo: INFO?): Unit = Unit

  override fun onIntermediateImageFailed(id: String): Unit = Unit

  override fun onFailure(id: String, throwable: Throwable?, extras: Extras?): Unit = Unit

  override fun onRelease(id: String, extras: Extras?): Unit = Unit

  override fun onEmptyEvent(callerContext: Any?): Unit = Unit

  companion object {
    private val NO_OP_LISTENER: ControllerListener2<*> = BaseControllerListener2<Any?>()

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <I> getNoOpListener(): ControllerListener2<I> = NO_OP_LISTENER as ControllerListener2<I>
  }
}
