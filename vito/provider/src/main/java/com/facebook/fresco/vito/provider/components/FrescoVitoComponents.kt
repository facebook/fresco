/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.components

import com.facebook.common.logging.FLog
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.VitoImagePipeline
import java.lang.RuntimeException

object FrescoVitoComponents {

  private var _implementation: Implementation? = null

  @JvmStatic
  @Synchronized
  fun getController(): FrescoController2 = getImplementation().getController()

  @JvmStatic
  @Synchronized
  fun getPrefetcher(): FrescoVitoPrefetcher = getImplementation().getPrefetcher()

  @JvmStatic
  @Synchronized
  fun getImagePipeline(): VitoImagePipeline = getImplementation().getImagePipeline()

  @JvmStatic @Synchronized fun getConfig(): FrescoVitoConfig = getImplementation().getConfig()

  /**
   * Reset the implementation. This will remove any implementation currently set up and has to be
   * used with caution.
   */
  @JvmStatic
  @Synchronized
  fun resetImplementation() {
    _implementation = null
  }

  @JvmStatic @Synchronized fun hasBeenInitialized(): Boolean = _implementation != null

  // We do not allow to re-initialize Vito directly.
  // You can use #resetImplementation() if you must manually tear down Vito.
  @JvmStatic
  @Synchronized
  fun getImplementation(): Implementation {
    return _implementation ?: throw RuntimeException("Fresco context provider must be set")
  }

  @JvmStatic
  @Synchronized
  fun setImplementation(implementation: Implementation) {
    // We do not allow to re-initialize Vito directly.
    // You can use #resetImplementation() if you must manually tear down Vito.
    if (_implementation != null) {
      FLog.e(
          "FrescoVitoProvider",
          "Fresco Vito already initialized! Vito must be initialized only once.")
    }
    _implementation = implementation
  }

  interface Implementation {
    fun getController(): FrescoController2

    fun getPrefetcher(): FrescoVitoPrefetcher

    fun getImagePipeline(): VitoImagePipeline

    fun getConfig(): FrescoVitoConfig
  }
}
