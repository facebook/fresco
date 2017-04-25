/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.span;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
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

  /**
   * Resizes the drawable according to its requested size, allowing the drawable to grow if needed.
   */
  public static final int SCALE_TYPE_RESIZE = 0;
  /**
   * Scales the drawable to fit within the bounds specified in
   * {@link #setImageSpan(DraweeHolder, int, int, int, int, boolean, int, int)}, maintaining
   * its aspect ratio.
   */
  public static final int SCALE_TYPE_CENTER_INSIDE = 1;

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
    setImageSpan(draweeHolder,
        startIndex,
        endIndex,
        drawableWidthPx,
        drawableHeightPx,
        enableResizing,
        verticalAlignment,
        SCALE_TYPE_RESIZE);
  }

  public void setImageSpan(
      DraweeHolder draweeHolder,
      int startIndex,
      int endIndex,
      final int drawableWidthPx,
      final int drawableHeightPx,
      boolean enableResizing,
      @BetterImageSpan.BetterImageSpanAlignment int verticalAlignment,
      final int scaleType) {
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
          new DrawableChangedListener(draweeSpan, enableResizing, drawableWidthPx, drawableHeightPx, scaleType));
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
        mBoundView.scheduleDrawable(who, what, when);
      } else if (mBoundDrawable != null) {
        mBoundDrawable.scheduleSelf(what, when);
      }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
      if (mBoundView != null) {
        unscheduleDrawable(who, what);
      } else if (mBoundDrawable != null) {
        mBoundDrawable.unscheduleSelf(what);
      }
    }
  }

  private class DrawableChangedListener extends BaseControllerListener<ImageInfo> {

    private final DraweeSpan mDraweeSpan;

    private final boolean mEnableResizing;

    private final int mFixedWidth;
    private final int mFixedHeight;

    private final int mScaleType;

    public DrawableChangedListener(DraweeSpan draweeSpan) {
      this(draweeSpan, false);
    }

    public DrawableChangedListener(DraweeSpan draweeSpan, boolean enableResizing) {
      this(draweeSpan, enableResizing, UNSET_SIZE, UNSET_SIZE, SCALE_TYPE_RESIZE);
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
        int fixedWidth,
        int fixedHeight,
        int scaleType) {
      Preconditions.checkNotNull(draweeSpan);
      mDraweeSpan = draweeSpan;
      mEnableResizing = enableResizing;
      mFixedWidth = fixedWidth;
      mFixedHeight = fixedHeight;
      mScaleType = scaleType;
    }

    @Override
    public void onFinalImageSet(
        String id,
        ImageInfo imageInfo,
        Animatable animatable) {

      if (mFixedHeight != UNSET_SIZE && imageInfo != null) {
        Drawable topLevelDrawable = mDraweeSpan.getDraweeHolder().getTopLevelDrawable();

        final Pair<Integer, Integer> scaledDimens;

        if (mScaleType == SCALE_TYPE_RESIZE) {
          scaledDimens = getScaledDimensResize(topLevelDrawable, imageInfo);
        } else if (mScaleType == SCALE_TYPE_CENTER_INSIDE) {
          scaledDimens = getScaledDimensCenterInside(imageInfo);
        } else {
          throw new RuntimeException("invalid scaleType: " + mScaleType);
        }

        if (scaledDimens != null) {
          topLevelDrawable.setBounds(0, 0, scaledDimens.first, scaledDimens.second);

          if (mDraweeSpanChangedListener != null) {
            mDraweeSpanChangedListener.onDraweeSpanChanged(DraweeSpanStringBuilder.this);
          }
        }
      }
    }

    private @Nullable Pair<Integer, Integer> getScaledDimensResize(final Drawable drawable, final ImageInfo imageInfo) {
      final Rect drawableBounds = drawable.getBounds();
      if (mEnableResizing && mDraweeSpan.getDraweeHolder().getTopLevelDrawable() != null) {
        float imageWidth = ((float) mFixedHeight / imageInfo.getHeight()) * imageInfo.getWidth();
        int imageWidthPx = (int) imageWidth;
        if (drawableBounds.width() != imageWidthPx ||
            drawableBounds.height() != mFixedHeight) {

          return new Pair<>(imageWidthPx, mFixedHeight);
        }
      } else if (drawableBounds.width() != imageInfo.getWidth() ||
          drawableBounds.height() != imageInfo.getHeight()) {
        return new Pair<>(imageInfo.getWidth(), imageInfo.getHeight());
      }

      return null;
    }

    private Pair<Integer, Integer> getScaledDimensCenterInside(final ImageInfo imageInfo) {
      int scaledWidth = imageInfo.getWidth();
      int scaledHeight = imageInfo.getHeight();

      if (mFixedWidth != UNSET_SIZE) {
        if (scaledWidth > mFixedWidth) {
          final float widthScaleRatio = scaledWidth / (float) mFixedWidth;
          scaledWidth = mFixedWidth;
          scaledHeight = (int) (scaledHeight / widthScaleRatio);
        }
      }

      if (mFixedHeight != UNSET_SIZE) {
        if (scaledHeight > mFixedHeight) {
          final float heightScaleRatio = scaledHeight / (float) mFixedHeight;
          scaledHeight = mFixedHeight;
          scaledWidth = (int) (scaledWidth / heightScaleRatio);
        }
      }

      return new Pair<>(scaledWidth, scaledHeight);
    }
  }
}
