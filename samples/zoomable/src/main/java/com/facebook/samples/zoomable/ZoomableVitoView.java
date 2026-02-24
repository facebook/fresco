/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.core.view.ScrollingView;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.listener.BaseImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

/**
 * ImageView with zoomable capabilities, using Vito for image loading.
 *
 * <p>Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
public class ZoomableVitoView extends ImageView implements ScrollingView {

  private static final Class<?> TAG = ZoomableVitoView.class;

  private final RectF mImageBounds = new RectF();
  private final RectF mViewBounds = new RectF();

  private ZoomableController mZoomableController;
  private GestureDetector mTapGestureDetector;
  private boolean mAllowTouchInterceptionWhileZoomed = true;

  private boolean mIsDialtoneEnabled = false;
  private boolean mZoomingEnabled = true;

  private final BaseImageListener mImageListener =
      new BaseImageListener() {
        @Override
        public void onFinalImageSet(
            long id, int imageOrigin, @Nullable ImageInfo imageInfo, @Nullable Drawable drawable) {
          ZoomableVitoView.this.onFinalImageSet();
        }

        @Override
        public void onRelease(long id) {
          ZoomableVitoView.this.onRelease();
        }
      };

  private final ZoomableController.Listener mZoomableListener =
      new ZoomableController.Listener() {
        @Override
        public void onTransformBegin(Matrix transform) {}

        @Override
        public void onTransformChanged(Matrix transform) {
          ZoomableVitoView.this.onTransformChanged(transform);
        }

        @Override
        public void onTransformEnd(Matrix transform) {}
      };

  private final GestureListenerWrapper mTapListenerWrapper = new GestureListenerWrapper();

  @SuppressWarnings("this-escape")
  public ZoomableVitoView(Context context) {
    super(context);
    setScaleType(ScaleType.FIT_CENTER);
    init();
  }

  @SuppressWarnings("this-escape")
  public ZoomableVitoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setScaleType(ScaleType.FIT_CENTER);
    init();
  }

  @SuppressWarnings("this-escape")
  public ZoomableVitoView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setScaleType(ScaleType.FIT_CENTER);
    init();
  }

  private void init() {
    mZoomableController = createZoomableController();
    mZoomableController.setListener(mZoomableListener);
    mTapGestureDetector = new GestureDetector(getContext(), mTapListenerWrapper);
  }

  public void setIsDialtoneEnabled(boolean isDialtoneEnabled) {
    mIsDialtoneEnabled = isDialtoneEnabled;
  }

  /**
   * Gets the original image bounds, in view-absolute coordinates.
   *
   * <p>The bounds are computed from the drawable dimensions transformed by the ImageView's image
   * matrix (which accounts for the ScaleType). This matches the image position within the view
   * before any zoomable transformation is applied.
   */
  protected void getImageBounds(RectF outBounds) {
    Drawable d = getDrawable();
    if (d != null) {
      outBounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
      getImageMatrix().mapRect(outBounds);
    } else {
      outBounds.setEmpty();
    }
  }

  /**
   * Gets the bounds used to limit the translation, in view-absolute coordinates.
   *
   * <p>Unless overridden by a subclass, these bounds are same as the view bounds.
   */
  protected void getLimitBounds(RectF outBounds) {
    outBounds.set(0, 0, getWidth(), getHeight());
  }

  /** Sets a custom zoomable controller, instead of using the default one. */
  public void setZoomableController(ZoomableController zoomableController) {
    mZoomableController = zoomableController;
    mZoomableController.setListener(mZoomableListener);
  }

  /** Gets the zoomable controller. */
  public ZoomableController getZoomableController() {
    return mZoomableController;
  }

  /**
   * Check whether the parent view can intercept touch events while zoomed.
   *
   * @return true if touch events can be intercepted
   */
  public boolean allowsTouchInterceptionWhileZoomed() {
    return mAllowTouchInterceptionWhileZoomed;
  }

  /**
   * If this is set to true, parent views can intercept touch events while the view is zoomed. For
   * example, this can be used to swipe between images in a view pager while zoomed.
   *
   * @param allowTouchInterceptionWhileZoomed true if the parent needs to intercept touches
   */
  public void setAllowTouchInterceptionWhileZoomed(boolean allowTouchInterceptionWhileZoomed) {
    mAllowTouchInterceptionWhileZoomed = allowTouchInterceptionWhileZoomed;
  }

  /** Sets the tap listener. */
  public void setTapListener(GestureDetector.SimpleOnGestureListener tapListener) {
    mTapListenerWrapper.setListener(tapListener);
  }

  /**
   * Sets whether long-press tap detection is enabled. Unfortunately, long-press conflicts with
   * onDoubleTapEvent.
   */
  public void setIsLongpressEnabled(boolean enabled) {
    mTapGestureDetector.setIsLongpressEnabled(enabled);
  }

  public void setZoomingEnabled(boolean zoomingEnabled) {
    mZoomingEnabled = zoomingEnabled;
    mZoomableController.setEnabled(false);
  }

  /**
   * Loads an image into this view using Vito.
   *
   * @param uri the image URI to load
   * @param callerContext caller context for logging
   */
  public void loadImage(Uri uri, @Nullable Object callerContext) {
    loadImage(uri, ImageOptions.defaults(), callerContext);
  }

  /**
   * Loads an image into this view using Vito with custom options.
   *
   * @param uri the image URI to load
   * @param imageOptions the image options
   * @param callerContext caller context for logging
   */
  public void loadImage(Uri uri, ImageOptions imageOptions, @Nullable Object callerContext) {
    mZoomableController.setEnabled(false);
    VitoView.show(uri, imageOptions, callerContext, mImageListener, this);
  }

  /** Releases the current image. */
  public void releaseImage() {
    mZoomableController.setEnabled(false);
    VitoView.release(this);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int saveCount = canvas.save();
    canvas.concat(mZoomableController.getTransform());
    super.onDraw(canvas);
    canvas.restoreToCount(saveCount);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int a = event.getActionMasked();
    FLog.v(getLogTag(), "onTouchEvent: %d, view %x, received", a, this.hashCode());
    if (!mIsDialtoneEnabled && mTapGestureDetector.onTouchEvent(event)) {
      FLog.v(
          getLogTag(),
          "onTouchEvent: %d, view %x, handled by tap gesture detector",
          a,
          this.hashCode());
      return true;
    }

    if (!mIsDialtoneEnabled && mZoomableController.onTouchEvent(event)) {
      FLog.v(
          getLogTag(),
          "onTouchEvent: %d, view %x, handled by zoomable controller",
          a,
          this.hashCode());
      if (!mAllowTouchInterceptionWhileZoomed && !mZoomableController.isIdentity()) {
        if (getParent() != null) {
          getParent().requestDisallowInterceptTouchEvent(true);
        }
      }
      return true;
    }
    if (super.onTouchEvent(event)) {
      FLog.v(getLogTag(), "onTouchEvent: %d, view %x, handled by the super", a, this.hashCode());
      return true;
    }
    MotionEvent cancelEvent = MotionEvent.obtain(event);
    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
    mTapGestureDetector.onTouchEvent(cancelEvent);
    mZoomableController.onTouchEvent(cancelEvent);
    cancelEvent.recycle();
    return false;
  }

  @Override
  public int computeHorizontalScrollRange() {
    return mZoomableController.computeHorizontalScrollRange();
  }

  @Override
  public int computeHorizontalScrollOffset() {
    return mZoomableController.computeHorizontalScrollOffset();
  }

  @Override
  public int computeHorizontalScrollExtent() {
    return mZoomableController.computeHorizontalScrollExtent();
  }

  @Override
  public int computeVerticalScrollRange() {
    return mZoomableController.computeVerticalScrollRange();
  }

  @Override
  public int computeVerticalScrollOffset() {
    return mZoomableController.computeVerticalScrollOffset();
  }

  @Override
  public int computeVerticalScrollExtent() {
    return mZoomableController.computeVerticalScrollExtent();
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    FLog.v(getLogTag(), "onLayout: view %x", this.hashCode());
    super.onLayout(changed, left, top, right, bottom);
    updateZoomableControllerBounds();
  }

  private void onFinalImageSet() {
    FLog.v(getLogTag(), "onFinalImageSet: view %x", this.hashCode());
    if (!mZoomableController.isEnabled() && mZoomingEnabled) {
      mZoomableController.setEnabled(true);
      updateZoomableControllerBounds();
    }
  }

  private void onRelease() {
    FLog.v(getLogTag(), "onRelease: view %x", this.hashCode());
    mZoomableController.setEnabled(false);
  }

  protected void onTransformChanged(Matrix transform) {
    FLog.v(getLogTag(), "onTransformChanged: view %x, transform: %s", this.hashCode(), transform);
    invalidate();
  }

  protected void updateZoomableControllerBounds() {
    getImageBounds(mImageBounds);
    getLimitBounds(mViewBounds);
    mZoomableController.setImageBounds(mImageBounds);
    mZoomableController.setViewBounds(mViewBounds);
    FLog.v(
        getLogTag(),
        "updateZoomableControllerBounds: view %x, view bounds: %s, image bounds: %s",
        this.hashCode(),
        mViewBounds,
        mImageBounds);
  }

  protected Class<?> getLogTag() {
    return TAG;
  }

  protected ZoomableController createZoomableController() {
    return AnimatedZoomableController.newInstance();
  }
}
