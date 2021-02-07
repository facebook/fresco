/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.glide;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.facebook.samples.comparison.configs.ConfigConstants;

@GlideModule
public class SampleGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(final Context context, GlideBuilder builder) {
    builder.setDiskCache(
        new DiskCache.Factory() {
          @Override
          public DiskCache build() {
            return DiskLruCacheWrapper.get(
                Glide.getPhotoCacheDir(context), ConfigConstants.MAX_DISK_CACHE_SIZE);
          }
        });
    builder.setMemoryCache(new LruResourceCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE));
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
