/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.volley;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.samples.comparison.configs.ConfigConstants;
import java.io.File;

/** Creates singletons of relevant volley classes */
public class SampleVolleyFactory {
  private static final String VOLLEY_CACHE_DIR = "volley";

  private static ImageLoader sImageLoader;
  private static RequestQueue sRequestQueue;
  private static VolleyMemoryCache sMemoryCache;

  public static RequestQueue getRequestQueue(Context context) {
    if (sRequestQueue == null) {
      File cacheDir = new File(context.getCacheDir(), VOLLEY_CACHE_DIR);
      sRequestQueue =
          new RequestQueue(
              new DiskBasedCache(cacheDir, ConfigConstants.MAX_DISK_CACHE_SIZE),
              new BasicNetwork(new HurlStack()));
      sRequestQueue.start();
    }
    return sRequestQueue;
  }

  public static VolleyMemoryCache getMemoryCache() {
    if (sMemoryCache == null) {
      sMemoryCache = new VolleyMemoryCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE);
    }
    return sMemoryCache;
  }

  public static ImageLoader getImageLoader(Context context) {
    if (sImageLoader == null) {
      sImageLoader = new ImageLoader(getRequestQueue(context), getMemoryCache());
    }
    return sImageLoader;
  }
}
