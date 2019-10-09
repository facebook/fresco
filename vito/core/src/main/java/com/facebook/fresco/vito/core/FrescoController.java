/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.net.Uri;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.infer.annotation.ThreadSafe;
import javax.annotation.Nullable;

@ThreadSafe
public interface FrescoController {
  // Lifecycle methods {
  FrescoState createState(
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      Resources resources,
      @Nullable ImageListener imageListener);

  FrescoState onPrepare(
      FrescoState frescoState,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      Resources resources,
      @Nullable ImageListener imageListener);

  void onAttach(FrescoState state, @Nullable ImageListener imageListener);

  void onDetach(FrescoState state);

  // }

  // DataSubscriber methods: {
  void onNewResult(FrescoState state, DataSource<CloseableReference<CloseableImage>> dataSource);

  void onFailure(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource);

  void onCancellation(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource);

  void onProgressUpdate(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource);

  // }
}
