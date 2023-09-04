/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.memory.config

object MemorySpikeConfig {

  private var _avoidObjectsHashCode = false

  @JvmStatic
  fun setAvoidObjectsHashCode(avoidObjectsHashCode: Boolean) {
    _avoidObjectsHashCode = avoidObjectsHashCode
  }

  @JvmStatic fun avoidObjectsHashCode(): Boolean = _avoidObjectsHashCode
}
