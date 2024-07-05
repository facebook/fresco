/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig.DefaultPrefetchConfig

open class DefaultFrescoVitoConfig
@JvmOverloads
constructor(override val prefetchConfig: PrefetchConfig = DefaultPrefetchConfig()) :
    FrescoVitoConfig {

  override fun submitFetchOnBgThread(): Boolean = true

  override fun useBindOnly(): Boolean = false

  override fun useNewReleaseCallback(): Boolean = false

  override fun useNativeRounding(): Supplier<Boolean>? = Suppliers.BOOLEAN_TRUE

  override fun layoutPrefetchingEnabled(callerContext: Any?): Boolean = false

  override fun useSmartPropertyDiffing(): Boolean = false

  override fun stopAnimationInOnRelease(): Boolean = false

  override fun onlyStopAnimationWhenAutoPlayEnabled(): Boolean = true

  override fun fastPathForEmptyRequests(): Boolean = false

  override fun enableWindowWideColorGamut(): Boolean = false

  override fun handleImageResultInBackground(): Boolean = false

  override fun useIntermediateImagesAsPlaceholder(): Boolean = false

  open class DefaultPrefetchConfig : PrefetchConfig {
    override fun prefetchInOnPrepare(): Boolean = true

    override fun prefetchTargetOnPrepare(): PrefetchTarget = PrefetchTarget.MEMORY_DECODED
  }
}
