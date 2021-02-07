/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.uil;

import android.content.Context;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.configs.ConfigConstants;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Provides instance of ImageLoader with appropriately configured caches and placeholder/failure
 * drawables.
 */
public class SampleUilFactory {
  private static ImageLoader sImageLoader;

  public static ImageLoader getImageLoader(Context context) {
    if (sImageLoader == null) {
      DisplayImageOptions displayImageOptions =
          new DisplayImageOptions.Builder()
              .showImageOnLoading(Drawables.sPlaceholderDrawable)
              .showImageOnFail(Drawables.sErrorDrawable)
              .cacheInMemory(true)
              .cacheOnDisk(true)
              .build();
      ImageLoaderConfiguration config =
          new ImageLoaderConfiguration.Builder(context)
              .defaultDisplayImageOptions(displayImageOptions)
              .diskCacheSize(ConfigConstants.MAX_DISK_CACHE_SIZE)
              .memoryCacheSize(ConfigConstants.MAX_MEMORY_CACHE_SIZE)
              .build();
      sImageLoader = ImageLoader.getInstance();
      sImageLoader.init(config);
    }
    return sImageLoader;
  }
}
