/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable

fun Drawable?.toggleAnimation(animationEnabled: Boolean) {
  when (this) {
    is Animatable -> if (animationEnabled) start() else stop()
  }
}
