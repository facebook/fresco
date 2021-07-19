/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpFrescoVitoPrefetcher implements FrescoVitoPrefetcher {

  private static final String EXCEPTION_MSG = "Image prefetching with Fresco Vito is disabled!";

  @Override
  public DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      Uri uri,
      @Nullable ImageOptions imageOptions,
      @Nullable Object callerContext,
      String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public DataSource<Void> prefetchToBitmapCache(
      Uri uri,
      @Nullable DecodedImageOptions imageOptions,
      @Nullable Object callerContext,
      String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public DataSource<Void> prefetchToEncodedCache(
      Uri uri,
      @Nullable EncodedImageOptions imageOptions,
      @Nullable Object callerContext,
      String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public DataSource<Void> prefetchToDiskCache(
      Uri uri,
      @Nullable ImageOptions imageOptions,
      @Nullable Object callerContext,
      String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener,
      String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }
}
