/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

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
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("GenericDraweeView#inflateHierarchy");
    }
    GenericDraweeHierarchyBuilder builder =
        GenericDraweeHierarchyInflater.inflateBuilder(context, attrs);
    setAspectRatio(builder.getDesiredAspectRatio());
    setHierarchy(builder.build());
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }
}
