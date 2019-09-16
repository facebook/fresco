/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.vito.core.FrescoController;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.DefaultFrescoContext;

/** You must initialize this class before use by calling {#code VitoView.init()}. */
@Deprecated /* Experimental */
public class VitoView {

  private static final Class<?> TAG = VitoView.class;
  private static volatile boolean sIsInitialized = false;

  @SuppressWarnings("deprecation")
  private static VitoView sInstance;

  private static @Nullable FrescoController sController = null;

  private static final View.OnAttachStateChangeListener sOnAttachStateChangeListenerCallback =
      new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          onAttach(view);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          onDetach(view);
        }
      };

  private VitoView() {}

  public static void init(Resources resources) {
    if (sIsInitialized) {
      FLog.w(TAG, "VitoView has already been initialized!");
      return;
    } else {
      sIsInitialized = true;
    }

    sInstance = new VitoView();
    sController = DefaultFrescoContext.get(resources).getController();
  }

  /*
   * Display an image with default options
   */
  public static void show(Uri uri, View target) {
    show(uri, ImageOptions.defaults(), target);
  }

  /*
   * Display an image
   */
  public static void show(Uri uri, ImageOptions imageOptions, final View target) {
    FrescoDrawable frescoDrawable;
    final Drawable background = target.getBackground();
    if (background instanceof FrescoDrawable) {
      frescoDrawable = (FrescoDrawable) background;
    } else {
      frescoDrawable = new FrescoDrawable(true);
      ViewCompat.setBackground(target, frescoDrawable);
    }

    frescoDrawable.setVisibilityCallback(
        new VisibilityCallback() {
          @Override
          public void onVisibilityChange(boolean visible) {
            if (!visible) {
              onDetach(target);
            }
          }

          @Override
          public void onDraw() {
            // NOP
          }
        });

    final FrescoState oldState = getState(target);
    final FrescoState state =
        sController.onPrepare(oldState, uri, null, imageOptions, null, target.getResources(), null);
    state.setFrescoDrawable(frescoDrawable);
    setState(target, state);

    // `addOnAttachStateChangeListener` is not idempotent
    target.removeOnAttachStateChangeListener(sOnAttachStateChangeListenerCallback);
    target.addOnAttachStateChangeListener(sOnAttachStateChangeListenerCallback);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // If the view is already attached, we should tell this to controller.
      if (target.isAttachedToWindow()) {
        onAttach(target);
      }
    } else {
      // Before Kitkat we don't have a good way to know.
      // Normally we expect the view to be already attached, thus we always call `onAttach`.
      onAttach(target);
    }
  }

  private static void onAttach(View view) {
    onAttach(getState(view));
  }

  private static void onDetach(View view) {
    onDetach(getState(view));
  }

  private static void onAttach(FrescoState state) {
    if (!state.isAttached()) {
      sController.onAttach(state, null);
    }
  }

  private static void onDetach(FrescoState state) {
    if (state.isAttached()) {
      sController.onDetach(state);
    }
  }

  private static void setState(View view, FrescoState state) {
    view.setTag(R.id.fresco_vito_tag_state, state);
  }

  private static FrescoState getState(View view) {
    return (FrescoState) view.getTag(R.id.fresco_vito_tag_state);
  }
}
