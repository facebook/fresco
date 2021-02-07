/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.picasso;

import android.content.Context;
import com.facebook.samples.comparison.configs.ConfigConstants;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

/** Provides instance of Picasso with common configuration for the sample app */
public class SamplePicassoFactory {

  private static Picasso sPicasso;

  public static Picasso getPicasso(Context context) {
    if (sPicasso == null) {
      sPicasso =
          new Picasso.Builder(context)
              .downloader(new OkHttp3Downloader(context, ConfigConstants.MAX_DISK_CACHE_SIZE))
              .memoryCache(new LruCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE))
              .build();
    }
    return sPicasso;
  }
}
