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
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public class FrescoVitoPrefetcher {

  private final FrescoContext mFrescoContext;

  public FrescoVitoPrefetcher(FrescoContext frescoContext) {
    mFrescoContext = frescoContext;
  }

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
  public DataSource<Void> prefetchToBitmapCache(
      final Uri uri,
      final @Nullable DecodedImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest = mFrescoContext.buildImageRequest(uri, imageOptions);
    return mFrescoContext.getImagePipeline().prefetchToBitmapCache(imageRequest, callerContext);
  }

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
  public DataSource<Void> prefetchToEncodedCache(
      final Uri uri,
      final @Nullable EncodedImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest = mFrescoContext.buildEncodedImageRequest(uri, imageOptions);
    return mFrescoContext.getImagePipeline().prefetchToEncodedCache(imageRequest, callerContext);
  }

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
  public DataSource<Void> prefetchToDiskCache(
      final Uri uri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest = mFrescoContext.buildEncodedImageRequest(uri, imageOptions);
    return mFrescoContext.getImagePipeline().prefetchToDiskCache(imageRequest, callerContext);
  }
}
