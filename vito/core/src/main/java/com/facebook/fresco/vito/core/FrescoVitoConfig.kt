/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.internal.Supplier

interface FrescoVitoConfig {
  val prefetchConfig: PrefetchConfig

  fun submitFetchOnBgThread(): Boolean

  fun useBind(): Boolean

  fun useMount(): Boolean

  fun useUnmount(): Boolean

  fun useUnbind(): Boolean

  fun useDetached(): Boolean

  fun onUnbindReleaseStrategy(): ReleaseStrategy

  fun onUnmountReleaseStrategy(): ReleaseStrategy

  fun onDetachedReleaseStrategy(): ReleaseStrategy

  fun releaseDelayMs(): Long

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

  fun experimentalDynamicSizeBloksDisableDiskCacheCheck(): Boolean

  fun experimentalDynamicSizeWithCacheFallbackVito2(): Boolean

  fun experimentalDynamicSizeOnPrepareMainThreadVito2(): Boolean

  fun experimentalDynamicSizeDiskCacheCheckTimeoutMs(): Long

  fun experimentalDynamicSizeUseSfOnDiskCacheTimeout(): Boolean

  fun isAppStarting(): Boolean

  fun experimentalDynamicSizeDisableWhenAppIsStarting(): Boolean

  fun experimentalDynamicSizeCheckIfProductIsEnabled(): Boolean

  fun experimentalDynamicSizeIsProductEnabled(
      callerContext: Any?,
      contextChain: ContextChain?,
  ): Boolean

  fun experimentalDynamicSizeIsFallbackEnabled(
      callerContext: Any?,
      contextChain: ContextChain?,
  ): Boolean

  fun isCallerContextBloks(callerContext: Any?): Boolean

  fun experimentalResetVitoImageRequestListener(): Boolean

  fun experimentalResetLocalVitoImageRequestListener(): Boolean

  fun experimentalResetLocalImagePerfStateListener(): Boolean

  fun experimentalResetControllerListener2(): Boolean

  fun experimentalDynamicSizeIsUriEligible(uri: Uri?): Boolean

  fun enablePrepareToDrawOnFetch(): Boolean

  fun experimentalOptimizeAlphaHandling(): Boolean

  fun enableRetriggerListenersIfImageAlreadySet(): Boolean

  fun fixOnBindRetriggerListenersClobber(): Boolean = false

  fun disableBitmapCacheShortcut(): Boolean = false

  /**
   * Enables offer-back-on-release for `CloseableBitmap` images. When on, the drawable's image
   * reference is re-offered to the memory cache when the Vito drawable is reset, instead of being
   * closed immediately — letting the cache potentially reuse the entry on the next request.
   *
   * Applies only to `CloseableBitmap` instances; for non-bitmap images use
   * [useOfferBackOnReleaseForNonBitmapImage].
   */
  fun useOfferBackOnRelease(): Boolean = false

  /**
   * Enables offer-back-on-release for non-`CloseableBitmap` images (animated images, XML/SVG
   * decodes, etc.). When the new non-bitmap memory cache is enabled via
   * `experiments.useSeparateNonBitmapImageCache`, offered-back entries land in that cache.
   */
  fun useOfferBackOnReleaseForNonBitmapImage(): Boolean = false

  /**
   * When on, an image is released immediately when its view becomes non-visible (e.g. scrolled off
   * screen) instead of on the next frame. Releasing sooner drops the drawable's strong reference to
   * the underlying bitmap earlier, making it eligible for GC while scrolling rather than only after
   * the view detaches. Trade-off: an item that scrolls off and quickly back triggers a re-fetch
   * (usually a memory-cache hit). Default off; gated behind an A/B flag.
   */
  fun releaseImageOnVisibilityGoneImmediately(): Boolean = false
}
