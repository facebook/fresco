/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.infer.annotation.Nullsafe;

/** Vito View implementation */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoViewImpl2 {

  private static final View.OnAttachStateChangeListener mOnAttachStateChangeListenerCallback =
      new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          FrescoDrawableInterface current = getDrawable(view);
          if (current != null) {
            maybeFetchImage(current);
          }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          FrescoDrawableInterface current = getDrawable(view);
          if (current != null) {
            FrescoVitoProvider.getController().release(current);
          }
        }
      };

  public static void show(
      final ImageSource imageSource,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable ImageListener imageListener,
      final View target) {
    VitoImageRequest imageRequest =
        FrescoVitoProvider.getImagePipeline()
            .createImageRequest(target.getResources(), imageSource, imageOptions);

    final FrescoDrawableInterface frescoDrawable = ensureDrawableSet(target);
    // The Drawable might be re-purposed before being cleaned up, so we release if necessary.
    VitoImageRequest oldImageRequest = frescoDrawable.getImageRequest();
    if (oldImageRequest != null && !oldImageRequest.equals(imageRequest)) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable);
    }
    // We always set fields required to fetch the image.
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setCallerContext(callerContext);
    frescoDrawable.setImageListener(imageListener);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // If the view is already attached to the window, immediately fetch the image.
      // Otherwise, the fetch will be submitted later when then View is attached.
      if (target.isAttachedToWindow()) {
        maybeFetchImage(frescoDrawable);
      }
    } else {
      // Before Kitkat we don't have a good way to know.
      // Normally we expect the view to be already attached, thus we always fetch the image.
      maybeFetchImage(frescoDrawable);
    }

    // `addOnAttachStateChangeListener` is not idempotent
    target.removeOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
    target.addOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
  }

  private static void maybeFetchImage(final FrescoDrawableInterface drawable) {
    final VitoImageRequest request = drawable.getImageRequest();
    if (request == null) {
      return;
    }
    FrescoVitoProvider.getController()
        .fetch(
            drawable,
            request,
            drawable.getCallerContext(),
            null,
            drawable.getImageListener(),
            null,
            null);
  }

  /**
   * Ensure that a {@link FrescoDrawableInterface} is set for the given View target
   *
   * @param target the target to use
   * @return The drawable to use for the given target
   */
  private static FrescoDrawableInterface ensureDrawableSet(final View target) {
    if (target instanceof ImageView) {
      ImageView iv = (ImageView) target;
      Drawable current = iv.getDrawable();
      if (current instanceof FrescoDrawableInterface) {
        return (FrescoDrawableInterface) current;
      } else {
        FrescoDrawableInterface drawable = createDrawable();
        iv.setImageDrawable((Drawable) drawable);
        return drawable;
      }
    }
    final Drawable background = target.getBackground();
    if (background instanceof FrescoDrawableInterface) {
      return (FrescoDrawableInterface) background;
    }
    FrescoDrawableInterface drawable = createDrawable();
    ViewCompat.setBackground(target, (Drawable) drawable);
    return drawable;
  }

  @Nullable
  private static FrescoDrawableInterface getDrawable(final View view) {
    Drawable d =
        view instanceof ImageView ? ((ImageView) view).getDrawable() : view.getBackground();
    return d instanceof FrescoDrawableInterface ? (FrescoDrawableInterface) d : null;
  }

  private static FrescoDrawableInterface createDrawable() {
    final FrescoDrawableInterface frescoDrawable =
        FrescoVitoProvider.getController().createDrawable();

    frescoDrawable.setVisibilityCallback(
        new VisibilityCallback() {
          @Override
          public void onVisibilityChange(boolean visible) {
            if (!visible) {
              FrescoVitoProvider.getController().release(frescoDrawable);
            } else {
              maybeFetchImage(frescoDrawable);
            }
          }

          @Override
          public void onDraw() {
            // NOP
          }
        });
    return frescoDrawable;
  }
}
