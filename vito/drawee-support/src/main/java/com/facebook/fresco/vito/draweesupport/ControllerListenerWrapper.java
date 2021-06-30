/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport;

import static com.facebook.infer.annotation.Assertions.nullsafeFIXME;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import androidx.annotation.VisibleForTesting;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.ui.common.OnDrawControllerListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.PropagatesNullable;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ControllerListenerWrapper implements ImageListener {

  /**
   * Create a new controller listener wrapper or return null if the listener is null.
   *
   * @param controllerListener the controller listener to wrap
   * @return the wrapped controller listener or null if no wrapping required
   */
  public static ControllerListenerWrapper create(
      @PropagatesNullable ControllerListener<ImageInfo> controllerListener) {
    return controllerListener == null ? null : new ControllerListenerWrapper(controllerListener);
  }

  private final ControllerListener<ImageInfo> mControllerListener;

  @VisibleForTesting
  ControllerListenerWrapper(ControllerListener<ImageInfo> controllerListener) {
    mControllerListener = controllerListener;
  }

  @Override
  public void onSubmit(long id, @Nullable Object callerContext) {
    mControllerListener.onSubmit(
        toStringId(id), nullsafeFIXME(callerContext, "Legacy ControllerListener is not nullsafe"));
  }

  @Override
  public void onPlaceholderSet(long id, @Nullable Drawable placeholder) {
    // Not present in old API
  }

  @Override
  public void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable) {
    Animatable animatable = drawable instanceof Animatable ? (Animatable) drawable : null;
    mControllerListener.onFinalImageSet(toStringId(id), imageInfo, animatable);
  }

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {
    mControllerListener.onIntermediateImageSet(toStringId(id), imageInfo);
  }

  @Override
  public void onIntermediateImageFailed(long id, @Nullable Throwable throwable) {
    mControllerListener.onIntermediateImageFailed(
        toStringId(id), nullsafeFIXME(throwable, "Legacy ControllerListener is not nullsafe"));
  }

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {
    mControllerListener.onFailure(
        toStringId(id), nullsafeFIXME(throwable, "Legacy ControllerListener is not nullsafe"));
  }

  @Override
  public void onRelease(long id) {
    mControllerListener.onRelease(toStringId(id));
  }

  private static String toStringId(long id) {
    return "v" + id;
  }

  @Override
  public void onImageDrawn(String id, ImageInfo imageInfo, DimensionsInfo dimensionsInfo) {
    if (mControllerListener instanceof OnDrawControllerListener) {
      ((OnDrawControllerListener) mControllerListener).onImageDrawn(id, imageInfo, dimensionsInfo);
    }
  }
}
