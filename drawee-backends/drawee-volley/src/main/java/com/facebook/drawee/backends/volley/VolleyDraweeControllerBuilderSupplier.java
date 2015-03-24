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

import com.android.volley.toolbox.ImageLoader;
import com.facebook.common.executors.UiThreadExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.ControllerListener;

import java.util.Set;

/**
 * Supplier of Volley Drawee controller builders.
 */
public class VolleyDraweeControllerBuilderSupplier implements
    Supplier<VolleyDraweeControllerBuilder> {

  private final Context mContext;
  private final ImageLoader mImageLoader;
  private final VolleyDraweeControllerFactory mVolleyDraweeControllerFactory;
  private final Set<ControllerListener> mBoundControllerListeners;

  public VolleyDraweeControllerBuilderSupplier(
      Context context,
      ImageLoader imageLoader) {
    this(context, imageLoader, null);
  }

  public VolleyDraweeControllerBuilderSupplier(
      Context context,
      ImageLoader imageLoader,
      Set<ControllerListener> boundControllerListeners) {
    mContext = context;
    mImageLoader = imageLoader;
    mVolleyDraweeControllerFactory = new VolleyDraweeControllerFactory(
        context.getResources(),
        DeferredReleaser.getInstance(),
        UiThreadExecutorService.getInstance());
    mBoundControllerListeners = boundControllerListeners;
  }

  @Override
  public VolleyDraweeControllerBuilder get() {
    return new VolleyDraweeControllerBuilder(
        mContext,
        mImageLoader,
        mVolleyDraweeControllerFactory,
        mBoundControllerListeners);
  }
}
