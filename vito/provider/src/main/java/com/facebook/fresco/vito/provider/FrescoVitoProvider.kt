/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider

import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.provider.components.FrescoVitoComponents

object FrescoVitoProvider {

  @JvmStatic
  @Synchronized
  fun getController(): FrescoController2 = FrescoVitoComponents.getController()

  @JvmStatic
  @Synchronized
  fun getPrefetcher(): FrescoVitoPrefetcher = FrescoVitoComponents.getPrefetcher()

  @JvmStatic
  @Synchronized
  fun getImagePipeline(): VitoImagePipeline = FrescoVitoComponents.getImagePipeline()

  @JvmStatic @Synchronized fun getConfig(): FrescoVitoConfig = FrescoVitoComponents.getConfig()

  @JvmStatic
  @Synchronized
  fun hasBeenInitialized(): Boolean = FrescoVitoComponents.hasBeenInitialized()
}
