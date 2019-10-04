/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.volley;

import android.graphics.Bitmap;
import androidx.collection.LruCache;
import com.android.volley.toolbox.ImageLoader;

/** Default bitmap memory cache for Volley. */
public class VolleyMemoryCache implements ImageLoader.ImageCache {
  private final LruCache<String, Bitmap> mLruCache;

  public VolleyMemoryCache(int maxSize) {
    mLruCache =
        new LruCache<String, Bitmap>(maxSize) {
          protected int sizeOf(final String key, final Bitmap value) {
            return value.getRowBytes() * value.getHeight();
          }
        };
  }

  @Override
  public Bitmap getBitmap(String url) {
    return mLruCache.get(url);
  }

  @Override
  public void putBitmap(String url, Bitmap bitmap) {
    mLruCache.put(url, bitmap);
  }

  public void clear() {
    mLruCache.evictAll();
  }
}
