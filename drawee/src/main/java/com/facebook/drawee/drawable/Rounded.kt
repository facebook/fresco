/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable
/** Interface for Drawables that round corners or form a circle. */
interface Rounded {

  var isCircle: Boolean

  fun setRadius(radius: Float)

  var radii: FloatArray

  fun setBorder(color: Int, width: Float)

  val borderColor: Int
  val borderWidth: Float
  var padding: Float
  var scaleDownInsideBorders: Boolean
  var paintFilterBitmap: Boolean

  fun setRepeatEdgePixels(repeatEdgePixels: Boolean)
}
