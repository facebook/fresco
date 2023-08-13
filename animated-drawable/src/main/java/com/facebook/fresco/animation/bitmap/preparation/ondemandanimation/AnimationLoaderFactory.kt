/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFrameTaskFactory
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory

class FrameLoaderFactory(
    platformBitmapFactory: PlatformBitmapFactory,
    bitmapFrameRenderer: BitmapFrameRenderer,
    private val bitmapCache: BitmapFrameCache,
) {
  private val loadFrameTaskFactory by lazy {
    LoadFrameTaskFactory(platformBitmapFactory, bitmapFrameRenderer)
  }

  fun cacheLoader(animationInformation: AnimationInformation): FrameLoader =
      CacheFrameLoader(loadFrameTaskFactory, bitmapCache, animationInformation)
}
