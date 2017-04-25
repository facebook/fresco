/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import javax.annotation.Nullable;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.generic.RoundingParams;

/**
 * DraweeView that uses GenericDraweeHierarchy.
 *
 * The hierarchy can be set either programmatically or inflated from XML.
 * See {@link GenericDraweeHierarchyInflater} for supported XML attributes.
 */
public class GenericDraweeView extends DraweeView<GenericDraweeHierarchy> {

  public GenericDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
    super(context);
    setHierarchy(hierarchy);
  }

  public GenericDraweeView(Context context) {
    super(context);
    inflateHierarchy(context, null);
  }

  public GenericDraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    inflateHierarchy(context, attrs);
  }

  public GenericDraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflateHierarchy(context, attrs);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public GenericDraweeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    inflateHierarchy(context, attrs);
  }

  protected void inflateHierarchy(Context context, @Nullable AttributeSet attrs) {
    GenericDraweeHierarchyBuilder builder =
        GenericDraweeHierarchyInflater.inflateBuilder(context, attrs);
    setAspectRatio(builder.getDesiredAspectRatio());
    setHierarchy(builder.build());
  }

  /**
   * Sets the rounding params.
   */
  public void setRoundingParams(@Nullable RoundingParams roundingParams) {
    if (hasHierarchy()) {
      getHierarchy().setRoundingParams(roundingParams);
    }
    if (Build.VERSION.SDK_INT >= 21 &&
        roundingParams.getRoundingMethod() == RoundingParams.RoundingMethod.OUTLINE) {
      if (roundingParams != null) {
        this.setClipToOutline(true);
        this.setOutlineProvider(getClipOutlineProvider());
      } else {
        this.setClipToOutline(false);
        this.setOutlineProvider(null);
      }
    }
  }

  /**
   * Gets the rounding params.
   */
  @Nullable
  public RoundingParams getRoundingParams() {
    if (hasHierarchy()) {
      return getHierarchy().getRoundingParams();
    } else {
      return null;
    }
  }

  /**
   * Cached return value of {@link #getClipOutlineProvider}.
   */
  private static ViewOutlineProvider mClipOutlineProvider;

  /**
   * Creates an outline used for clipping the image so that it has rounded corners.
   * Used only when the rounding method is RoundingMethod.OUTLINE.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static ViewOutlineProvider getClipOutlineProvider() {
    if (mClipOutlineProvider == null) {
      mClipOutlineProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
          if (!(view instanceof GenericDraweeView)) {
            throw new AssertionError(
                "For rounded corners using the outline API, View must be a GenericDraweeView");
          }
          final GenericDraweeView dv = (GenericDraweeView) view;
          RoundingParams roundingParams = dv.getHierarchy().getRoundingParams();
          if (roundingParams.getRoundAsCircle()) {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w > h) {
              int l = (w - h) / 2;
              outline.setOval(l, 0, l + h, h);
            } else {
              int t = (h - w) / 2;
              outline.setOval(0, t, w, t + w);
            }
          } else {
            float[] cornerRadii = roundingParams.getCornersRadii();
            if (cornerRadii != null) {
              // The cornerRadii can specify custom radius for each corner. However,
              // the outline API only supports ovals and rounded rectangles (each corner
              // has the same radius). When RoundingMethod.OUTLINE is used we simply use
              // the x radius of the first corner.
              float topLeftCornerXRadius = cornerRadii[0];
              if (topLeftCornerXRadius > 0) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), topLeftCornerXRadius);
              }
            }
          }
        }
      };
    }
    return mClipOutlineProvider;
  }
}
