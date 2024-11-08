/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import android.net.Uri
import com.facebook.infer.annotation.PropagatesNullable
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.JvmField

interface ControllerListener2<INFO> {

  class Extras {
    @JvmField var componentExtras: Map<String, Any>? = null
    @JvmField var shortcutExtras: Map<String, Any>? = null
    @JvmField var datasourceExtras: Map<String, Any>? = null
    @JvmField var imageExtras: Map<String, Any>? = null
    @JvmField var imageSourceExtras: Map<String, Any>? = null
    @JvmField var callerContext: Any? = null
    @JvmField var mainUri: Uri? = null
    @JvmField var viewportWidth: Int = -1
    @JvmField var viewportHeight: Int = -1
    @JvmField var scaleType: Any? = null
    @JvmField var focusX: Float? = null
    @JvmField var focusY: Float? = null
    @JvmField var logWithHighSamplingRate: Boolean = false
    @JvmField var modifiedUriStatus: String? = null
    @JvmField var originalUri: Uri? = null
    @JvmField var uiFramework: String? = null

    fun makeExtrasCopy(): Extras {
      val extras = Extras()
      extras.componentExtras = copyMap(componentExtras)
      extras.shortcutExtras = copyMap(shortcutExtras)
      extras.datasourceExtras = copyMap(datasourceExtras)
      extras.imageExtras = copyMap(imageExtras)
      extras.callerContext = callerContext
      extras.mainUri = mainUri
      extras.viewportWidth = viewportWidth
      extras.viewportHeight = viewportHeight
      extras.scaleType = scaleType
      extras.focusX = focusX
      extras.focusY = focusY
      extras.uiFramework = uiFramework
      return extras
    }

    companion object {
      @JvmStatic
      fun of(componentExtras: Map<String, Any>?): Extras {
        val extras = Extras()
        extras.componentExtras = componentExtras
        return extras
      }

      private fun copyMap(@PropagatesNullable map: Map<String, Any>?): Map<String, Any>? =
          map?.let(::ConcurrentHashMap)
    }
  }

  /**
   * Called before the image request is submitted.
   *
   * IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   * @param callerContext caller context
   * @param extraData extra data
   */
  fun onSubmit(id: String, callerContext: Any?, extraData: Extras?)

  /**
   * Called after the final image has been set.
   *
   * @param id controller id
   * @param imageInfo image info
   * @param extraData extra data
   */
  fun onFinalImageSet(id: String, imageInfo: INFO?, extraData: Extras?)

  /**
   * Called after any intermediate image has been set.
   *
   * @param id controller id
   */
  fun onIntermediateImageSet(id: String, imageInfo: INFO?)

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param id controller id
   */
  fun onIntermediateImageFailed(id: String)

  /**
   * Called after the fetch of the final image failed.
   *
   * @param id controller id
   * @param throwable failure cause
   * @param extraData extra data
   */
  fun onFailure(id: String, throwable: Throwable?, extraData: Extras?)

  /**
   * Called after the controller released the fetched image.
   *
   * IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   * @param extraData extra data
   */
  fun onRelease(id: String, extraData: Extras?)

  fun onEmptyEvent(callerContext: Any?)
}
