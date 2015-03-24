/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.android.volley.toolbox.ImageLoader;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;

import java.util.Set;

/**
 * Concrete implementation of Volley Drawee controller builder.
 * <p/> See {@link AbstractDraweeControllerBuilder} for more details.
 */
public class VolleyDraweeControllerBuilder extends AbstractDraweeControllerBuilder<
    VolleyDraweeControllerBuilder,
    Uri,
    Bitmap,
    Bitmap> {

  private final ImageLoader mImageLoader;
  private final VolleyDraweeControllerFactory mVolleyDraweeControllerFactory;

  public VolleyDraweeControllerBuilder(
      Context context,
      ImageLoader imageLoader,
      VolleyDraweeControllerFactory volleyDraweeControllerFactory,
      Set<ControllerListener> boundControllerListeners) {
    super(context, boundControllerListeners);
    mImageLoader = imageLoader;
    mVolleyDraweeControllerFactory = volleyDraweeControllerFactory;
  }

  @Override
  protected VolleyDraweeController obtainController() {
    DraweeController oldController = getOldController();
    VolleyDraweeController controller;
    if (oldController instanceof VolleyDraweeController) {
      controller = (VolleyDraweeController) oldController;
      controller.initialize(
          obtainDataSourceSupplier(),
          generateUniqueControllerId(),
          getCallerContext());
    } else {
      controller = mVolleyDraweeControllerFactory.newController(
          obtainDataSourceSupplier(),
          generateUniqueControllerId(),
          getCallerContext());
    }
    return controller;
  }

  @Override
  protected DataSource<Bitmap> getDataSourceForRequest(
      final Uri imageRequest,
      final Object callerContext,
      final boolean bitmapCacheOnly) {
    return new VolleyDataSource(mImageLoader, imageRequest, bitmapCacheOnly);
  }

  @Override
  public VolleyDraweeControllerBuilder setUri(Uri uri) {
    return setImageRequest(uri);
  }

  @Override
  protected VolleyDraweeControllerBuilder getThis() {
    return this;
  }
}
