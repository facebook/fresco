/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.drawable;

/**
 * Interface for Drawables that round corners or form a circle.
 */
public interface Rounded {

  void setCircle(boolean isCircle);

  void setRadius(float radius);

  void setRadii(float[] radii);

  void setBorder(int color, float width);
}
