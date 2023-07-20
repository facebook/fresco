/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.middleware.MiddlewareUtils
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.imagepipeline.image.CloseableImage

private val COMPONENT_EXTRAS: Map<String, Any> = mapOf("component_tag" to "vito2")

private val SHORTCUT_EXTRAS: Map<String, Any> =
    mapOf("origin" to "memory_bitmap", "origin_sub" to "shortcut")

fun KFrescoVitoDrawable.obtainExtras(
    dataSource: DataSource<CloseableReference<CloseableImage>>? = null,
    image: CloseableReference<CloseableImage>? = null,
): ControllerListener2.Extras =
    MiddlewareUtils.obtainExtras(
        COMPONENT_EXTRAS,
        SHORTCUT_EXTRAS,
        dataSource?.extras,
        null, // FIXME
        viewportDimensions,
        imageRequest?.imageOptions?.actualImageScaleType.toString(),
        imageRequest?.imageOptions?.actualImageFocusPoint,
        image?.get()?.extras,
        callerContext,
        imageRequest?.logWithHighSamplingRate ?: false,
        imageRequest?.finalImageRequest?.sourceUri)
