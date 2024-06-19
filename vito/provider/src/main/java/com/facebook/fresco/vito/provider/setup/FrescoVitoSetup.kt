/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.setup

import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.VitoImagePipeline

interface FrescoVitoSetup {

  fun getController(): FrescoController2

  fun getPrefetcher(): FrescoVitoPrefetcher

  fun getImagePipeline(): VitoImagePipeline

  fun getConfig(): FrescoVitoConfig
}
