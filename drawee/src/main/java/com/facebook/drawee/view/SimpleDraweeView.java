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

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.SimpleDraweeControllerBuilder;

/**
 * This view takes a uri as input and internally builds and sets a controller.
 *
 * <p>This class must be statically initialized in order to be used. If you are using the Fresco
 * image pipeline, use {@link com.facebook.drawee.backends.pipeline.Fresco#initialize} to do this.
 */
public class SimpleDraweeView extends GenericDraweeView {

  private static Supplier<? extends SimpleDraweeControllerBuilder> sDraweeControllerBuilderSupplier;

  /** Initializes {@link SimpleDraweeView} with supplier of Drawee controller builders. */
  public static void initialize(
      Supplier<? extends SimpleDraweeControllerBuilder> draweeControllerBuilderSupplier) {
    sDraweeControllerBuilderSupplier = draweeControllerBuilderSupplier;
  }

  /** Shuts {@link SimpleDraweeView} down. */
  public static void shutDown() {
    sDraweeControllerBuilderSupplier = null;
  }

  private SimpleDraweeControllerBuilder mSimpleDraweeControllerBuilder;

  public SimpleDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
    super(context, hierarchy);
    init();
  }

  public SimpleDraweeView(Context context) {
    super(context);
    init();
  }

  public SimpleDraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public SimpleDraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    if (isInEditMode()) {
      return;
    }
    Preconditions.checkNotNull(
        sDraweeControllerBuilderSupplier,
        "SimpleDraweeView was not initialized!");
    mSimpleDraweeControllerBuilder = sDraweeControllerBuilderSupplier.get();
  }

  protected SimpleDraweeControllerBuilder getControllerBuilder() {
    return mSimpleDraweeControllerBuilder;
  }

  /**
   * Displays an image given by the uri.
   *
   * @param uri uri of the image
   * @undeprecate
   */
  @Override
  public void setImageURI(Uri uri) {
    setImageURI(uri, null);
  }

  /**
   * Displays an image given by the uri.
   *
   * @param uri uri of the image
   * @param callerContext caller context
   */
  public void setImageURI(Uri uri, @Nullable Object callerContext) {
    DraweeController controller = mSimpleDraweeControllerBuilder
        .setCallerContext(callerContext)
        .setUri(uri)
        .setOldController(getController())
        .build();
    setController(controller);
  }
}
