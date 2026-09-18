/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

data class PrefetchData(
    val cacheKeyString: String? = null,
    val startTimeMs: Long = ImagePerfData.UNSET,
    val endTimeMs: Long = ImagePerfData.UNSET,
    val origin: String? = null,
    val originSub: String? = null,
    val prefetchCount: Int = 1,
)
