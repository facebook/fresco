/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3;

import android.content.Context;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import okhttp3.OkHttpClient;

/**
 * Factory for getting an {@link com.facebook.imagepipeline.core.ImagePipelineConfig} that uses
 * {@link OkHttpNetworkFetcher}.
 */
public class OkHttpImagePipelineConfigFactory {

  public static ImagePipelineConfig.Builder newBuilder(Context context, OkHttpClient okHttpClient) {
    return ImagePipelineConfig.newBuilder(context)
        .setNetworkFetcher(new OkHttpNetworkFetcher(okHttpClient));
  }
}
