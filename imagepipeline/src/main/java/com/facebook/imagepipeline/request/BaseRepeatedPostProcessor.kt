/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request

abstract class BaseRepeatedPostProcessor : BasePostprocessor(), RepeatedPostprocessor {

  @get:Synchronized private var callback: RepeatedPostprocessorRunner? = null

  @Synchronized
  override fun setCallback(runner: RepeatedPostprocessorRunner) {
    callback = runner
  }

  fun update() {
    val callback = callback
    callback?.update()
  }
}
