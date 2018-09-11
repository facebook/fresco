/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.common.internal.Preconditions;
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
    String controllerId = generateUniqueControllerId();
    if (oldController instanceof VolleyDraweeController) {
      controller = (VolleyDraweeController) oldController;
    } else {
      controller = mVolleyDraweeControllerFactory.newController();
    }
    controller.initialize(
        obtainDataSourceSupplier(controller, controllerId), controllerId, getCallerContext());
    return controller;
  }

  @Override
  protected DataSource<Bitmap> getDataSourceForRequest(
      final DraweeController controller,
      final String controllerId,
      final Uri imageRequest,
      final Object callerContext,
      final CacheLevel cacheLevel) {
    return new VolleyDataSource(mImageLoader, imageRequest, cacheLevel);
  }

  @Override
  public VolleyDraweeControllerBuilder setUri(Uri uri) {
    return setImageRequest(uri);
  }

  @Override
  public VolleyDraweeControllerBuilder setUri(String uriString) {
    Preconditions.checkNotNull(uriString);
    return setImageRequest(Uri.parse(uriString));
  }
}
