/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
