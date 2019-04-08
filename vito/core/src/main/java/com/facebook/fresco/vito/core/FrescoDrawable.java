/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.imagepipeline.image.CloseableImage;
import java.io.Closeable;

public class FrescoDrawable extends FadeDrawable implements Closeable {

  @VisibleForTesting @Nullable CloseableReference<CloseableImage> mImageReference;

  private static final int LAYER_COUNT = 4;

  private static final int PLACEHOLDER_DRAWABLE_INDEX = 0;
  private static final int IMAGE_DRAWABLE_INDEX = 1;
  private static final int PROGRESS_DRAWABLE_INDEX = 2;
  private static final int OVERLAY_DRAWABLE_INDEX = 3;

  public FrescoDrawable() {
    super(new Drawable[LAYER_COUNT], true);
  }

  public FrescoDrawable(boolean allLayersVisible) {
    super(new Drawable[LAYER_COUNT], allLayersVisible);
  }

  public @Nullable Drawable setImage(
      @Nullable Drawable imageDrawable,
      @Nullable CloseableReference<CloseableImage> imageReference) {
    CloseableReference.closeSafely(mImageReference);
    mImageReference = CloseableReference.cloneOrNull(imageReference);
    return setDrawable(IMAGE_DRAWABLE_INDEX, imageDrawable);
  }

  public @Nullable Drawable setImageDrawable(@Nullable Drawable newDrawable) {
    return setImage(newDrawable, null);
  }

  public Drawable setOverlayDrawable(@Nullable Drawable drawable) {
    return setDrawable(OVERLAY_DRAWABLE_INDEX, drawable);
  }

  public @Nullable Drawable setProgressDrawable(@Nullable Drawable drawable) {
    return setDrawable(PROGRESS_DRAWABLE_INDEX, drawable);
  }

  public @Nullable Drawable setPlaceholderDrawable(@Nullable Drawable drawable) {
    return setDrawable(PLACEHOLDER_DRAWABLE_INDEX, drawable);
  }

  public void fadeInImage(int durationMs) {
    setTransitionDuration(durationMs);
    beginBatchMode();
    fadeOutLayer(PLACEHOLDER_DRAWABLE_INDEX);
    fadeOutLayer(PROGRESS_DRAWABLE_INDEX);
    fadeInLayer(IMAGE_DRAWABLE_INDEX);
    endBatchMode();
  }

  public void showImageImmediately() {
    hideLayerImmediately(PLACEHOLDER_DRAWABLE_INDEX);
    showLayerImmediately(IMAGE_DRAWABLE_INDEX);
  }

  public void showOverlayImmediately() {
    showLayerImmediately(OVERLAY_DRAWABLE_INDEX);
  }

  @Override
  public void close() {
    CloseableReference.closeSafely(mImageReference);
    mImageReference = null;
    for (int i = 0; i < LAYER_COUNT; i++) {
      setDrawable(i, null);
    }
  }
}
