/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.R;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.multiuri.MultiUri;

/** You must initialize this class before use by calling {#code VitoView.init()}. */
@Deprecated /* Experimental */
public class VitoViewImpl implements VitoView.Implementation {

  private final FrescoContext mFrescoContext;

  private final View.OnAttachStateChangeListener sOnAttachStateChangeListenerCallback =
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

  public VitoViewImpl(FrescoContext frescoContext) {
    mFrescoContext = frescoContext;
  }

  @Override
  public void show(
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      @Nullable ImageListener imageListener,
      final View target) {
    Preconditions.checkArgument(
        !(uri != null && multiUri != null), "Setting both a Uri and MultiUri is not allowed!");

    final FrescoState oldState = getState(target);
    final FrescoState state =
        mFrescoContext
            .getController()
            .onPrepare(
                oldState,
                uri,
                multiUri,
                imageOptions,
                callerContext,
                target.getResources(),
                imageListener);
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

  private FrescoDrawable createFrescoDrawable(final View target) {
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
    return frescoDrawable;
  }

  private void onAttach(View view) {
    FrescoDrawable frescoDrawable = createFrescoDrawable(view);
    FrescoState state = getState(view);
    state.setFrescoDrawable(frescoDrawable);
    onAttach(state);
  }

  private void onDetach(View view) {
    onDetach(getState(view));
  }

  private void onAttach(@Nullable FrescoState state) {
    if (state != null && !state.isAttached()) {
      mFrescoContext.getController().onAttach(state, null);
    }
  }

  private void onDetach(@Nullable FrescoState state) {
    if (state != null && state.isAttached()) {
      mFrescoContext.getController().onDetach(state);
    }
  }

  private static void setState(View view, FrescoState state) {
    view.setTag(R.id.fresco_vito_tag_state, state);
  }

  @Nullable
  private static FrescoState getState(View view) {
    return (FrescoState) view.getTag(R.id.fresco_vito_tag_state);
  }
}
