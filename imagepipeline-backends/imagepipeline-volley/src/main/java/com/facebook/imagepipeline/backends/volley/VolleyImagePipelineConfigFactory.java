/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.volley;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

/**
 * Factory for getting a {@link com.facebook.imagepipeline.core.ImagePipelineConfig} that uses
 * {@link VolleyNetworkFetcher}.
 */
public class VolleyImagePipelineConfigFactory {

  public static ImagePipelineConfig.Builder newBuilder(
      Context context,
      RequestQueue requestQueue) {
    return ImagePipelineConfig.newBuilder(context)
        .setNetworkFetcher(new VolleyNetworkFetcher(requestQueue));
  }
}
