/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3

import android.content.Context
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.core.ImagePipelineConfig.Companion.newBuilder
import okhttp3.OkHttpClient

/**
 * Factory for getting an [com.facebook.imagepipeline.core.ImagePipelineConfig] that uses
 * [OkHttpNetworkFetcher].
 */
object OkHttpImagePipelineConfigFactory {
  @JvmStatic
  fun newBuilder(context: Context, okHttpClient: OkHttpClient): ImagePipelineConfig.Builder =
      newBuilder(context).setNetworkFetcher(OkHttpNetworkFetcher(okHttpClient))
}
