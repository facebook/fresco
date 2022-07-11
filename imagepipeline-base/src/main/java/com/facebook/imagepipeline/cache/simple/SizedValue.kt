/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache.simple

import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableImage

@Suppress("KtDataClass")
data class SizedValue(val value: CloseableReference<CloseableImage>, val size: Int)
