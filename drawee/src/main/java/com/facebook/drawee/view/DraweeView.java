/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import com.facebook.common.internal.Objects;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/**
 * View that displays a {@link DraweeHierarchy}.
 *
 * <p> Hierarchy should be set prior to using this view. See {@code setHierarchy}. Because creating
 * a hierarchy is an expensive operation, it is recommended this be done once per view, typically
 * near creation time.
 *
 * <p> In order to display an image, controller has to be set. See {@code setController}.

 * <p> Although ImageView is subclassed instead of subclassing View directly, this class does not
 * support ImageView's setImageXxx, setScaleType and similar methods. Extending ImageView is a short
 * term solution in order to inherit some of its implementation (padding calculations, etc.).
 * This class is likely to be converted to extend View directly in the future, so avoid using
 * ImageView's methods and properties.
 */
public class DraweeView<DH extends DraweeHierarchy> extends ImageView {

  private final AspectRatioMeasure.Spec mMeasureSpec = new AspectRatioMeasure.Spec();
  private float mAspectRatio = 0;
  private DraweeHolder<DH> mDraweeHolder;
  private boolean mInitialised = false;
  private boolean mLegacyVisibilityHandlingEnabled = false;

  private static boolean sGlobalLegacyVisibilityHandlingEnabled = false;

  public static void setGlobalLegacyVisibilityHandlingEnabled(
      boolean legacyVisibilityHandlingEnabled) {
    sGlobalLegacyVisibilityHandlingEnabled = legacyVisibilityHandlingEnabled;
  }

  public DraweeView(Context context) {
    super(context);
    init(context);
  }

  public DraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public DraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public DraweeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context,attrs,defStyleAttr,defStyleRes);
    init(context);
  }

  /** This method is idempotent so it only has effect the first time it's called */
  private void init(Context context) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("DraweeView#init");
      }
      if (mInitialised) {
        return;
      }
      mInitialised = true;
      mDraweeHolder = DraweeHolder.create(null, context);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ColorStateList imageTintList = getImageTintList();
        if (imageTintList == null) {
          return;
        }
        setColorFilter(imageTintList.getDefaultColor());
      }
      // In Android N and above, visibility handling for Drawables has been changed, which breaks
      // activity transitions with DraweeViews.
      mLegacyVisibilityHandlingEnabled =
          sGlobalLegacyVisibilityHandlingEnabled
              && context.getApplicationInfo().targetSdkVersion >= 24; // Build.VERSION_CODES.N
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /** Sets the hierarchy. */
  public void setHierarchy(DH hierarchy) {
    mDraweeHolder.setHierarchy(hierarchy);
    super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
  }

  /** Gets the hierarchy if set, throws NPE otherwise. */
  public DH getHierarchy() {
    return mDraweeHolder.getHierarchy();
  }

  /** Returns whether the hierarchy is set or not. */
  public boolean hasHierarchy() {
    return mDraweeHolder.hasHierarchy();
  }

  /** Gets the top-level drawable if hierarchy is set, null otherwise. */
  @Nullable public Drawable getTopLevelDrawable() {
    return mDraweeHolder.getTopLevelDrawable();
  }

  /** Sets the controller. */
  public void setController(@Nullable DraweeController draweeController) {
    mDraweeHolder.setController(draweeController);
    super.setImageDrawable(mDraweeHolder.getTopLevelDrawable());
  }

  /** Gets the controller if set, null otherwise. */
  @Nullable public DraweeController getController() {
    return mDraweeHolder.getController();
  }

  /** Returns whether the controller is set or not.  */
  public boolean hasController() {
    return mDraweeHolder.getController() != null;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    maybeOverrideVisibilityHandling();
    onAttach();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    maybeOverrideVisibilityHandling();
    onDetach();
  }

  @Override
  public void onStartTemporaryDetach() {
    super.onStartTemporaryDetach();
    maybeOverrideVisibilityHandling();
    onDetach();
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    maybeOverrideVisibilityHandling();
    onAttach();
  }

  /** Called by the system to attach. Subclasses may override. */
  protected void onAttach() {
    doAttach();
  }

  /**  Called by the system to detach. Subclasses may override. */
  protected void onDetach() {
    doDetach();
  }

  /**
   * Does the actual work of attaching.
   *
   * Non-test subclasses should NOT override. Use onAttach for custom code.
   */
  protected void doAttach() {
    mDraweeHolder.onAttach();
  }

  /**
   * Does the actual work of detaching.
   *
   * Non-test subclasses should NOT override. Use onDetach for custom code.
   */
  protected void doDetach() {
    mDraweeHolder.onDetach();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mDraweeHolder.onTouchEvent(event)) {
      return true;
    }
    return super.onTouchEvent(event);
  }

  /**
   * Use this method only when using this class as an ordinary ImageView.
   * @deprecated Use {@link #setController(DraweeController)} instead.
   */
  @Override
  @Deprecated
  public void setImageDrawable(Drawable drawable) {
    init(getContext());
    mDraweeHolder.setController(null);
    super.setImageDrawable(drawable);
  }

  /**
   * Use this method only when using this class as an ordinary ImageView.
   * @deprecated Use {@link #setController(DraweeController)} instead.
   */
  @Override
  @Deprecated
  public void setImageBitmap(Bitmap bm) {
    init(getContext());
    mDraweeHolder.setController(null);
    super.setImageBitmap(bm);
  }

  /**
   * Use this method only when using this class as an ordinary ImageView.
   * @deprecated Use {@link #setController(DraweeController)} instead.
   */
  @Override
  @Deprecated
  public void setImageResource(int resId) {
    init(getContext());
    mDraweeHolder.setController(null);
    super.setImageResource(resId);
  }

  /**
   * Use this method only when using this class as an ordinary ImageView.
   * @deprecated Use {@link #setController(DraweeController)} instead.
   */
  @Override
  @Deprecated
  public void setImageURI(Uri uri) {
    init(getContext());
    mDraweeHolder.setController(null);
    super.setImageURI(uri);
  }

  /**
   * Sets the desired aspect ratio (w/h).
   */
  public void setAspectRatio(float aspectRatio) {
    if (aspectRatio == mAspectRatio) {
      return;
    }
    mAspectRatio = aspectRatio;
    requestLayout();
  }

  /**
   * Gets the desired aspect ratio (w/h).
   */
  public float getAspectRatio() {
    return mAspectRatio;
  }

  public void setLegacyVisibilityHandlingEnabled(boolean legacyVisibilityHandlingEnabled) {
    mLegacyVisibilityHandlingEnabled = legacyVisibilityHandlingEnabled;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mMeasureSpec.width = widthMeasureSpec;
    mMeasureSpec.height = heightMeasureSpec;
    AspectRatioMeasure.updateMeasureSpec(
        mMeasureSpec,
        mAspectRatio,
        getLayoutParams(),
        getPaddingLeft() + getPaddingRight(),
        getPaddingTop() + getPaddingBottom());
    super.onMeasure(mMeasureSpec.width, mMeasureSpec.height);
  }

  @Override
  protected void onVisibilityChanged(
      View changedView,
      int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    maybeOverrideVisibilityHandling();
  }

  private void maybeOverrideVisibilityHandling() {
    if (mLegacyVisibilityHandlingEnabled)  {
      Drawable drawable = getDrawable();
      if (drawable != null) {
        drawable.setVisible(getVisibility() == VISIBLE, false);
      }
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("holder", mDraweeHolder != null ? mDraweeHolder.toString(): "<no holder set>")
        .toString();
  }
}
