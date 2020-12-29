/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.PointF;
import android.graphics.Rect;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoVitoImageDecodeOptionsBuilder
    extends ImageDecodeOptionsBuilder<FrescoVitoImageDecodeOptionsBuilder> {

  private @Nullable Rect mParentBounds;
  private @Nullable PointF mFocusPoint;
  private @Nullable ScalingUtils.ScaleType mScaleType;

  public FrescoVitoImageDecodeOptionsBuilder setFrom(FrescoVitoImageDecodeOptions options) {
    super.setFrom(options);
    mParentBounds = options.parentBounds;
    mFocusPoint = options.focusPoint;
    mScaleType = options.scaleType;
    return getThis();
  }

  /**
   * Sets the target parent bounds for decoding.
   *
   * @param parentBounds target parent bounds for decoding.
   */
  public FrescoVitoImageDecodeOptionsBuilder setParentBounds(Rect parentBounds) {
    mParentBounds = parentBounds;
    return getThis();
  }

  /**
   * Gets the target parent bounds for decoding.
   *
   * @return the target parent bounds.
   */
  @Nullable
  public Rect getParentBounds() {
    return mParentBounds;
  }

  /**
   * Sets the target focus point for decoding.
   *
   * @param focusPoint target focus point for decoding.
   */
  public FrescoVitoImageDecodeOptionsBuilder setFocusPoint(PointF focusPoint) {
    mFocusPoint = focusPoint;
    return getThis();
  }

  /**
   * Gets the target focus point for decoding.
   *
   * @return the target focus point.
   */
  @Nullable
  public PointF getFocusPoint() {
    return mFocusPoint;
  }

  /**
   * Sets the target scale Type for decoding.
   *
   * @param scaleType target scale Type for decoding.
   */
  public FrescoVitoImageDecodeOptionsBuilder setScaleType(ScalingUtils.ScaleType scaleType) {
    mScaleType = scaleType;
    return getThis();
  }

  /**
   * Gets the target scale type for decoding.
   *
   * @return the target scale type.
   */
  @Nullable
  public ScalingUtils.ScaleType getScaleType() {
    return mScaleType;
  }

  @Override
  public FrescoVitoImageDecodeOptions build() {
    return new FrescoVitoImageDecodeOptions(this);
  }
}
