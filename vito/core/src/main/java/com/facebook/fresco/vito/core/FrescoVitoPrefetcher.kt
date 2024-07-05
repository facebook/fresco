/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.listener.RequestListener

interface FrescoVitoPrefetcher {

  /**
   * Prefetch an image to the given [PrefetchTarget]
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the given [PrefetchTarget]
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param contextChain the context chain for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the bitmap memory cache (for decoded images). In order to cancel the
   * prefetch, close the [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the bitmap memory cache (for decoded images). In order to cancel the
   * prefetch, close the [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param contextChain the context chain for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the encoded memory cache. In order to cancel the prefetch, close the
   * [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the encoded memory cache. In order to cancel the prefetch, close the
   * [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param contextChain the context chain for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the disk cache. In order to cancel the prefetch, close the [DataSource]
   * returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the disk cache. In order to cancel the prefetch, close the [DataSource]
   * returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @param contextChain the context chain for the given image
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the given [PrefetchTarget] using a [VitoImageRequest]. In order to cancel
   * the prefetch, close the [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param callerContext the caller context for the given image
   * @param requestListener optional request listener
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?>

  /**
   * Prefetch an image to the given [PrefetchTarget] using a [VitoImageRequest]. In order to cancel
   * the prefetch, close the [DataSource] returned by this method.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param callerContext the caller context for the given image
   * @param contextChain the context chain for the given image
   * @param requestListener optional request listener
   * @param callsite the prefetch callsite from which this request is being made, for logging
   * @return a DataSource that can safely be ignored.
   */
  fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?>
}
