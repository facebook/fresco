/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport

import com.facebook.drawee.generic.RoundingParams
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.RoundingOptions

object RoundingParamsWrapper {

  @JvmStatic
  fun roundingOptionsFromRoundingParams(roundingParams: RoundingParams?): RoundingOptions? {
    return when {
      roundingParams == null -> null
      roundingParams.roundAsCircle -> RoundingOptions.asCircle(true)
      roundingParams.cornersRadii != null ->
          RoundingOptions.forCornerRadii(roundingParams.cornersRadii!!, true)
      // TODO: we also have an overlay color mode where you specify a color to be drawn on top. Vito
      // does not support this.
      // Probably only used for borders
      else -> null
    }
  }

  @JvmStatic
  fun borderOptionsFromRoundingParams(roundingParams: RoundingParams?): BorderOptions? =
      if (roundingParams == null || roundingParams.borderWidth <= 0f) {
        null
      } else {
        BorderOptions.create(
            roundingParams.borderColor,
            roundingParams.borderWidth,
            roundingParams.padding,
            roundingParams.scaleDownInsideBorders)
      }
}
