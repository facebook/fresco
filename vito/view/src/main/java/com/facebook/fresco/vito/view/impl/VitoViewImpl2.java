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
import com.facebook.fresco.vito.core.FrescoDrawable2;
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
          FrescoDrawable2 current = getDrawable(view);
          if (current != null) {
            VitoImageRequest request = current.getImageRequest();
            if (request != null) {
              fetchImage(current, request);
            }
          }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          FrescoDrawable2 current = getDrawable(view);
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
    VitoImageRequest imageRequest =
        mVitoImagePipeline.createImageRequest(target.getResources(), imageSource, imageOptions);

    final FrescoDrawable2 frescoDrawable = ensureDrawableSet(target);
    // The Drawable might be re-purposed before being cleaned up, so we release if necessary.
    VitoImageRequest oldImageRequest = frescoDrawable.getImageRequest();
    if (oldImageRequest != null && !oldImageRequest.equals(imageRequest)) {
      mController.release(frescoDrawable);
    }
    // We always set fields required to fetch the image.
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setCallerContext(callerContext);
    frescoDrawable.setImageListener(imageListener);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // If the view is already attached to the window, immediately fetch the image.
      // Otherwise, the fetch will be submitter later when then View is attached.
      if (target.isAttachedToWindow()) {
        fetchImage(frescoDrawable, imageRequest);
      }
    } else {
      // Before Kitkat we don't have a good way to know.
      // Normally we expect the view to be already attached, thus we always fetch the image.
      fetchImage(frescoDrawable, imageRequest);
    }

    // `addOnAttachStateChangeListener` is not idempotent
    target.removeOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
    target.addOnAttachStateChangeListener(mOnAttachStateChangeListenerCallback);
  }

  private void fetchImage(final FrescoDrawable2 drawable, final VitoImageRequest request) {
    mController.fetch(
        drawable,
        request,
        drawable.getCallerContext(),
        drawable.getImageListener().getImageListener(),
        null,
        null);
  }

  /**
   * Ensure that a {@link FrescoDrawable2} is set for the given View target
   *
   * @param target the target to use
   * @return The drawable to use for the given target
   */
  private FrescoDrawable2 ensureDrawableSet(final View target) {
    if (target instanceof ImageView) {
      ImageView iv = (ImageView) target;
      Drawable current = iv.getDrawable();
      if (current instanceof FrescoDrawable2) {
        return (FrescoDrawable2) current;
      } else {
        FrescoDrawable2 drawable = createDrawable();
        iv.setImageDrawable(drawable);
        return drawable;
      }
    }
    final Drawable background = target.getBackground();
    if (background instanceof FrescoDrawable2) {
      return (FrescoDrawable2) background;
    }
    FrescoDrawable2 drawable = createDrawable();
    ViewCompat.setBackground(target, drawable);
    return drawable;
  }

  @Nullable
  private static FrescoDrawable2 getDrawable(final View view) {
    Drawable d =
        view instanceof ImageView ? ((ImageView) view).getDrawable() : view.getBackground();
    return d instanceof FrescoDrawable2 ? (FrescoDrawable2) d : null;
  }

  private FrescoDrawable2 createDrawable() {
    final FrescoDrawable2 frescoDrawable = mController.createDrawable();

    frescoDrawable.setVisibilityCallback(
        new VisibilityCallback() {
          @Override
          public void onVisibilityChange(boolean visible) {
            if (!visible) {
              mController.release(frescoDrawable);
            } else {
              VitoImageRequest request = frescoDrawable.getImageRequest();
              if (request != null) {
                fetchImage(frescoDrawable, request);
              }
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
