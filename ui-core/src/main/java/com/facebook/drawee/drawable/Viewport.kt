/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

data class Viewport(
    val width: Int,
    val height: Int,
    val scaleType: ScalingUtils.ScaleType,
    val sizingHint: SizingHint = SizingHint.DEFAULT,
)
