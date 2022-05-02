/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.ninepatch

import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.samples.showcase.LithoSample
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.litho.FrescoVitoImage2
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext

class LithoNinePatchSample : LithoSample {

  override fun createLithoComponent(
      c: ComponentContext,
      uris: ImageUriProvider,
      callerContext: Any
  ): Component {
    val imageOptions =
        ImageOptions.create()
            .imageDecodeOptions(
                ImageDecodeOptions.newBuilder()
                    .setCustomImageDecoder(NinePatchExample.NinePatchDecoder())
                    .build())
            .customDrawableFactory(NinePatchExample.NinePatchDrawableFactory(c.resources))
            .scale(ScalingUtils.ScaleType.FIT_XY)
            .build()
    return FrescoVitoImage2.create(c)
        .uri(uris.createNinePatchUri())
        .imageOptions(imageOptions)
        .build()
  }
}
