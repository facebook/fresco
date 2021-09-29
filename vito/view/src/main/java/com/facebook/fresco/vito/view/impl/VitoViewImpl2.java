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
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.infer.annotation.Nullsafe;

/** You must initialize this class before use by calling {#code VitoView.init()}. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoViewImpl2 implements VitoView.Implementation {

  private final View.OnAttachStateChangeListener mOnAttachStateChangeListenerCallback =
      new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          FrescoDrawableInterface current = getDrawable(view);
          if (current != null) {
            maybeRefetchImage(current);
          }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          FrescoDrawableInterface current = getDrawable(view);
          if (current != null) {
            mController.release(current);
          }
        }
      };

  private final FrescoController2 mController;
  private final VitoImagePipeline mVitoImagePipeline;

  public VitoViewImpl2(FrescoController2 controller, VitoImagePipeline vitoImagePipeline) {
    mController = controller;
    mVitoImagePipeline = vitoImagePipeline;
  }

  @Override
  public void show(
      final ImageSource imageSource,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable ImageListener imageListener,
      final View target) {
    final VitoImageRequest imageRequest =
        mVitoImagePipeline.createImageRequest(target.getResources(), imageSource, imageOptions);

    final FrescoDrawableInterface frescoDrawable = ensureDrawableSet(target);
    Runnable fetchRunnable =
        new Runnable() {
          @Override
          public void run() {
            mController.fetch(
                frescoDrawable, imageRequest, callerContext, null, imageListener, null, null);
          }
        };
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setPersistentFetchRunnable(fetchRunnable);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // If the view is already attached to the window, immediately fetch the image.
      // Otherwise, the fetch will be submitter later when then View is attached.
      if (target.isAttachedToWindow()) {
        fetchRunnable.run();
      }
    } else {
      // Before Kitkat we don't have a good way to know.
      // Normally we expect the view to be already attached, thus we always fetch the image.
      fetchRunnable.run();
    }

    // `addOnAttachStateChangeListener` is not idempotent
    target.removeOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
    target.addOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
  }

  private void maybeRefetchImage(final FrescoDrawableInterface drawable) {
    Runnable fetchRunnable = drawable.getPersistentFetchRunnable();
    if (fetchRunnable != null) {
      fetchRunnable.run();
    }
  }

  /**
   * Ensure that a {@link FrescoDrawableInterface} is set for the given View target
   *
   * @param target the target to use
   * @return The drawable to use for the given target
   */
  private <T extends Drawable & FrescoDrawableInterface> T ensureDrawableSet(final View target) {
    if (target instanceof ImageView) {
      ImageView iv = (ImageView) target;
      Drawable current = iv.getDrawable();
      if (current instanceof FrescoDrawableInterface) {
        return (T) current;
      } else {
        T drawable = createDrawable();
        iv.setImageDrawable(drawable);
        return drawable;
      }
    }
    final Drawable background = target.getBackground();
    if (background instanceof FrescoDrawableInterface) {
      return (T) background;
    }
    T drawable = createDrawable();
    ViewCompat.setBackground(target, drawable);
    return drawable;
  }

  @Nullable
  private static FrescoDrawableInterface getDrawable(final View view) {
    Drawable d =
        view instanceof ImageView ? ((ImageView) view).getDrawable() : view.getBackground();
    return d instanceof FrescoDrawableInterface ? (FrescoDrawableInterface) d : null;
  }

  private <T extends Drawable & FrescoDrawableInterface> T createDrawable() {
    final T frescoDrawable = mController.createDrawable();

    frescoDrawable.setVisibilityCallback(
        new VisibilityCallback() {
          @Override
          public void onVisibilityChange(boolean visible) {
            if (visible) {
              maybeRefetchImage(frescoDrawable);
            } else {
              mController.release(frescoDrawable);
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
