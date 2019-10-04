/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomable;

import android.content.Context;
import android.util.AttributeSet;
import com.facebook.drawee.generic.GenericDraweeHierarchy;

/**
 * DraweeView that has zoomable capabilities.
 *
 * <p>Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
public class ZoomableDraweeViewSupport extends ZoomableDraweeView {

  private static final Class<?> TAG = ZoomableDraweeViewSupport.class;

  public ZoomableDraweeViewSupport(Context context, GenericDraweeHierarchy hierarchy) {
    super(context, hierarchy);
  }

  public ZoomableDraweeViewSupport(Context context) {
    super(context);
  }

  public ZoomableDraweeViewSupport(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ZoomableDraweeViewSupport(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected Class<?> getLogTag() {
    return TAG;
  }

  @Override
  protected ZoomableController createZoomableController() {
    return AnimatedZoomableControllerSupport.newInstance();
  }
}
