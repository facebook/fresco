/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.net.Uri;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import javax.annotation.Nullable;

public interface FrescoVitoPrefetcher {

  DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      Uri uri,
      @Nullable ImageOptions imageOptions,
      @Nullable Object callerContext);

  DataSource<Void> prefetchToBitmapCache(
      Uri uri, @Nullable DecodedImageOptions imageOptions, @Nullable Object callerContext);

  DataSource<Void> prefetchToEncodedCache(
      Uri uri, @Nullable EncodedImageOptions imageOptions, @Nullable Object callerContext);

  DataSource<Void> prefetchToDiskCache(
      Uri uri, @Nullable ImageOptions imageOptions, @Nullable Object callerContext);

  @Nullable
  DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget, VitoImageRequest imageRequest, @Nullable Object callerContext);
}
