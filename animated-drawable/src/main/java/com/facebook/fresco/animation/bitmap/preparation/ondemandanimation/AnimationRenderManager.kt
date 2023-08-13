/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import com.facebook.fresco.animation.backend.AnimationInformation

object AnimationRenderManager {

  private val frameLoaders = mutableSetOf<FrameLoader>()

  @Synchronized
  fun allocate(
      frameLoaderFactory: FrameLoaderFactory,
      animationInformation: AnimationInformation
  ): FrameLoader {
    val frameLoader = frameLoaderFactory.cacheLoader(animationInformation)
    frameLoaders.add(frameLoader)
    return frameLoader
  }

  @Synchronized
  fun deallocate(frameLoader: FrameLoader) {
    frameLoaders.remove(frameLoader)
  }
}
