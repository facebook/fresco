/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

/**
 * Interface for Drawables that round corners or form a circle.
 */
public interface Rounded {

  void setCircle(boolean isCircle);
  boolean isCircle();

  void setRadius(float radius);
  void setRadii(float[] radii);
  float[] getRadii();

  void setBorder(int color, float width);
  int getBorderColor();
  float getBorderWidth();

  void setPadding(float padding);
  float getPadding();

  void setScaleDownInsideBorders(boolean scaleDownInsideBorders);
  boolean getScaleDownInsideBorders();

  void setPaintFilterBitmap(boolean paintFilterBitmap);
  boolean getPaintFilterBitmap();
}
