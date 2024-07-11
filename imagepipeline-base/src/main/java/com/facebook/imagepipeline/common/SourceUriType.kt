/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import androidx.annotation.IntDef

/**
 * This is the interface we use in order to define different types of Uri an ImageRequest can have.
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    SourceUriType.SOURCE_TYPE_UNKNOWN,
    SourceUriType.SOURCE_TYPE_NETWORK,
    SourceUriType.SOURCE_TYPE_LOCAL_FILE,
    SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE,
    SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE,
    SourceUriType.SOURCE_TYPE_LOCAL_CONTENT,
    SourceUriType.SOURCE_TYPE_LOCAL_ASSET,
    SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE,
    SourceUriType.SOURCE_TYPE_DATA,
    SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE)
annotation class SourceUriType {
  companion object {
    const val SOURCE_TYPE_UNKNOWN: Int = -1
    const val SOURCE_TYPE_NETWORK = 0
    const val SOURCE_TYPE_LOCAL_FILE = 1
    const val SOURCE_TYPE_LOCAL_VIDEO_FILE = 2
    const val SOURCE_TYPE_LOCAL_IMAGE_FILE = 3
    const val SOURCE_TYPE_LOCAL_CONTENT = 4
    const val SOURCE_TYPE_LOCAL_ASSET = 5
    const val SOURCE_TYPE_LOCAL_RESOURCE = 6
    const val SOURCE_TYPE_DATA = 7
    const val SOURCE_TYPE_QUALIFIED_RESOURCE = 8
  }
}
