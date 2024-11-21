/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.Supplier

interface FrescoVitoConfig {
  val prefetchConfig: PrefetchConfig

  fun submitFetchOnBgThread(): Boolean

  fun useBindOnly(): Boolean

  fun useNewReleaseCallback(): Boolean

  fun useNativeRounding(): Supplier<Boolean>?

  fun layoutPrefetchingEnabled(callerContext: Any?): Boolean

  fun useSmartPropertyDiffing(): Boolean

  fun onlyStopAnimationWhenAutoPlayEnabled(): Boolean

  fun fastPathForEmptyRequests(): Boolean

  fun enableWindowWideColorGamut(): Boolean

  fun handleImageResultInBackground(): Boolean

  fun useIntermediateImagesAsPlaceholder(): Boolean

  fun fallbackToDefaultImageOptions(): Boolean

  fun experimentalDynamicSizeVito2(): Boolean

  fun experimentalDynamicSizeWithCacheFallbackVito2(): Boolean

  fun experimentalDynamicSizeOnPrepareMainThreadVito2(): Boolean

  fun experimentalDynamicSizeDiskCacheCheckTimeoutMs(): Long

  fun experimentalDynamicSizeUseSfOnDiskCacheTimeout(): Boolean

  fun isAppStarting(): Boolean

  fun experimentalDynamicSizeDisableWhenAppIsStarting(): Boolean

  fun experimentalDynamicSizeCheckIfProductIsEnabled(): Boolean

  fun experimentalDynamicSizeIsProductEnabled(
      callerContext: Any?,
      contextChain: ContextChain?
  ): Boolean

  fun experimentalResetVitoImageRequestListener(): Boolean

  fun experimentalResetLocalVitoImageRequestListener(): Boolean

  fun experimentalResetLocalImagePerfStateListener(): Boolean

  fun experimentalResetControllerListener2(): Boolean
}
