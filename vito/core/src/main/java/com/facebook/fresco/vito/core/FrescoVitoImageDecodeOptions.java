/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.PointF;
import android.graphics.Rect;
import com.facebook.common.internal.Objects;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import javax.annotation.Nullable;

public class FrescoVitoImageDecodeOptions extends ImageDecodeOptions {

  private static final FrescoVitoImageDecodeOptions DEFAULTS =
      FrescoVitoImageDecodeOptions.newBuilder().build();

  /** Image parent bounds. */
  public final @Nullable Rect parentBounds;

  /** Image focus point. */
  public final @Nullable PointF focusPoint;

  /** Image focus point. */
  public final @Nullable ScalingUtils.ScaleType scaleType;

  /**
   * Gets the default options.
   *
   * @return the default options
   */
  public static FrescoVitoImageDecodeOptions defaults() {
    return DEFAULTS;
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder
   */
  public static FrescoVitoImageDecodeOptionsBuilder newBuilder() {
    return new FrescoVitoImageDecodeOptionsBuilder();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return false;
    }
    if (obj == null || getClass() != obj.getClass()) return false;

    FrescoVitoImageDecodeOptions that = (FrescoVitoImageDecodeOptions) obj;

    if (!Objects.equal(parentBounds, that.parentBounds)
        || !Objects.equal(focusPoint, that.focusPoint)
        || !Objects.equal(scaleType, that.scaleType)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (parentBounds != null ? parentBounds.hashCode() : 0);
    result = 31 * result + (focusPoint != null ? focusPoint.hashCode() : 0);
    result = 31 * result + (scaleType != null ? scaleType.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FrescoVitoImageDecodeOptions{" + toStringHelper().toString() + "}";
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper()
        .add("parentBounds", parentBounds)
        .add("focusPoint", focusPoint)
        .add("scaleType", scaleType);
  }

  public FrescoVitoImageDecodeOptions(FrescoVitoImageDecodeOptionsBuilder b) {
    super(b);
    this.parentBounds = b.getParentBounds();
    this.focusPoint = b.getFocusPoint();
    this.scaleType = b.getScaleType();
  }
}
