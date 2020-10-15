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
import com.facebook.imagepipeline.listener.RequestListener;
import javax.annotation.Nullable;

public interface FrescoVitoPrefetcher {

  /**
   * Prefetch an image to the given {@link PrefetchTarget}
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      Uri uri,
      @Nullable ImageOptions imageOptions,
      @Nullable Object callerContext);

  /**
   * Prefetch an image to the bitmap memory cache (for decoded images). In order to cancel the
   * prefetch, close the {@link DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  DataSource<Void> prefetchToBitmapCache(
      Uri uri, @Nullable DecodedImageOptions imageOptions, @Nullable Object callerContext);

  /**
   * Prefetch an image to the encoded memory cache. In order to cancel the prefetch, close the
   * {@link DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  DataSource<Void> prefetchToEncodedCache(
      Uri uri, @Nullable EncodedImageOptions imageOptions, @Nullable Object callerContext);

  /**
   * Prefetch an image to the disk cache. In order to cancel the prefetch, close the {@link
   * DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  DataSource<Void> prefetchToDiskCache(
      Uri uri, @Nullable ImageOptions imageOptions, @Nullable Object callerContext);

  /**
   * Prefetch an image to the given {@link PrefetchTarget} using a {@link VitoImageRequest}. In
   * order to cancel the prefetch, close the {@link DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param callerContext the caller context for the given image
   * @param requestListener optional request listener
   * @return a DataSource that can safely be ignored.
   */
  DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener);
}
