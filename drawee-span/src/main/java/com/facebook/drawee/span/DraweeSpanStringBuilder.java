/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.span;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.view.View;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.lifecycle.AttachDetachListener;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.widget.text.span.BetterImageSpan;
import java.util.HashSet;
import java.util.Set;

/**
 * DraweeSpanStringBuilder that can be used to add {@link DraweeSpan}s to strings.
 *
 * <p>The containing view must also call {@link #onDetachFromView(View)} ()} from its {@link
 * View#onStartTemporaryDetach()} and {@link View#onDetachedFromWindow()} methods. Similarly, it
 * must call {@link #onAttachToView(View)} from its {@link View#onFinishTemporaryDetach()} and
 * {@link View#onAttachedToWindow()} methods.
 *
 * <p>If you attach the same DraweeSpanStringBuilder to different views, only the most recent view
 * will be updated correctly since you can only bind the same builder to 1 view at a time. Older
 * views will be automatically unbound.
 *
 * {@see DraweeHolder}
 */
public class DraweeSpanStringBuilder extends SpannableStringBuilder
    implements AttachDetachListener {

  public interface DraweeSpanChangedListener {

    public void onDraweeSpanChanged(DraweeSpanStringBuilder draweeSpanStringBuilder);
  }

  public static final int UNSET_SIZE = -1;

  private final Set<DraweeSpan> mDraweeSpans = new HashSet<>();
  private final DrawableCallback mDrawableCallback = new DrawableCallback();

  private View mBoundView;
  private Drawable mBoundDrawable;
  private DraweeSpanChangedListener mDraweeSpanChangedListener;

  public DraweeSpanStringBuilder() {
    super();
  }

  public DraweeSpanStringBuilder(CharSequence text) {
    super(text);
  }

  public DraweeSpanStringBuilder(CharSequence text, int start, int end) {
    super(text, start, end);
  }

  public void setImageSpan(
      DraweeHolder draweeHolder,
      int index,
      final int drawableWidthPx,
      final int drawableHeightPx,
      boolean enableResizing,
      @BetterImageSpan.BetterImageSpanAlignment int verticalAlignment) {
    setImageSpan(
        draweeHolder,
        index,
        index,
        drawableWidthPx,
        drawableHeightPx,
        enableResizing,
        verticalAlignment);
  }

  public void setImageSpan(
      DraweeHolder draweeHolder,
      int startIndex,
      int endIndex,
      final int drawableWidthPx,
      final int drawableHeightPx,
      boolean enableResizing,
      @BetterImageSpan.BetterImageSpanAlignment int verticalAlignment) {
    if (endIndex >= length()) {
      // Unfortunately, some callers use this wrong. The original implementation also swallows
      // an exception if this happens (e.g. if you tap on a video that has a minutiae as well.
      // Example: Text = "ABC", insert image at position 18.
      return;
    }
    Drawable topLevelDrawable = draweeHolder.getTopLevelDrawable();
    if (topLevelDrawable != null) {
      if (topLevelDrawable.getBounds().isEmpty()) {
        topLevelDrawable.setBounds(0, 0, drawableWidthPx, drawableHeightPx);
      }
      topLevelDrawable.setCallback(mDrawableCallback);
    }
    DraweeSpan draweeSpan = new DraweeSpan(draweeHolder, verticalAlignment);
    final DraweeController controller = draweeHolder.getController();
    if (controller instanceof AbstractDraweeController) {
      ((AbstractDraweeController) controller).addControllerListener(
          new DrawableChangedListener(draweeSpan, enableResizing, drawableHeightPx));
    }
    mDraweeSpans.add(draweeSpan);
    setSpan(draweeSpan, startIndex, endIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  public void setImageSpan(
      Context context,
      DraweeHierarchy draweeHierarchy,
      DraweeController draweeController,
      int index,
      final int drawableWidthPx,
      final int drawableHeightPx,
      boolean enableResizing,
      @BetterImageSpan.BetterImageSpanAlignment int verticalAlignment) {
    setImageSpan(
        context,
        draweeHierarchy,
        draweeController,
        index,
        index,
        drawableWidthPx,
        drawableHeightPx,
        enableResizing,
        verticalAlignment);
  }

  public void setImageSpan(
      Context context,
      DraweeHierarchy draweeHierarchy,
      DraweeController draweeController,
      int startIndex,
      int endIndex,
      final int drawableWidthPx,
      final int drawableHeightPx,
      boolean enableResizing,
      @BetterImageSpan.BetterImageSpanAlignment int verticalAlignment) {
    DraweeHolder draweeHolder = DraweeHolder.create(draweeHierarchy, context);
    draweeHolder.setController(draweeController);
    setImageSpan(
        draweeHolder,
        startIndex,
        endIndex,
        drawableWidthPx,
        drawableHeightPx,
        enableResizing,
        verticalAlignment);
  }

  public void setDraweeSpanChangedListener(DraweeSpanChangedListener draweeSpanChangedListener) {
    mDraweeSpanChangedListener = draweeSpanChangedListener;
  }

  public boolean hasDraweeSpans() {
    return !mDraweeSpans.isEmpty();
  }

  @Override
  public void onAttachToView(View view) {
    bindToView(view);
    onAttach();
  }

  @Override
  public void onDetachFromView(View view) {
    unbindFromView(view);
    onDetach();
  }

  @VisibleForTesting
  void onAttach() {
    for (DraweeSpan span : mDraweeSpans) {
      span.onAttach();
    }
  }

  @VisibleForTesting
  void onDetach() {
    for (DraweeSpan span : mDraweeSpans) {
      span.onDetach();
    }
  }

  @VisibleForTesting
  public Set<DraweeSpan> getDraweeSpans() {
    return mDraweeSpans;
  }

  protected void bindToView(View view) {
    unbindFromPreviousComponent();
    mBoundView = view;
  }

  protected void bindToDrawable(Drawable drawable) {
    unbindFromPreviousComponent();
    mBoundDrawable = drawable;
  }

  protected void unbindFromView(View view) {
    if (view != mBoundView) {
      return; // we are bound to a different view already
    }
    mBoundView = null;
  }

  protected void unbindFromDrawable(Drawable drawable) {
    if (drawable != mBoundDrawable) {
      return; // we are bound to a different view already
    }
    mBoundDrawable = null;
  }

  protected void unbindFromPreviousComponent() {
    if (mBoundView != null) {
      unbindFromView(mBoundView);
    }
    if (mBoundDrawable != null) {
      unbindFromDrawable(mBoundDrawable);
    }
  }

  private class DrawableCallback implements Drawable.Callback {

    @Override
    public void invalidateDrawable(Drawable who) {
      if (mBoundView != null) {
        // invalidateDrawable might not work correctly since we don't know the exact location
        // of the drawable and invalidateDrawable could mark the wrong rect as dirty
        mBoundView.invalidate();
      } else if (mBoundDrawable != null) {
        mBoundDrawable.invalidateSelf();
      }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
      if (mBoundView != null) {
        // 'mBoundView.scheduleDrawable(who, what, when)' wouldn't work because
        // it cannot determine the 'who' drawable with 'verifyDrawable(who)'.
        // So we're re-implementing 'scheduleDrawable' manually.
        final long delay = when - SystemClock.uptimeMillis();
        mBoundView.postDelayed(what, delay);
      } else if (mBoundDrawable != null) {
        mBoundDrawable.scheduleSelf(what, when);
      }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
      if (mBoundView != null) {
        mBoundView.removeCallbacks(what);
      } else if (mBoundDrawable != null) {
        mBoundDrawable.unscheduleSelf(what);
      }
    }
  }

  private class DrawableChangedListener extends BaseControllerListener<ImageInfo> {

    private final DraweeSpan mDraweeSpan;

    private final boolean mEnableResizing;

    private final int mFixedHeight;

    public DrawableChangedListener(DraweeSpan draweeSpan) {
      this(draweeSpan, false);
    }

    public DrawableChangedListener(DraweeSpan draweeSpan, boolean enableResizing) {
      this(draweeSpan, enableResizing, UNSET_SIZE);
    }

    /**
     * Create a new DrawableChangedListener If resizing is enabled, the drawable will be resized to
     * the size of the actual image once it is available. If a fixed height is given and resizing is
     * enabled, the drawable will be resized to match the aspect ratio of the original image but
     * will have the given fixed height.
     *
     * @param draweeSpan the Drawee span to listen to
     * @param enableResizing if true, the drawable will be resized according to the final image
     * size
     * @param fixedHeight use a fixed height even if resizing is enabled {@link #UNSET_SIZE}
     */
    public DrawableChangedListener(
        DraweeSpan draweeSpan,
        boolean enableResizing,
        int fixedHeight) {
      Preconditions.checkNotNull(draweeSpan);
      mDraweeSpan = draweeSpan;
      mEnableResizing = enableResizing;
      mFixedHeight = fixedHeight;
    }

    @Override
    public void onFinalImageSet(
        String id,
        ImageInfo imageInfo,
        Animatable animatable) {
      if (mEnableResizing &&
          imageInfo != null &&
          mDraweeSpan.getDraweeHolder().getTopLevelDrawable() != null) {
        Drawable topLevelDrawable = mDraweeSpan.getDraweeHolder().getTopLevelDrawable();
        Rect topLevelDrawableBounds = topLevelDrawable.getBounds();
        if (mFixedHeight != UNSET_SIZE) {
          float imageWidth = ((float) mFixedHeight / imageInfo.getHeight()) * imageInfo.getWidth();
          int imageWidthPx = (int) imageWidth;
          if (topLevelDrawableBounds.width() != imageWidthPx ||
              topLevelDrawableBounds.height() != mFixedHeight) {
            topLevelDrawable.setBounds(0, 0, imageWidthPx, mFixedHeight);

            if (mDraweeSpanChangedListener != null) {
              mDraweeSpanChangedListener.onDraweeSpanChanged(DraweeSpanStringBuilder.this);
            }
          }
        } else if (topLevelDrawableBounds.width() != imageInfo.getWidth() ||
            topLevelDrawableBounds.height() != imageInfo.getHeight()) {
          topLevelDrawable.setBounds(0, 0, imageInfo.getWidth(), imageInfo.getHeight());

          if (mDraweeSpanChangedListener != null) {
            mDraweeSpanChangedListener.onDraweeSpanChanged(DraweeSpanStringBuilder.this);
          }
        }
      }
    }
  }
}
