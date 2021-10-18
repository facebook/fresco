/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface FrescoDrawableInterface {
  long getImageId();

  void setCallerContext(@Nullable Object callerContext);

  @Nullable
  Object getCallerContext();

  VitoImagePerfListener getImagePerfListener();

  void setMutateDrawables(boolean mutateDrawables);

  @Nullable
  Drawable getActualImageDrawable();

  boolean hasImage();

  boolean isFetchSubmitted();

  @Nullable
  VitoImageRequest getImageRequest();

  void setImageRequest(@Nullable VitoImageRequest imageRequest);

  void setVisibilityCallback(@Nullable VisibilityCallback visibilityCallback);

  void setImageListener(@Nullable ImageListener imageListener);

  @Nullable
  ImageListener getImageListener();

  @Nullable
  Drawable setOverlayDrawable(@Nullable Drawable drawable);

  @Nullable
  Object getExtras();

  void setExtras(@Nullable Object extras);
}
