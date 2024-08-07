/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface DrawableDataSubscriber {
  void onNewResult(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource);

  void onFailure(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource);

  void onProgressUpdate(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource);

  void onRelease(FrescoDrawable2Impl drawable);
}
